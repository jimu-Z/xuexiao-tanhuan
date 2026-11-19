package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.talk.domain.TalkAttachment;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.mapper.TalkAttachmentMapper;
import com.ruoyi.talk.mapper.TalkSessionMapper;
import com.ruoyi.talk.mapper.TalkSessionParticipantMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkAttachmentServiceImpl;

class AttachmentDeleteFailureAuditTest {
    @Test void databaseFailureOccursAfterPhysicalFileDeletion() throws Exception {
        Path root = Path.of("..", "..", "qa-runtime", "attachment-failure-audit-temp").toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path file = Files.writeString(root.resolve("simulated.txt"), "synthetic");
        try {
            var service = new TalkAttachmentServiceImpl();
            var attachments = mock(TalkAttachmentMapper.class);
            var sessions = mock(TalkSessionMapper.class);
            var participants = mock(TalkSessionParticipantMapper.class);
            ReflectionTestUtils.setField(service, "profile", root.toString());
            ReflectionTestUtils.setField(service, "talkAttachmentMapper", attachments);
            ReflectionTestUtils.setField(service, "talkSessionMapper", sessions);
            ReflectionTestUtils.setField(service, "participantMapper", participants);
            var item = new TalkAttachment(); item.setAttachmentId(1L); item.setSessionId(10L); item.setFilePath("/profile/simulated.txt");
            var session = new TalkSession(); session.setSessionId(10L);
            when(attachments.selectTalkAttachmentById(1L)).thenReturn(item);
            when(sessions.selectTalkSessionBySessionId(10L)).thenReturn(session);
            when(attachments.deleteTalkAttachmentById(1L)).thenThrow(new IllegalStateException("simulated database failure"));
            try (var permission = mockStatic(TalkPermissionUtils.class)) {
                assertThatThrownBy(() -> service.deleteTalkAttachmentByIds(new Long[]{1L}))
                        .hasMessage("simulated database failure");
                assertThat(file).doesNotExist();
                verify(attachments).deleteTalkAttachmentById(1L);
            }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(root);
        }
    }
}
