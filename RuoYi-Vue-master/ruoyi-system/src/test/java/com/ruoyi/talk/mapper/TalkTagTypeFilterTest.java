package com.ruoyi.talk.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.talk.domain.TalkTag;

class TalkTagTypeFilterTest {
    @Test void typeFilterCombinesWithCollegeScopeAndName() throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("TalkTag", TalkTag.class);
        try (InputStream xml = getClass().getResourceAsStream("/mapper/talk/TalkTagMapper.xml")) {
            new XMLMapperBuilder(xml, config, "TalkTagMapper.xml", config.getSqlFragments()).parse();
        }
        TalkTag query = new TalkTag();
        query.setScopeType("college");
        query.setTagName("模拟");
        query.getParams().put("collegeDeptId", 910001L);
        var statement = config.getMappedStatement("com.ruoyi.talk.mapper.TalkTagMapper.selectTalkTagList");
        var sql = statement.getBoundSql(query);
        assertThat(sql.getSql().replaceAll("\\s+", " ")).contains("scope_type = ?", "tag_name like", "college_dept_id = ?");
        assertThat(sql.getParameterMappings()).extracting(m -> m.getProperty()).contains("scopeType", "tagName", "params.collegeDeptId");
        query.setScopeType(null);
        assertThat(statement.getBoundSql(query).getParameterMappings()).extracting(m -> m.getProperty()).doesNotContain("scopeType");
    }
}
