package com.ruoyi.talk.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.talk.domain.TalkStudentRecord;

class TalkRecordClassQueryTest {
    @Test void classFilterKeepsSessionCollegeScopeAndIncludesDescendants() throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("TalkStudentRecord", TalkStudentRecord.class);
        try (InputStream xml = getClass().getResourceAsStream("/mapper/talk/TalkStudentRecordMapper.xml")) {
            new XMLMapperBuilder(xml, config, "TalkStudentRecordMapper.xml", config.getSqlFragments()).parse();
        }
        TalkStudentRecord query = new TalkStudentRecord();
        query.setDeptId(123L);
        query.getParams().put("collegeDeptId", 456L);
        BoundSql bound = config.getMappedStatement("com.ruoyi.talk.mapper.TalkStudentRecordMapper.selectTalkStudentRecordList").getBoundSql(query);
        assertThat(bound.getSql().toLowerCase()).contains("st.dept_id", "find_in_set", "ts.college_dept_id");
        assertThat(bound.getParameterMappings()).extracting(m -> m.getProperty()).contains("deptId", "params.collegeDeptId");
    }
}
