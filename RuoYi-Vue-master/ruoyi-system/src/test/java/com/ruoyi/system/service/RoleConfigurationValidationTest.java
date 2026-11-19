package com.ruoyi.system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RoleConfigurationValidationTest {
    private SysRoleServiceImpl service;
    private SysRoleMapper mapper;
    @BeforeEach void setup() {
        service=new SysRoleServiceImpl(); mapper=mock(SysRoleMapper.class);
        ReflectionTestUtils.setField(service,"roleMapper",mapper);
    }
    @Test void invalidScopeIsRejectedBeforeScopeUpdate() {
        SysRole role=new SysRole();role.setRoleId(42L);role.setDataScope("9");
        assertThatThrownBy(()->service.authDataScope(role)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(mapper);
    }
    @Test void invalidStatusIsRejectedBeforeStatusUpdate() {
        SysRole role=new SysRole();role.setRoleId(42L);role.setStatus("2");
        assertThatThrownBy(()->service.updateRoleStatus(role)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(mapper);
    }
    @Test void invalidScopeIsRejectedBeforeRoleCreation() {
        SysRole role=new SysRole();role.setDataScope("9");
        assertThatThrownBy(()->service.insertRole(role)).isInstanceOf(ServiceException.class);
        verifyNoInteractions(mapper);
    }
}
