package com.ruoyi.talk.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ruoyi.talk.domain.*;
import com.ruoyi.talk.mapper.*;
import com.ruoyi.talk.controller.TalkSessionController;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class TalkGroupArchiveTest {
    private static TalkSession session(long id, String type, String person) {
        TalkSession session = new TalkSession();
        session.setSessionId(id);
        session.setTalkType(type);
        session.setTalkPerson(person);
        session.setTalkTime(Timestamp.valueOf("2026-11-13 15:30:00"));
        return session;
    }

    private static TalkStudentRecord record(long recordId, long studentId, String name, String code) {
        TalkStudentRecord record = new TalkStudentRecord();
        record.setRecordId(recordId);
        record.setSessionId(1L);
        record.setStudentId(studentId);
        record.setStudentName(name);
        record.setStudentCode(code);
        return record;
    }

    @Test void missingTemplateStillExportsSeparateStudentsAsGroupZip() throws Exception {
        TalkDocxService service=new TalkDocxService();ReflectionTestUtils.setField(service,"templatePath","qa-missing-template.docx");
        TalkSessionMapper sessions=mock(TalkSessionMapper.class);ReflectionTestUtils.setField(service,"talkSessionMapper",sessions);TalkSession session=session(1L,"group","李老师");when(sessions.selectTalkSessionBySessionId(1L)).thenReturn(session);
        TalkStudentRecordMapper records=mock(TalkStudentRecordMapper.class);ReflectionTestUtils.setField(service,"talkStudentRecordMapper",records);
        TalkStudentRecord r1=record(1,10,"张三","20220001");TalkStudentRecord r2=record(2,11,"李四","20220002");when(records.selectTalkStudentRecordBySessionId(1L)).thenReturn(List.of(r1,r2));
        TalkStudentMapper students=mock(TalkStudentMapper.class);ReflectionTestUtils.setField(service,"talkStudentMapper",students);
        for(long id:new long[]{10,11}){TalkStudent student=new TalkStudent();student.setStudentId(id);student.setStudentName(id==10?"../../张三":"李四");student.setStudentCode(id==10?"20220001":"20220002");when(students.selectTalkStudentByStudentId(id)).thenReturn(student);}
        TalkSessionTagMapper tags=mock(TalkSessionTagMapper.class);ReflectionTestUtils.setField(service,"talkSessionTagMapper",tags);TalkSessionTag coded=new TalkSessionTag();coded.setTagValue("[\"mental_health\",\"study_style\"]");TalkSessionTag chinese=new TalkSessionTag();chinese.setTagValue("学习辅导");when(tags.selectTalkSessionTagBySessionId(1L)).thenReturn(List.of(coded,chinese));
        ReflectionTestUtils.setField(service,"talkAttachmentMapper",mock(TalkAttachmentMapper.class));
        List<String> names=new ArrayList<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(service.generateDocxBySession(1L)))){ZipEntry e;while((e=zip.getNextEntry())!=null){names.add(e.getName());byte[] data=zip.readAllBytes();assertThat(data).startsWith((byte)'P',(byte)'K');try(org.apache.poi.xwpf.usermodel.XWPFDocument document=new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(data))){String text=document.getParagraphs().stream().map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText).collect(java.util.stream.Collectors.joining("\n"));assertThat(text).contains("心理健康教育与咨询","学风建设","学习辅导").doesNotContain("mental_health","study_style");}}}
        assertThat(names).hasSize(2).doesNotHaveDuplicates().allMatch(n->n.endsWith(".docx")&&!n.contains("/")&&!n.contains("\\"));
        assertThat(names).contains("张三_李老师_20261113_集体谈话_1530.docx","李四_李老师_20261113_集体谈话_1530.docx");
    }

    @Test void batchGroupDocumentIsFlatZipWithStudentNames() throws Exception {
        TalkSessionController controller=new TalkSessionController();ITalkSessionService sessions=mock(ITalkSessionService.class);TalkDocxService docs=mock(TalkDocxService.class);TalkStudentRecordMapper records=mock(TalkStudentRecordMapper.class);
        ReflectionTestUtils.setField(controller,"talkSessionService",sessions);ReflectionTestUtils.setField(controller,"talkDocxService",docs);ReflectionTestUtils.setField(controller,"talkStudentRecordMapper",records);
        TalkSession session=session(1L,"group","../../老师");when(sessions.selectTalkSessionBySessionId(1L)).thenReturn(session);
        TalkStudentRecord role=record(8,10,"张三","20220001");role.setSessionId(1L);
        TalkStudentRecord role2=record(9,11,"李四","20220002");role2.setSessionId(1L);
        when(records.selectTalkStudentRecordBySessionId(1L)).thenReturn(List.of(role,role2));
        when(docs.generateDocxForRecord(eq(session),any(TalkStudentRecord.class))).thenReturn(new byte[]{'P','K'});
        MockHttpServletResponse response=new MockHttpServletResponse();controller.exportDocxBatch(Map.of("sessionIds",List.of(1L,1L)),response);
        List<String> names=new ArrayList<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))){ZipEntry e;while((e=zip.getNextEntry())!=null){names.add(e.getName());zip.readAllBytes();}}
        assertThat(names).containsExactly("张三_老师_20261113_集体谈话_1530.docx","李四_老师_20261113_集体谈话_1530.docx");
        assertThat(names).noneMatch(n->n.endsWith(".zip"));
        assertThat(URLDecoder.decode(response.getHeader("download-filename"), StandardCharsets.UTF_8))
                .startsWith("谈话记录_老师_").endsWith(".zip");
        assertThat(response.getContentType()).isEqualTo("application/zip");
    }
}
