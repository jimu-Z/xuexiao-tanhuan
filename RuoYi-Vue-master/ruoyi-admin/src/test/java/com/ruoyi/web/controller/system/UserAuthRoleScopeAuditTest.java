package com.ruoyi.web.controller.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

class UserAuthRoleScopeAuditTest {
    @Test void roleReadDoesNotCallUserScopeCheck() {
        var controller = new SysUserController();
        var users = mock(ISysUserService.class); var roles = mock(ISysRoleService.class);
        ReflectionTestUtils.setField(controller, "userService", users);
        ReflectionTestUtils.setField(controller, "roleService", roles);
        doThrow(new ServiceException("模拟目标不在用户范围")).when(users).checkUserDataScope(200L);
        var otherUser = new SysUser(); otherUser.setUserId(200L); otherUser.setNickName("模拟目标");
        when(users.selectUserById(200L)).thenReturn(otherUser);
        when(roles.selectRolesByUserId(200L)).thenReturn(List.of());
        try (var security = mockStatic(SecurityUtils.class)) {
            assertThat(controller.authRole(200L).get("user")).isSameAs(otherUser);
            verify(users, never()).checkUserDataScope(200L);
        }
    }
}
