package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.mapper.TalkSessionParticipantMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TalkParticipantWriteAccessTest {
    @Test void onlyCreatorOrRecordedTeacherMayWriteWithinCollege() {
        TalkSession session = new TalkSession(); session.setSessionId(42L); session.setCollegeDeptId(8L); session.setCreateBy("creator");
        TalkSessionParticipantMapper mapper = mock(TalkSessionParticipantMapper.class);
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class, CALLS_REAL_METHODS);
             MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            permissions.when(() -> TalkPermissionUtils.requireCollegeAccess(8L)).thenAnswer(inv -> null);
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(false);
            permissions.when(TalkPermissionUtils::isCollegeAdmin).thenReturn(false);
            permissions.when(TalkPermissionUtils::isTeacher).thenReturn(true);
            security.when(SecurityUtils::getUsername).thenReturn("other");
            security.when(SecurityUtils::getUserId).thenReturn(7L);
            assertThatThrownBy(() -> TalkPermissionUtils.requireSessionWrite(session, mapper)).isInstanceOf(ServiceException.class);
            when(mapper.countUser(42L, 7L)).thenReturn(1);
            assertThatCode(() -> TalkPermissionUtils.requireSessionWrite(session, mapper)).doesNotThrowAnyException();
            permissions.when(() -> TalkPermissionUtils.requireCollegeAccess(8L)).thenThrow(new ServiceException("学院隔离"));
            assertThatThrownBy(() -> TalkPermissionUtils.requireSessionWrite(session, mapper)).hasMessageContaining("学院隔离");
        }
    }
}
