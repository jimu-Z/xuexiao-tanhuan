package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.talk.domain.*;
import com.ruoyi.talk.mapper.*;
import com.ruoyi.talk.security.TalkPermissionUtils;

class TalkDocxTransferAuditTest {
    @Test void oldCollegeRecordTemplateContainsCurrentClassAndOtherCollegeHistory() throws Exception {
        var service = new TalkDocxService();
        var sessions = mock(TalkSessionMapper.class);
        var records = mock(TalkStudentRecordMapper.class);
        var students = mock(TalkStudentMapper.class);
        var tags = mock(TalkSessionTagMapper.class);
        var departments = mock(SysDeptMapper.class);
        ReflectionTestUtils.setField(service, "templatePath", "templates/学生谈心谈话记录表.docx");
        ReflectionTestUtils.setField(service, "talkSessionMapper", sessions);
        ReflectionTestUtils.setField(service, "talkStudentRecordMapper", records);
        ReflectionTestUtils.setField(service, "talkStudentMapper", students);
        ReflectionTestUtils.setField(service, "talkSessionTagMapper", tags);
        ReflectionTestUtils.setField(service, "talkAttachmentMapper", mock(TalkAttachmentMapper.class));
        ReflectionTestUtils.setField(service, "talkTagMapper", mock(TalkTagMapper.class));
        ReflectionTestUtils.setField(service, "sysDictDataMapper", mock(SysDictDataMapper.class));
        ReflectionTestUtils.setField(service, "sysDeptMapper", departments);
        var oldSession = new TalkSession(); oldSession.setSessionId(10L); oldSession.setCollegeDeptId(100L); oldSession.setTalkType("individual"); oldSession.setTalkPerson("原学院老师");
        var newSession = new TalkSession(); newSession.setSessionId(20L); newSession.setCollegeDeptId(200L); newSession.setTalkType("individual"); newSession.setTalkPerson("新学院模拟老师");
        var oldRecord = new TalkStudentRecord(); oldRecord.setRecordId(1L); oldRecord.setSessionId(10L); oldRecord.setStudentId(101L);
        var newRecord = new TalkStudentRecord(); newRecord.setRecordId(2L); newRecord.setSessionId(20L); newRecord.setStudentId(101L);
        var student = new TalkStudent(); student.setStudentId(101L); student.setStudentName("模拟学生"); student.setDeptId(201L);
        var dept = new SysDept(); dept.setDeptName("新学院模拟班级");
        when(records.selectTalkStudentRecordByRecordId(1L)).thenReturn(oldRecord);
        when(records.selectTalkStudentRecordByStudentId(101L)).thenReturn(List.of(oldRecord, newRecord));
        when(sessions.selectTalkSessionBySessionId(10L)).thenReturn(oldSession);
        when(sessions.selectTalkSessionBySessionId(20L)).thenReturn(newSession);
        when(students.selectTalkStudentByStudentId(101L)).thenReturn(student);
        when(departments.selectDeptById(201L)).thenReturn(dept);
        when(tags.selectTalkSessionTagBySessionId(10L)).thenReturn(List.of());
        try (var permissions = mockStatic(TalkPermissionUtils.class)) {
            byte[] bytes = service.generateDocxByRecord(1L);
            permissions.verify(() -> TalkPermissionUtils.requireCollegeAccess(100L));
            try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes)); var extractor = new XWPFWordExtractor(doc)) {
                assertThat(extractor.getText()).contains("新学院模拟班级", "新学院模拟老师");
            }
        }
    }
}
