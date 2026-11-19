package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.*;
import com.ruoyi.talk.mapper.*;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkSessionServiceImpl;

class SessionTagValidationAuditTest {
    @Test void createPassesUnknownTagValueToSessionTagMapper() {
        var service = new TalkSessionServiceImpl();
        var students = mock(TalkStudentMapper.class); var departments = mock(SysDeptMapper.class);
        var sessions = mock(TalkSessionMapper.class); var tags = mock(TalkSessionTagMapper.class);
        ReflectionTestUtils.setField(service, "talkStudentMapper", students);
        ReflectionTestUtils.setField(service, "sysDeptMapper", departments);
        ReflectionTestUtils.setField(service, "talkSessionMapper", sessions);
        ReflectionTestUtils.setField(service, "talkSessionTagMapper", tags);
        ReflectionTestUtils.setField(service, "talkStudentRecordMapper", mock(TalkStudentRecordMapper.class));
        var student = new TalkStudent(); student.setStudentId(7L); student.setDeptId(100L);
        var college = new SysDept(); college.setDeptId(100L); college.setDeptType("college");
        when(students.selectTalkStudentByStudentId(7L)).thenReturn(student);
        when(departments.selectDeptById(100L)).thenReturn(college);
        doAnswer(call -> { ((TalkSession)call.getArgument(0)).setSessionId(9L); return 1; }).when(sessions).insertTalkSession(any());
        var request = new TalkSessionCreateRequest(); request.setTalkType("individual");
        request.setTalkTime("2026-01-01"); request.setTalkPerson("模拟教师");
        request.setStudentIds(List.of(7L)); request.setTags(List.of("unknown-simulated-key"));
        try (var permission = mockStatic(TalkPermissionUtils.class); var security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUsername).thenReturn("simulated");
            service.createTalkWithRecords(request);
            permission.verify(() -> TalkPermissionUtils.requireCollegeAccess(100L));
        }
        var saved = ArgumentCaptor.forClass(TalkSessionTag.class);
        verify(tags).insertTalkSessionTag(saved.capture());
        assertThat(saved.getValue().getTagValue()).isEqualTo("unknown-simulated-key");
    }
}
