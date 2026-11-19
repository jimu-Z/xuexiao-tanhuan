package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.talk.domain.TalkAlert;
import com.ruoyi.talk.mapper.TalkAlertMapper;
import com.ruoyi.talk.service.impl.TalkAlertServiceImpl;

class AlertRecoveryAuditTest {
    @Test void pendingAutoAlertPreventsNormalStatusFromReachingClosePath() {
        var mapper = mock(TalkAlertMapper.class);
        var service = new TalkAlertServiceImpl();
        ReflectionTestUtils.setField(service, "talkAlertMapper", mapper);
        var pending = new TalkAlert();
        pending.setAlertId(1L); pending.setStudentId(7L);
        pending.setAlertType("auto"); pending.setAlertStatus("pending");
        when(mapper.selectTalkAlertList(any(TalkAlert.class))).thenReturn(List.of(pending));
        assertThat(service.autoGenerateAlertForStudent(7L, "normal")).isFalse();
        verify(mapper, never()).selectPendingAutoAlertsByStudentId(anyLong());
        verify(mapper, never()).updateTalkAlert(any(TalkAlert.class));
        assertThat(pending.getAlertStatus()).isEqualTo("pending");
    }

    @Test void withoutPendingDuplicateNormalStatusReachesClosePath() {
        var mapper = mock(TalkAlertMapper.class);
        var service = new TalkAlertServiceImpl();
        ReflectionTestUtils.setField(service, "talkAlertMapper", mapper);
        var inProgress = new TalkAlert(); inProgress.setAlertId(2L); inProgress.setAlertStatus("in_progress");
        when(mapper.selectTalkAlertList(any(TalkAlert.class))).thenReturn(List.of());
        when(mapper.selectPendingAutoAlertsByStudentId(7L)).thenReturn(List.of(inProgress));
        assertThat(service.autoGenerateAlertForStudent(7L, "normal")).isFalse();
        verify(mapper).updateTalkAlert(inProgress);
        assertThat(inProgress.getAlertStatus()).isEqualTo("resolved");
    }
}
