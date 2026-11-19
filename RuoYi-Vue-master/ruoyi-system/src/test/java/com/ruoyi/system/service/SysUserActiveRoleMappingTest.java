package com.ruoyi.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.entity.SysMenu;

class SysUserActiveRoleMappingTest {
    @Test void userLookupExcludesDeletedRolesAndMapsDeletionStatus() throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("SysUser", SysUser.class);
        config.getTypeAliasRegistry().registerAlias("SysDept", SysDept.class);
        config.getTypeAliasRegistry().registerAlias("SysRole", SysRole.class);
        String resource = "mapper/system/SysUserMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
        }
        String sql = config.getMappedStatement("com.ruoyi.system.mapper.SysUserMapper.selectUserById")
                .getBoundSql(42L).getSql().replaceAll("\\s+", " ");
        assertThat(sql).contains("r.del_flag = '0'").contains("r.del_flag as role_del_flag");
        assertThat(config.getResultMap("com.ruoyi.system.mapper.SysUserMapper.RoleResult").getMappedColumns())
                .contains("ROLE_DEL_FLAG");
    }

    @Test void fallbackMenuPermissionLookupExcludesDeletedRoles() throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("SysMenu", SysMenu.class);
        String resource = "mapper/system/SysMenuMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
        }
        String sql = config.getMappedStatement("com.ruoyi.system.mapper.SysMenuMapper.selectMenuPermsByUserId")
                .getBoundSql(42L).getSql().replaceAll("\\s+", " ");
        assertThat(sql).contains("r.del_flag = '0'").contains("r.status = '0'");
    }

    @Test void menuLookupsExcludeDisabledAndDeletedRoles() throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("SysMenu", SysMenu.class);
        String resource = "mapper/system/SysMenuMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
        }
        SysMenu menu = new SysMenu();
        for (String id : new String[]{"selectMenuTreeByUserId", "selectMenuListByUserId"}) {
            Object parameter = id.equals("selectMenuTreeByUserId") ? 42L : menu;
            String sql = config.getMappedStatement("com.ruoyi.system.mapper.SysMenuMapper." + id)
                    .getBoundSql(parameter).getSql().replaceAll("\\s+", " ");
            assertThat(sql).contains("ro.del_flag = '0'").contains("ro.status = '0'");
        }
    }
}
