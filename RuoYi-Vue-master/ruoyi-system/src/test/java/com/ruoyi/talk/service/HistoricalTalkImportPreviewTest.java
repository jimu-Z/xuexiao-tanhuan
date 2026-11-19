package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.domain.TalkTeacher;
import com.ruoyi.talk.mapper.*;
import com.ruoyi.talk.security.TalkPermissionUtils;

class HistoricalTalkImportPreviewTest {
    @Test void rejectsMismatchedTeacherNameWithoutCreatingSession() {
        var service = new HistoricalTalkImportService();
        var students = mock(TalkStudentMapper.class); var teachers = mock(TalkTeacherMapper.class);
        var depts = mock(SysDeptMapper.class); var participants = mock(TalkSessionParticipantMapper.class);
        var tags = mock(TalkTagMapper.class); var duplicates = mock(HistoricalTalkImportMapper.class);
        ReflectionTestUtils.setField(service, "students", students); ReflectionTestUtils.setField(service, "teachers", teachers);
        ReflectionTestUtils.setField(service, "depts", depts); ReflectionTestUtils.setField(service, "participants", participants);
        ReflectionTestUtils.setField(service, "tags", tags); ReflectionTestUtils.setField(service, "duplicates", duplicates);
        var student = new TalkStudent(); student.setStudentId(1L); student.setDeptId(3L);
        when(students.selectTalkStudentByCode("001")).thenReturn(student);
        var dept = new SysDept(); dept.setDeptId(3L); dept.setDeptType("college"); when(depts.selectDeptById(3L)).thenReturn(dept);
        var teacher = new TalkTeacher(); teacher.setUserId(8L); teacher.setDeptId(3L); teacher.setTeacherName("王老师");
        when(teachers.selectByTeacherCode("T1")).thenReturn(teacher);
        try (var permissions = mockStatic(TalkPermissionUtils.class)) {
            var input = new HistoricalTalkImportParser.Row(2, "个别谈话", "2026-01-02", List.of("001"), List.of("T1"), List.of("李老师"), "", "", "内容", List.of());
            var result = service.inspect(input);
            assertThat(result.error()).contains("姓名");
        }
    }
    @Test void duplicateAccountsThroughDifferentTeacherCodesAreRejectedDuringPreview() {
        var service = new HistoricalTalkImportService();
        var students = mock(TalkStudentMapper.class); var teachers = mock(TalkTeacherMapper.class);
        var depts = mock(SysDeptMapper.class); var participants = mock(TalkSessionParticipantMapper.class);
        ReflectionTestUtils.setField(service, "students", students); ReflectionTestUtils.setField(service, "teachers", teachers);
        ReflectionTestUtils.setField(service, "depts", depts); ReflectionTestUtils.setField(service, "participants", participants);
        ReflectionTestUtils.setField(service, "tags", mock(TalkTagMapper.class)); ReflectionTestUtils.setField(service, "duplicates", mock(HistoricalTalkImportMapper.class));
        var student = new TalkStudent(); student.setStudentId(1L); student.setDeptId(3L); when(students.selectTalkStudentByCode("001")).thenReturn(student);
        var dept = new SysDept(); dept.setDeptId(3L); dept.setDeptType("college"); when(depts.selectDeptById(3L)).thenReturn(dept);
        var teacher = new TalkTeacher(); teacher.setUserId(8L); teacher.setDeptId(3L); teacher.setTeacherName("王老师");
        when(teachers.selectByTeacherCode("T1")).thenReturn(teacher); when(teachers.selectByTeacherCode("T2")).thenReturn(teacher);
        when(participants.selectActiveTeacherName(3L, 8L)).thenReturn("王老师");
        try (var permissions = mockStatic(TalkPermissionUtils.class)) {
            var input = new HistoricalTalkImportParser.Row(2, "个别谈话", "2026-01-02", List.of("001"), List.of("T1", "T2"), List.of("王老师", "王老师"), "", "", "", List.of());
            assertThat(service.inspect(input).error()).contains("重复");
        }
    }

}
