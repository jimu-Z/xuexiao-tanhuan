package com.ruoyi.framework.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Set;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.web.service.PermissionService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.talk.security.TalkPermissionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

class RoleStatusAuthorizationTest {
    private final PermissionService permissions = new PermissionService();

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void authenticate(SysRole... roles) {
        SysUser user = new SysUser(); user.setUserId(42L); user.setRoles(List.of(roles));
        LoginUser login = new LoginUser(42L, 100L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }

    private SysRole role(String key, String status) {
        SysRole role = new SysRole(); role.setRoleKey(key); role.setStatus(status); role.setDelFlag("0"); return role;
    }

    @Test void disabledAdminDoesNotGrantAdminOrOtherRoles() {
        authenticate(role("admin", "1"), role("talk_teacher", "0"));
        assertThat(permissions.hasRole("admin")).isFalse();
        assertThat(permissions.hasRole("college_admin")).isFalse();
        assertThat(permissions.hasRole("talk_teacher")).isTrue();
        assertThat(permissions.hasAnyRoles("admin,college_admin")).isFalse();
    }

    @Test void enabledAdminStillGrantsRoleChecks() {
        authenticate(role("admin", "0"));
        assertThat(permissions.hasRole("admin")).isTrue();
        assertThat(permissions.hasRole("college_admin")).isTrue();
    }

    @Test void missingRoleStatusDoesNotGrantAccess() {
        authenticate(role("admin", null));
        assertThat(permissions.hasRole("admin")).isFalse();
    }

    @Test void deletedAdminRoleDoesNotGrantRoleChecks() {
        SysRole deletedAdmin = role("admin", "0"); deletedAdmin.setDelFlag("2");
        authenticate(deletedAdmin);
        assertThat(permissions.hasRole("admin")).isFalse();
    }

    @Test void deletedSchoolRoleDoesNotGrantGlobalAccess() {
        SysRole deletedSchool = role("school_admin", "0"); deletedSchool.setDelFlag("2");
        authenticate(deletedSchool);
        assertThat(TalkPermissionUtils.isGlobalAdmin()).isFalse();
    }

    @Test void deletedSchoolRoleDoesNotBecomeFrontendAdminAlias() {
        SysRole deletedSchool = role("school_admin", "0"); deletedSchool.setDelFlag("2");
        authenticate(deletedSchool);
        SysPermissionService service = new SysPermissionService();
        ISysRoleService roles = mock(ISysRoleService.class);
        when(roles.selectRolePermissionByUserId(42L)).thenReturn(Set.of());
        ReflectionTestUtils.setField(service, "roleService", roles);
        assertThat(service.getRolePermission(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof LoginUser login ? login.getUser() : null))
                .doesNotContain("admin");
    }
}
