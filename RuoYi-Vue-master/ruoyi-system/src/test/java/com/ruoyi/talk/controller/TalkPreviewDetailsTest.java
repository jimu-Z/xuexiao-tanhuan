package com.ruoyi.talk.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.domain.TalkStudentRecord;
import com.ruoyi.talk.service.ITalkStudentRecordService;
import com.ruoyi.talk.service.ITalkStudentService;

class TalkPreviewDetailsTest {
    @Test void previewLoadsStudentThroughScopeAndHistoryThroughScopedList() {
        var records = mock(ITalkStudentRecordService.class); var students = mock(ITalkStudentService.class);
        var controller = new TalkStudentRecordController();
        ReflectionTestUtils.setField(controller, "talkStudentRecordService", records);
        ReflectionTestUtils.setField(controller, "talkStudentService", students);
        ReflectionTestUtils.setField(controller, "previewDictMapper", mock(com.ruoyi.system.mapper.SysDictDataMapper.class));
        var student = new TalkStudent(); student.setStudentId(7L);
        when(students.selectTalkStudentByStudentId(7L)).thenReturn(student);
        when(records.selectTalkStudentRecordList(any())).thenReturn(List.of());
        assertThat(controller.previewStudent(7L).get("data")).isNotNull();
        verify(records).selectTalkStudentRecordList(argThat(q -> Long.valueOf(7).equals(q.getStudentId())));
    }
    @Test void previewTextMatchesDocxForHtmlAndRecordMarker() {
        var controller = new TalkStudentRecordController();
        var result = controller.previewText(java.util.Map.of("content", "<p>共享 &amp; 正文</p><p>第二段<br>第三行</p><p>【谈话记录】历史反馈</p>"));
        assertThat(result.get("data")).isEqualTo("共享 & 正文\n第二段\n第三行");
    }
}
