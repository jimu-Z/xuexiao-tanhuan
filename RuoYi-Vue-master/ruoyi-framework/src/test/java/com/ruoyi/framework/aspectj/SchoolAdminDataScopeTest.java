package com.ruoyi.framework.aspectj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Set;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SchoolAdminDataScopeTest {
    @DataScope(deptAlias="d", permission="system:dept:list")
    void departmentQuery() {}

    @Test void requestMapSyntaxBindsDataScopeBeforeAspectClearsIt() throws Throwable {
        BaseEntity query = new BaseEntity();
        WebDataBinder binder = new WebDataBinder(query);
        binder.bind(new MutablePropertyValues(java.util.Map.of("params[dataScope]", " AND 1=0 ")));
        assertThat(binder.getBindingResult().hasErrors()).isFalse();
        assertThat(query.getParams().get("dataScope")).isEqualTo(" AND 1=0 ");
        SysUser user = new SysUser(); user.setUserId(1L);
        JoinPoint point = mock(JoinPoint.class); when(point.getArgs()).thenReturn(new Object[]{query});
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getLoginUser).thenReturn(new LoginUser(1L, 100L, user, Set.of("*:*:*")));
            security.when(() -> SecurityUtils.isAdmin(1L)).thenReturn(true);
            new DataScopeAspect().doBefore(point, getClass().getDeclaredMethod("departmentQuery").getAnnotation(DataScope.class));
        }
        assertThat(query.getParams().get("dataScope")).isEqualTo("");
    }

    String scope(Long userId, String key, String status, String dataScope, Set<String> permissions) throws Throwable {
        SysRole role = new SysRole(); role.setRoleKey(key); role.setStatus(status);
        role.setDataScope(dataScope); role.setPermissions(permissions);
        SysUser user = new SysUser(); user.setUserId(userId); user.setDeptId(100L); user.setRoles(List.of(role));
        BaseEntity query = new BaseEntity(); query.getParams().put("dataScope", " OR 1=1");
        JoinPoint point = mock(JoinPoint.class); when(point.getArgs()).thenReturn(new Object[]{query});
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(() -> SecurityUtils.isAdmin(userId)).thenReturn(userId == 1L);
            security.when(SecurityUtils::getLoginUser).thenReturn(new LoginUser(userId, 100L, user, Set.of("*:*:*")));
            new DataScopeAspect().doBefore(point, getClass().getDeclaredMethod("departmentQuery").getAnnotation(DataScope.class));
        } finally { RequestContextHolder.resetRequestAttributes(); }
        return (String) query.getParams().get("dataScope");
    }

    @Test void schoolAdministratorHasGlobalScopeWithoutPerRoleMenuPermissions() throws Throwable {
        assertThat(scope(42L, "school_admin", "0", "1", null)).isEmpty();
    }
    @Test void disabledSchoolAdministratorDoesNotGainGlobalScope() throws Throwable {
        assertThat(scope(42L, "school_admin", "1", "1", null)).contains("d.dept_id = 0");
    }
    @ParameterizedTest @ValueSource(strings={"college_admin", "talk_teacher"})
    void collegeRolesRetainTheirDepartmentBoundary(String key) throws Throwable {
        assertThat(scope(42L, key, "0", "4", Set.of("system:dept:list")))
            .contains("dept_id = 100", "find_in_set( 100 , ancestors )").doesNotContain("1=1");
    }
    @Test void unrelatedAllDataRoleWithoutMatchingPermissionStillHasNoData() throws Throwable {
        assertThat(scope(42L, "other", "0", "1", null)).contains("d.dept_id = 0");
    }
    @Test void builtinAdministratorStillHasGlobalScopeAndClearsInjectedScope() throws Throwable {
        assertThat(scope(1L, "admin", "0", "1", null)).isEmpty();
    }

    @Test void unknownRoleStatusCannotExpandInternalQueryScope() {
        SysRole role = new SysRole(); role.setStatus("2"); role.setDataScope("1");
        SysUser user = new SysUser(); user.setUserId(42L); user.setDeptId(100L); user.setRoles(List.of(role));
        BaseEntity query = new BaseEntity();
        JoinPoint point = mock(JoinPoint.class); when(point.getArgs()).thenReturn(new Object[]{query});
        DataScopeAspect.dataScopeFilter(point, user, "d", "u", "");
        assertThat((String) query.getParams().get("dataScope")).contains("d.dept_id = 0");
    }

    @Test void invalidAndDeletedRolesCannotExpandActiveDepartmentScope() {
        SysRole active = new SysRole(); active.setStatus("0"); active.setDataScope("3");
        SysRole invalid = new SysRole(); invalid.setStatus("2"); invalid.setDataScope("1");
        SysRole deleted = new SysRole(); deleted.setStatus("0"); deleted.setDelFlag("2"); deleted.setDataScope("1");
        SysUser user = new SysUser(); user.setUserId(42L); user.setDeptId(100L); user.setRoles(List.of(active, invalid, deleted));
        BaseEntity query = new BaseEntity();
        JoinPoint point = mock(JoinPoint.class); when(point.getArgs()).thenReturn(new Object[]{query});
        DataScopeAspect.dataScopeFilter(point, user, "d", "u", "");
        assertThat((String) query.getParams().get("dataScope")).contains("d.dept_id = 100");
    }

    @Test void unknownDataScopeDeniesRatherThanReturningAllDepartments() throws Throwable {
        assertThat(scope(42L, "other", "0", "9", Set.of("system:dept:list")))
                .contains("d.dept_id = 0");
    }

    @Test void missingDataScopeDeniesRatherThanReturningAllDepartments() throws Throwable {
        assertThat(scope(42L, "other", "0", null, Set.of("system:dept:list")))
                .contains("d.dept_id = 0");
    }
}
