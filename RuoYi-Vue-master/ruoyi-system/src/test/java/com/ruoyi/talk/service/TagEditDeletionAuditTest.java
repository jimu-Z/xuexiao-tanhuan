package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.talk.domain.TalkTag;
import com.ruoyi.talk.mapper.TalkTagMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkTagServiceImpl;

class TagEditDeletionAuditTest {
    @Test void editPassesRequestedDeletionFlagIntoUpdateSql() throws Exception {
        var mapper = mock(TalkTagMapper.class); var service = new TalkTagServiceImpl();
        ReflectionTestUtils.setField(service, "talkTagMapper", mapper);
        var existing = new TalkTag(); existing.setTagId(7L); existing.setScopeType("college"); existing.setCollegeDeptId(100L);
        when(mapper.selectTalkTagById(7L)).thenReturn(existing);
        when(mapper.updateTalkTag(any())).thenReturn(1);
        var submitted = new TalkTag(); submitted.setTagId(7L); submitted.setDelFlag("2");
        try (var permission = mockStatic(TalkPermissionUtils.class); var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUsername).thenReturn("simulated");
            assertThat(service.updateTalkTag(submitted)).isEqualTo(1);
            permission.verify(() -> TalkPermissionUtils.requireCollegeAccess(100L));
        }
        var captured = ArgumentCaptor.forClass(TalkTag.class); verify(mapper).updateTalkTag(captured.capture());
        assertThat(captured.getValue().getDelFlag()).isEqualTo("2");
        var config = new Configuration(); config.getTypeAliasRegistry().registerAlias("TalkTag", TalkTag.class);
        try (var xml = getClass().getResourceAsStream("/mapper/talk/TalkTagMapper.xml")) {
            new XMLMapperBuilder(xml, config, "TalkTagMapper.xml", config.getSqlFragments()).parse();
        }
        var sql = config.getMappedStatement("com.ruoyi.talk.mapper.TalkTagMapper.updateTalkTag").getBoundSql(captured.getValue());
        assertThat(sql.getParameterMappings()).extracting(p -> p.getProperty()).contains("delFlag");
        assertThat(sql.getSql()).contains("del_flag = ?");
    }
}
