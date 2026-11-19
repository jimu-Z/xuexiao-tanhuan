package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.mapper.TalkStudentMapper;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.service.impl.StudentPhotoService;
import com.ruoyi.talk.utils.StudentPhotoArchive.Photo;

class StudentPhotoImportTest {
    @Test void previewChecksAllIdentityFieldsAndExistingPhoto() {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        SysDeptMapper depts = mock(SysDeptMapper.class);
        ITalkStudentService access = mock(ITalkStudentService.class);
        StudentPhotoService.PhotoRepository photos = mock(StudentPhotoService.PhotoRepository.class);
        StudentPhotoService service = new StudentPhotoService(students, depts, access, photos);
        TalkStudent student = new TalkStudent(); student.setStudentId(5L); student.setStudentCode("100"); student.setStudentName("小明"); student.setDeptId(9L);
        SysDept dept = new SysDept(); dept.setDeptName("2023级1班");
        when(students.selectTalkStudentByCode("100")).thenReturn(student);
        when(access.selectTalkStudentByStudentId(5L)).thenReturn(student);
        when(depts.selectDeptById(9L)).thenReturn(dept);
        when(photos.exists(5L)).thenReturn(true);
        var good = service.preview(List.of(new Photo("2023级1班", "100", "小明", new byte[0])));
        assertThat(good.get(0).status()).isEqualTo("existing");
        var bad = service.preview(List.of(new Photo("其他班", "100", "小明", new byte[0])));
        assertThat(bad.get(0).status()).isEqualTo("error");
        verify(photos, never()).save(anyLong(), any(), anyBoolean());
    }
    @Test void previewDoesNotExposeStudentsOutsideScope() {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        ITalkStudentService access = mock(ITalkStudentService.class);
        var service = new StudentPhotoService(students, mock(SysDeptMapper.class), access, mock(StudentPhotoService.PhotoRepository.class));
        TalkStudent student = new TalkStudent(); student.setStudentId(5L);
        when(students.selectTalkStudentByCode("100")).thenReturn(student);
        when(access.selectTalkStudentByStudentId(5L)).thenThrow(new com.ruoyi.common.exception.ServiceException("denied"));
        assertThat(service.preview(List.of(new Photo("班级", "100", "姓名", new byte[0]))).get(0).status()).isEqualTo("error");
    }
    @Test void duplicateCodesAreRejectedBeforeLookupOrWrite() {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        var photos = mock(StudentPhotoService.PhotoRepository.class);
        var service = new StudentPhotoService(students, mock(SysDeptMapper.class), mock(ITalkStudentService.class), photos);
        var rows = service.execute(List.of(new Photo("班", "100", "小明", new byte[0]), new Photo("班", "100", "小明", new byte[0])), true);
        assertThat(rows).extracting(StudentPhotoService.Preview::status).containsExactly("error", "error");
        verifyNoInteractions(students, photos);
    }
}
