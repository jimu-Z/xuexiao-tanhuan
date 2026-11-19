package com.ruoyi.talk.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ruoyi.talk.domain.TalkAttachment;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.domain.TalkStudentRecord;

class TalkExportNamingTest {

    private static final Timestamp TALK_TIME = Timestamp.valueOf("2026-11-13 15:30:00");

    private TalkSession session(String type, String person) {
        TalkSession session = new TalkSession();
        session.setSessionId(10L);
        session.setTalkType(type);
        session.setTalkPerson(person);
        session.setTalkTime(TALK_TIME);
        return session;
    }

    private TalkStudentRecord record(String name, String code, long studentId) {
        TalkStudentRecord record = new TalkStudentRecord();
        record.setRecordId(studentId + 100);
        record.setSessionId(10L);
        record.setStudentId(studentId);
        record.setStudentName(name);
        record.setStudentCode(code);
        return record;
    }

    @Test
    void buildsStudentCentricDocxName() {
        List<String> names = TalkExportNaming.docxNames(session("group", "李老师"),
                List.of(record("张三", "20220001", 1L)));
        assertThat(names).containsExactly("张三_李老师_20261113_集体谈话_1530.docx");
    }

    @Test
    void usesFixedLabelsForMissingTimeAndPerson() {
        TalkSession unknown = new TalkSession();
        unknown.setTalkType("individual");
        List<String> names = TalkExportNaming.docxNames(unknown, List.of(record("李四", "20220002", 2L)));
        assertThat(names).containsExactly("李四_未填写_未知日期_个别谈话_未知时间.docx");
    }

    @Test
    void stripsIllegalCharactersAndPathComponents() {
        List<String> names = TalkExportNaming.docxNames(session("individual", "../../王/老师"),
                List.of(record("../../张三", "20220001", 1L)));
        assertThat(names).containsExactly("张三_老师_20261113_个别谈话_1530.docx");
    }

    @Test
    void appendsStudentCodeWhenStudentNamesCollide() {
        List<String> names = TalkExportNaming.docxNames(session("group", "李老师"),
                List.of(record("张三", "20220001", 1L), record("张三", "20220002", 2L)));
        assertThat(names).containsExactly(
                "张三(20220001)_李老师_20261113_集体谈话_1530.docx",
                "张三(20220002)_李老师_20261113_集体谈话_1530.docx");
    }

    @Test
    void appendsSuffixWhenStudentCodeAlsoCollides() {
        List<String> names = TalkExportNaming.docxNames(session("group", "李老师"),
                List.of(record("张三", "", 1L), record("张三", "", 2L)));
        assertThat(names).containsExactly(
                "张三(无学号)_李老师_20261113_集体谈话_1530.docx",
                "张三(无学号)_李老师_20261113_集体谈话_1530_2.docx");
    }

    @Test
    void namesSingleAndBatchZipFiles() {
        assertThat(TalkExportNaming.zipName(session("group", "李老师")))
                .isEqualTo("谈话记录_李老师_20261113_1530.zip");
        assertThat(TalkExportNaming.zipName(List.of("李老师"), LocalDateTime.of(2026, 11, 13, 16, 5)))
                .isEqualTo("谈话记录_李老师_20261113_1605.zip");
        assertThat(TalkExportNaming.zipName(List.of("李老师", "王老师", "李老师"),
                LocalDateTime.of(2026, 11, 13, 16, 5)))
                .isEqualTo("谈话记录_李老师等2人_20261113_1605.zip");
    }

    @Test
    void namesAttachmentWithOriginalNameFirst() {
        TalkAttachment attachment = new TalkAttachment();
        attachment.setAttachmentId(7L);
        attachment.setFileName("谈话照片.jpg");
        assertThat(TalkExportNaming.attachmentName(session("group", "李老师"), attachment, 1))
                .isEqualTo("谈话照片_20261113_李老师_集体谈话_1.jpg");
    }

    @Test
    void attachmentNameCannotContainDirectories() {
        TalkAttachment attachment = new TalkAttachment();
        attachment.setAttachmentId(7L);
        attachment.setFileName("../../秘密.txt");
        assertThat(TalkExportNaming.attachmentName(session("individual", "李老师"), attachment, 2))
                .isEqualTo("秘密_20261113_李老师_个别谈话_2.txt");
    }

    @Test
    void uniqueNameAddsSuffixOnlyOnCollision() {
        Set<String> used = new HashSet<>();
        assertThat(TalkExportNaming.uniqueName("记录.txt", used)).isEqualTo("记录.txt");
        assertThat(TalkExportNaming.uniqueName("记录.txt", used)).isEqualTo("记录_2.txt");
        assertThat(TalkExportNaming.uniqueName("记录.txt", used)).isEqualTo("记录_3.txt");
    }
}
