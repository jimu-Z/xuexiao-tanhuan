package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.talk.domain.TalkTeacherClass;
import com.ruoyi.talk.mapper.TalkTeacherClassMapper;
import com.ruoyi.talk.service.impl.TalkTeacherClassServiceImpl;

class TeacherClassReplacementAuditTest {
    @Test void savingNamesDeletesPreciseIdBindingsAndInsertsOnlyNames() {
        var mapper = mock(TalkTeacherClassMapper.class);
        var service = new TalkTeacherClassServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        service.saveTeacherClasses("QA-TEACHER", List.of("模拟同名班"));
        var order = inOrder(mapper);
        order.verify(mapper).deleteIdBindingsByTeacherCode("QA-TEACHER");
        order.verify(mapper).deleteByTeacherCode("QA-TEACHER");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TalkTeacherClass>> rows = ArgumentCaptor.forClass(List.class);
        order.verify(mapper).batchInsert(rows.capture());
        assertThat(rows.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getTeacherCode()).isEqualTo("QA-TEACHER");
            assertThat(row.getClassName()).isEqualTo("模拟同名班");
        });
    }
}
