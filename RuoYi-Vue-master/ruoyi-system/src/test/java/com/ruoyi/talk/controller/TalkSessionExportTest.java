package com.ruoyi.talk.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.domain.TalkStudentRecord;
import com.ruoyi.talk.mapper.TalkStudentRecordMapper;
import com.ruoyi.talk.service.ITalkSessionService;
import com.ruoyi.talk.service.TalkDocxService;

class TalkSessionExportTest {

    private TalkSessionController controller;
    private ITalkSessionService sessions;
    private TalkDocxService documents;
    private TalkStudentRecordMapper records;
    private TalkSession session;

    @BeforeEach
    void setup() {
        controller = new TalkSessionController();
        sessions = mock(ITalkSessionService.class);
        documents = mock(TalkDocxService.class);
        records = mock(TalkStudentRecordMapper.class);
        ReflectionTestUtils.setField(controller, "talkSessionService", sessions);
        ReflectionTestUtils.setField(controller, "talkDocxService", documents);
        ReflectionTestUtils.setField(controller, "talkStudentRecordMapper", records);
        session = new TalkSession();
        session.setSessionId(1L);
        session.setTalkType("group");
        session.setTalkPerson("李老师");
        session.setTalkTime(Timestamp.valueOf("2026-11-13 15:30:00"));
        when(sessions.selectTalkSessionBySessionId(1L)).thenReturn(session);
    }

    private TalkStudentRecord record(long recordId, long studentId, String name, String code) {
        TalkStudentRecord record = new TalkStudentRecord();
        record.setRecordId(recordId);
        record.setSessionId(1L);
        record.setStudentId(studentId);
        record.setStudentName(name);
        record.setStudentCode(code);
        return record;
    }

    private String downloadName(MockHttpServletResponse response) {
        return URLDecoder.decode(response.getHeader("download-filename"), StandardCharsets.UTF_8);
    }

    @Test
    void singleGroupExportReturnsOneZipNamedByTalkTime() throws Exception {
        when(records.selectTalkStudentRecordBySessionId(1L))
                .thenReturn(List.of(record(1, 10, "张三", "20220001")));
        when(documents.generateDocxBySession(1L)).thenReturn(new byte[] { 'P', 'K' });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDocx(1L, response);
        assertThat(response.getContentType()).isEqualTo("application/zip");
        assertThat(downloadName(response)).isEqualTo("谈话记录_李老师_20261113_1530.zip");
        assertThat(response.getContentAsByteArray()).containsExactly((byte) 'P', (byte) 'K');
    }

    @Test
    void singleIndividualExportReturnsDocxNamedByStudent() throws Exception {
        session.setTalkType("individual");
        when(records.selectTalkStudentRecordBySessionId(1L))
                .thenReturn(List.of(record(1, 10, "张三", "20220001")));
        when(documents.generateDocxBySession(1L)).thenReturn(new byte[] { 'P', 'K' });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDocx(1L, response);
        assertThat(downloadName(response)).isEqualTo("张三_李老师_20261113_个别谈话_1530.docx");
        assertThat(response.getContentType()).startsWith("application/vnd.openxmlformats");
    }

    @Test
    void emptySessionDoesNotReturnEmptyArchive() {
        when(records.selectTalkStudentRecordBySessionId(1L)).thenReturn(List.of());
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> controller.exportDocx(1L, response))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("没有参与学生");
        assertThat(response.getContentAsByteArray()).isEmpty();
    }

    @Test
    void singleStudentExportUsesSameNamingRule() throws Exception {
        when(records.selectTalkStudentRecordBySessionId(1L))
                .thenReturn(List.of(record(1, 10, "张三", "20220001"), record(2, 11, "李四", "20220002")));
        when(documents.generateDocxByStudent(11L, 1L)).thenReturn(new byte[] { 'P', 'K' });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDocxForStudent(1L, 11L, response);
        assertThat(downloadName(response)).isEqualTo("李四_李老师_20261113_集体谈话_1530.docx");
    }

    @Test
    void exportOfUnknownSessionReturnsJsonError() throws Exception {
        when(sessions.selectTalkSessionBySessionId(2L)).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDocx(2L, response);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void batchSkipsEmptySessionsAndRejectsAllEmpty() throws Exception {
        TalkSession empty = new TalkSession();
        empty.setSessionId(2L);
        empty.setTalkType("group");
        when(sessions.selectTalkSessionBySessionId(2L)).thenReturn(empty);
        when(records.selectTalkStudentRecordBySessionId(1L))
                .thenReturn(List.of(record(1, 10, "张三", "20220001")));
        when(records.selectTalkStudentRecordBySessionId(2L)).thenReturn(List.of());
        when(documents.generateDocxForRecord(eq(session), any(TalkStudentRecord.class)))
                .thenReturn(new byte[] { 'P', 'K' });
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportDocxBatch(java.util.Map.of("sessionIds", List.of(1L, 2L)), response);
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(response.getContentAsByteArray()))) {
            java.util.zip.ZipEntry entry = zip.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getName()).isEqualTo("张三_李老师_20261113_集体谈话_1530.docx");
            assertThat(zip.getNextEntry()).isNull();
        }
        assertThatThrownBy(() -> controller.exportDocxBatch(
                java.util.Map.of("sessionIds", List.of(2L)), new MockHttpServletResponse()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("没有可导出的学生谈话记录");
    }
}
