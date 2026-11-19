package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.talk.domain.TalkAlert;
import com.ruoyi.talk.domain.TalkAppointment;
import com.ruoyi.talk.mapper.TalkAlertMapper;
import com.ruoyi.talk.mapper.TalkAppointmentMapper;
import com.ruoyi.talk.service.impl.TalkAlertServiceImpl;
import com.ruoyi.talk.service.impl.TalkAppointmentServiceImpl;

class RetainedFeatureScopeAuditTest {
    @Test void ordinaryCurrentTeacherRoleAddsNoAlertListScope() {
        var mapper = mock(TalkAlertMapper.class); var service = new TalkAlertServiceImpl();
        ReflectionTestUtils.setField(service, "talkAlertMapper", mapper);
        var query = new TalkAlert();
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getUsername).thenReturn("simulated-teacher");
            security.when(() -> SecurityUtils.hasRole("talk_teacher")).thenReturn(true);
            service.selectTalkAlertList(query);
            assertThat(query.getParams()).doesNotContainKeys("counselorUsername", "secretaryDeptId", "collegeDeptId");
            verify(mapper).selectTalkAlertList(query);
        }
    }
    @Test void ordinaryCurrentTeacherRoleAddsNoAppointmentListScope() {
        var mapper = mock(TalkAppointmentMapper.class); var service = new TalkAppointmentServiceImpl();
        ReflectionTestUtils.setField(service, "talkAppointmentMapper", mapper);
        var query = new TalkAppointment();
        try (var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getUsername).thenReturn("simulated-teacher");
            security.when(() -> SecurityUtils.hasRole("talk_teacher")).thenReturn(true);
            service.selectTalkAppointmentList(query);
            assertThat(query.getParams()).doesNotContainKeys("studentUsername", "counselorUsername", "secretaryDeptId", "collegeDeptId");
            verify(mapper).selectTalkAppointmentList(query);
        }
    }
}
