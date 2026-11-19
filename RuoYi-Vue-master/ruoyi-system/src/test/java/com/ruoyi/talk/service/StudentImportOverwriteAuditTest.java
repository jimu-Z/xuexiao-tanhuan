package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.mapper.TalkStudentMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkStudentServiceImpl;

class StudentImportOverwriteAuditTest {
    @Test void missingColumnsBecomeEmptyUpdateValuesWhileThreeStatusesAreRetained() throws Exception {
        var students = mock(TalkStudentMapper.class);
        var departments = mock(SysDeptMapper.class);
        var service = new TalkStudentServiceImpl();
        ReflectionTestUtils.setField(service, "talkStudentMapper", students);
        ReflectionTestUtils.setField(service, "sysDeptMapper", departments);
        ReflectionTestUtils.setField(service, "talkAlertService", mock(ITalkAlertService.class));
        StudentImportTestSupport.unrelatedRelations(service);
        var existing = new TalkStudent();
        existing.setStudentId(5L); existing.setStudentCode("QA001"); existing.setDeptId(203L);
        existing.setPhone("13800000000"); existing.setAddress("模拟地址");
        existing.setEnrollmentStatus("active"); existing.setPoliticalStatus("people");
        existing.setMentalHealthStatus("normal"); existing.setPovertyLevel("none");
        when(students.selectTalkStudentByCode("QA001")).thenReturn(existing);
        when(students.updateTalkStudent(any())).thenReturn(1);
        when(departments.selectDeptList(any(SysDept.class))).thenAnswer(call -> {
            SysDept q = call.getArgument(0); var d = new SysDept(); d.setDeptName(q.getDeptName()); d.setParentId(q.getParentId());
            if ("模拟学院".equals(q.getDeptName())) { d.setDeptId(201L); d.setDeptType("college"); }
            else if ("模拟年级".equals(q.getDeptName())) { d.setDeptId(202L); d.setDeptType("grade"); }
            else { d.setDeptId(203L); d.setDeptType("class"); }
            return List.of(d);
        });
        var cls = new SysDept(); cls.setDeptId(203L); cls.setDeptType("class");
        when(departments.selectDeptById(203L)).thenReturn(cls);
        var row = new HashMap<String,Object>(Map.of("student_code","QA001", "student_name","模拟学生",
                "college","模拟学院", "grade","模拟年级", "class","模拟班级"));
        try (var permission = mockStatic(TalkPermissionUtils.class); var security = mockStatic(SecurityUtils.class)) {
            permission.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            security.when(SecurityUtils::getUsername).thenReturn("audit");
            var result = service.importExecute(List.of(row), "update");
            assertThat(result.get("successCount")).isEqualTo(1);
        }
        var captured = ArgumentCaptor.forClass(TalkStudent.class);
        verify(students).updateTalkStudent(captured.capture());
        TalkStudent update = captured.getValue();
        assertThat(update.getPhone()).isEmpty();
        assertThat(update.getAddress()).isEmpty();
        assertThat(update.getEnrollmentStatus()).isEmpty();
        assertThat(update.getPoliticalStatus()).isEqualTo("people");
        assertThat(update.getMentalHealthStatus()).isEqualTo("normal");
        assertThat(update.getPovertyLevel()).isEqualTo("none");
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("TalkStudent", TalkStudent.class);
        try (var xml = getClass().getResourceAsStream("/mapper/talk/TalkStudentMapper.xml")) {
            new XMLMapperBuilder(xml, config, "TalkStudentMapper.xml", config.getSqlFragments()).parse();
        }
        var sql = config.getMappedStatement("com.ruoyi.talk.mapper.TalkStudentMapper.updateTalkStudent").getBoundSql(update);
        assertThat(sql.getParameterMappings()).extracting(p -> p.getProperty())
                .contains("phone", "address", "enrollmentStatus", "politicalStatus", "mentalHealthStatus", "povertyLevel");
    }
}
