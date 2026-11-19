package com.ruoyi.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.service.impl.SysDeptServiceImpl;

class DepartmentDescendantMoveAuditTest {
    @Test void movingParentUnderChildWritesCyclicAncestorStrings() {
        var mapper = mock(SysDeptMapper.class); var service = new SysDeptServiceImpl();
        ReflectionTestUtils.setField(service, "deptMapper", mapper);
        var oldParent = new SysDept(); oldParent.setDeptId(10L); oldParent.setParentId(100L); oldParent.setAncestors("0,100");
        var child = new SysDept(); child.setDeptId(11L); child.setParentId(10L); child.setAncestors("0,100,10");
        when(mapper.selectDeptById(10L)).thenReturn(oldParent);
        when(mapper.selectDeptById(11L)).thenReturn(child);
        when(mapper.selectChildrenDeptById(10L)).thenReturn(List.of(child));
        when(mapper.updateDept(any())).thenReturn(1);
        var change = new SysDept(); change.setDeptId(10L); change.setParentId(11L);
        assertThat(service.updateDept(change)).isEqualTo(1);
        assertThat(change.getAncestors()).isEqualTo("0,100,10,11");
        assertThat(child.getAncestors()).isEqualTo("0,100,10,11,10");
        verify(mapper).updateDeptChildren(List.of(child));
        verify(mapper).updateDept(change);
    }
}
