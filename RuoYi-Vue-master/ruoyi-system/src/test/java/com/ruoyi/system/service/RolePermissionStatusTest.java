package com.ruoyi.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RolePermissionStatusTest {
    @Test void returnedRoleKeysExcludeDisabledRoles() {
        SysRole enabled = new SysRole(); enabled.setRoleKey("talk_teacher"); enabled.setStatus("0");
        SysRole disabled = new SysRole(); disabled.setRoleKey("admin"); disabled.setStatus("1");
        SysRoleMapper mapper = mock(SysRoleMapper.class);
        when(mapper.selectRolePermissionByUserId(42L)).thenReturn(List.of(enabled, disabled));
        SysRoleServiceImpl service = new SysRoleServiceImpl();
        ReflectionTestUtils.setField(service, "roleMapper", mapper);
        assertThat(service.selectRolePermissionByUserId(42L)).containsExactly("talk_teacher");
    }
}
