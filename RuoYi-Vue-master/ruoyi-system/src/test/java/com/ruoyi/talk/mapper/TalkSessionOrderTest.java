package com.ruoyi.talk.mapper;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.talk.domain.TalkSession;
class TalkSessionOrderTest {
    @Test void sessionPagesUseStableNewestFirstOrderWithFiltersAndCollegeScope() throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("TalkSession", TalkSession.class);
        try (InputStream xml = getClass().getResourceAsStream("/mapper/talk/TalkSessionMapper.xml")) {
            new XMLMapperBuilder(xml, config, "TalkSessionMapper.xml", config.getSqlFragments()).parse();
        }
        TalkSession query = new TalkSession();
        query.setStudentKeyword("QA"); query.setTagValue("qa_tag");
        query.getParams().put("collegeDeptId", 910001L);
        var bound = config.getMappedStatement("com.ruoyi.talk.mapper.TalkSessionMapper.selectTalkSessionList").getBoundSql(query);
        assertThat(bound.getSql().toLowerCase().replaceAll("\\s+", " "))
                .contains("order by ts.talk_time desc, ts.session_id desc");
        assertThat(bound.getParameterMappings()).extracting(m -> m.getProperty())
                .contains("studentKeyword", "tagValue", "params.collegeDeptId");
    }
}
