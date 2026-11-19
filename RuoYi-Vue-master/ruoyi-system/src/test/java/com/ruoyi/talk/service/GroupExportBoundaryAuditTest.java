package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.talk.controller.TalkSessionController;
import com.ruoyi.talk.domain.*;
import com.ruoyi.talk.mapper.*;
import com.ruoyi.talk.security.TalkPermissionUtils;

class GroupExportBoundaryAuditTest {
    private final Path temp = Path.of("..", "..", "qa-runtime", "group-export-boundary-temp", "profile").toAbsolutePath().normalize();

    private static Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (var in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = in.getNextEntry(); entry != null; entry = in.getNextEntry())
                result.put(entry.getName(), in.readAllBytes());
        }
        return result;
    }

    private static String text(byte[] bytes) throws Exception {
        try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             var extractor = new XWPFWordExtractor(doc)) { return extractor.getText(); }
    }

    @Test void groupSummaryAndBatchContainOtherStudentAndSharedAttachment() throws Exception {
        var docs = new TalkDocxService();
        var sessions = mock(TalkSessionMapper.class);
        var records = mock(TalkStudentRecordMapper.class);
        var students = mock(TalkStudentMapper.class);
        var tags = mock(TalkSessionTagMapper.class);
        var attachments = mock(TalkAttachmentMapper.class);
        var depts = mock(SysDeptMapper.class);
        ReflectionTestUtils.setField(docs, "templatePath", "templates/学生谈心谈话记录表.docx");
        ReflectionTestUtils.setField(docs, "talkSessionMapper", sessions);
        ReflectionTestUtils.setField(docs, "talkStudentRecordMapper", records);
        ReflectionTestUtils.setField(docs, "talkStudentMapper", students);
        ReflectionTestUtils.setField(docs, "talkSessionTagMapper", tags);
        ReflectionTestUtils.setField(docs, "talkAttachmentMapper", attachments);
        ReflectionTestUtils.setField(docs, "talkTagMapper", mock(TalkTagMapper.class));
        ReflectionTestUtils.setField(docs, "sysDictDataMapper", mock(SysDictDataMapper.class));
        ReflectionTestUtils.setField(docs, "sysDeptMapper", depts);
        var session = new TalkSession(); session.setSessionId(10L); session.setCollegeDeptId(100L);
        session.setTalkType("group"); session.setTalkPerson("模拟老师"); session.setTalkContent("共享模拟正文");
        var first = new TalkStudentRecord(); first.setRecordId(1L); first.setSessionId(10L); first.setStudentId(101L); first.setStudentFeedback("甲反馈");
        var second = new TalkStudentRecord(); second.setRecordId(2L); second.setSessionId(10L); second.setStudentId(102L); second.setStudentFeedback("乙反馈"); second.setFollowupPlan("乙跟进");
        var studentA = new TalkStudent(); studentA.setStudentId(101L); studentA.setStudentName("模拟甲"); studentA.setDeptId(201L);
        var studentB = new TalkStudent(); studentB.setStudentId(102L); studentB.setStudentName("模拟乙"); studentB.setDeptId(102L);
        var deptA = new SysDept(); deptA.setDeptName("转入后模拟班级");
        var deptB = new SysDept(); deptB.setDeptName("原学院模拟班级");
        var attachment = new TalkAttachment(); attachment.setAttachmentId(1L); attachment.setFileName("模拟共享附件.txt"); attachment.setFilePath("/profile/upload/shared.txt");
        when(sessions.selectTalkSessionBySessionId(10L)).thenReturn(session);
        when(records.selectTalkStudentRecordBySessionId(10L)).thenReturn(List.of(first, second));
        when(records.selectTalkStudentRecordByRecordId(1L)).thenReturn(first);
        when(records.selectTalkStudentRecordByStudentId(101L)).thenReturn(List.of(first));
        when(records.selectTalkStudentRecordByStudentId(102L)).thenReturn(List.of(second));
        when(students.selectTalkStudentByStudentId(101L)).thenReturn(studentA);
        when(students.selectTalkStudentByStudentId(102L)).thenReturn(studentB);
        when(depts.selectDeptById(201L)).thenReturn(deptA);
        when(depts.selectDeptById(102L)).thenReturn(deptB);
        when(tags.selectTalkSessionTagBySessionId(10L)).thenReturn(List.of());
        when(attachments.selectTalkAttachmentBySessionId(10L)).thenReturn(List.of(attachment));
        Files.createDirectories(temp.resolve("upload"));
        Files.writeString(temp.resolve("upload/shared.txt"), "合成附件内容", StandardCharsets.UTF_8);
        String oldProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(temp.toString());
        try (var permission = mockStatic(TalkPermissionUtils.class)) {
            String summary = text(docs.generateGroupSummaryDocx(10L));
            assertThat(summary).contains("模拟甲", "模拟乙", "转入后模拟班级", "乙反馈", "乙跟进", "共享模拟正文");
            String selected = text(docs.generateDocxByRecord(1L));
            assertThat(selected).contains("模拟甲", "共享模拟正文", "模拟共享附件.txt").doesNotContain("乙反馈", "乙跟进");
            var api = new TalkSessionController(); var sessionService = mock(ITalkSessionService.class);
            when(sessionService.selectTalkSessionBySessionId(10L)).thenReturn(session);
            ReflectionTestUtils.setField(api, "talkSessionService", sessionService);
            ReflectionTestUtils.setField(api, "talkDocxService", docs);
            ReflectionTestUtils.setField(api, "talkStudentRecordMapper", records);
            var response = new MockHttpServletResponse();
            api.exportDocxBatch(Map.of("sessionIds", List.of(10L)), response);
            var outer = unzip(response.getContentAsByteArray());
            assertThat(outer.keySet().stream().filter(n -> n.endsWith(".docx")).count()).isEqualTo(2);
            assertThat(outer.keySet().stream().filter(n -> n.endsWith(".zip")).count()).isZero();
            assertThat(outer.keySet().stream().filter(n -> n.startsWith("附件/") && n.endsWith(".txt")).count()).isEqualTo(1);
            assertThat(outer.values().stream().filter(b -> java.util.Arrays.equals(b, "合成附件内容".getBytes(StandardCharsets.UTF_8))).count()).isEqualTo(1);
            assertThat(response.getHeader("download-filename")).isNotBlank();
        } finally {
            new RuoYiConfig().setProfile(oldProfile);
            Files.deleteIfExists(temp.resolve("upload/shared.txt"));
            Files.deleteIfExists(temp.resolve("upload"));
            Files.deleteIfExists(temp);
        }
    }
}
