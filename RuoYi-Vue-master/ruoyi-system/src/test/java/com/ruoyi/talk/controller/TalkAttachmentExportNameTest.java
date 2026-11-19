package com.ruoyi.talk.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.talk.domain.TalkAttachment;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.mapper.TalkSessionMapper;
import com.ruoyi.talk.service.ITalkAttachmentService;

class TalkAttachmentExportNameTest {

    private TalkAttachmentController controller;
    private ITalkAttachmentService attachments;
    private TalkSessionMapper sessions;
    private Path root;
    private String oldProfile;

    @BeforeEach
    void setup() throws Exception {
        root = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "qa-attachment-");
        Files.createDirectories(root.resolve("upload"));
        oldProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(root.toString());
        controller = new TalkAttachmentController();
        attachments = mock(ITalkAttachmentService.class);
        sessions = mock(TalkSessionMapper.class);
        ReflectionTestUtils.setField(controller, "talkAttachmentService", attachments);
        ReflectionTestUtils.setField(controller, "talkSessionMapper", sessions);
    }

    @AfterEach
    void restore() {
        new RuoYiConfig().setProfile(oldProfile);
    }

    private TalkAttachment attachment() {
        TalkAttachment attachment = new TalkAttachment();
        attachment.setAttachmentId(7L);
        attachment.setSessionId(1L);
        attachment.setFileName("谈话照片.jpg");
        attachment.setFilePath("/profile/upload/photo.jpg");
        return attachment;
    }

    @Test
    void exportNameAddsTalkContextAndSequence() throws Exception {
        Files.writeString(root.resolve("upload/photo.jpg"), "IMAGE");
        TalkAttachment attachment = attachment();
        when(attachments.selectTalkAttachmentById(7L)).thenReturn(attachment);
        when(attachments.selectTalkAttachmentBySessionId(1L)).thenReturn(List.of(attachment));
        TalkSession session = new TalkSession();
        session.setSessionId(1L);
        session.setTalkType("group");
        session.setTalkPerson("李老师");
        session.setTalkTime(Timestamp.valueOf("2026-11-13 15:30:00"));
        when(sessions.selectTalkSessionBySessionId(1L)).thenReturn(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.download(7L, true, response);
        assertThat(URLDecoder.decode(response.getHeader("download-filename"), StandardCharsets.UTF_8))
                .isEqualTo("谈话照片_20261113_李老师_集体谈话_1.jpg");
    }

    @Test
    void normalPageDownloadKeepsOriginalName() throws Exception {
        Files.writeString(root.resolve("upload/photo.jpg"), "IMAGE");
        when(attachments.selectTalkAttachmentById(7L)).thenReturn(attachment());
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.download(7L, false, response);
        assertThat(URLDecoder.decode(response.getHeader("download-filename"), StandardCharsets.UTF_8))
                .isEqualTo("谈话照片.jpg");
    }
}
