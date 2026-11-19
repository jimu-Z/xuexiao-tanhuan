package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.talk.domain.TalkTag;
import com.ruoyi.talk.mapper.TalkTagMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkTagServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TalkTagIdentityTest {
    @Test void createsInternalKeyAndPreservesItOnRename() {
        TalkTagMapper mapper = mock(TalkTagMapper.class);
        TalkTagServiceImpl service = new TalkTagServiceImpl();
        ReflectionTestUtils.setField(service, "talkTagMapper", mapper);
        try (MockedStatic<TalkPermissionUtils> scope = mockStatic(TalkPermissionUtils.class);
             MockedStatic<SecurityUtils> user = mockStatic(SecurityUtils.class)) {
            scope.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            scope.when(TalkPermissionUtils::canManageSystemDefinitions).thenReturn(true);
            user.when(SecurityUtils::getUsername).thenReturn("qa");
            TalkTag first = new TalkTag(); first.setTagName(" 学业 "); first.setTagKey("client_key");
            service.insertTalkTag(first);
            assertThat(first.getTagKey()).matches("tag_[0-9a-f]{32}").isNotEqualTo("client_key");
            assertThat(first.getTagName()).isEqualTo("学业");
            TalkTag second = new TalkTag(); second.setTagName("学业"); service.insertTalkTag(second);
            assertThat(second.getTagKey()).isNotEqualTo(first.getTagKey());
            TalkTag existing = new TalkTag(); existing.setTagId(9L); existing.setTagKey(first.getTagKey()); existing.setScopeType("system");
            when(mapper.selectTalkTagById(9L)).thenReturn(existing);
            TalkTag edit = new TalkTag(); edit.setTagId(9L); edit.setTagName("新名称"); edit.setTagKey("forged");
            service.updateTalkTag(edit);
            ArgumentCaptor<TalkTag> captured = ArgumentCaptor.forClass(TalkTag.class);
            verify(mapper).updateTalkTag(captured.capture());
            assertThat(captured.getValue().getTagKey()).isNull();
            TalkTag blank = new TalkTag(); blank.setTagName("  ");
            assertThatThrownBy(() -> service.insertTalkTag(blank)).isInstanceOf(ServiceException.class).hasMessageContaining("名称");
        }
    }
}
