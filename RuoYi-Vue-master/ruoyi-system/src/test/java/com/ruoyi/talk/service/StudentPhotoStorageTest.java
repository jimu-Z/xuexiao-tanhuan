package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import com.ruoyi.talk.mapper.StudentPhotoMapper;
import com.ruoyi.talk.service.impl.StudentPhotoStorage;

class StudentPhotoStorageTest {
    Path temp = Path.of("target", "photo-test-" + java.util.UUID.randomUUID()).toAbsolutePath();
    @Test void savesOutsidePublicProfileAndSkipsExistingWithoutReplacement() {
        StudentPhotoMapper mapper = mock(StudentPhotoMapper.class);
        var tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus status) {}
            protected void doRollback(DefaultTransactionStatus status) {}
        };
        ITalkStudentService access = mock(ITalkStudentService.class);
        var departments = mock(com.ruoyi.system.mapper.SysDeptMapper.class);
        var store = new StudentPhotoStorage(mapper, tx, temp.resolve("public").toString(), access, departments);
        when(mapper.lockStudent(7L)).thenReturn(7L);
        var student = new com.ruoyi.talk.domain.TalkStudent(); student.setStudentCode("100"); student.setStudentName("小明"); student.setDeptId(9L);
        var dept = new com.ruoyi.common.core.domain.entity.SysDept(); dept.setDeptName("班");
        when(access.selectTalkStudentByStudentId(7L)).thenReturn(student);
        when(departments.selectDeptById(9L)).thenReturn(dept);
        when(mapper.findKey(7L)).thenReturn("existing.jpg");
        assertThat(store.save(7L, new com.ruoyi.talk.utils.StudentPhotoArchive.Photo("班", "100", "小明", new byte[]{1}), false)).isFalse();
        verify(mapper, never()).upsert(any());
        assertThat(temp.resolve("public")).doesNotExist();
    }
    @Test void storageRechecksStudentScopeAfterTakingTheRowLock() {
        StudentPhotoMapper mapper = mock(StudentPhotoMapper.class);
        ITalkStudentService access = mock(ITalkStudentService.class);
        var tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus status) {}
            protected void doRollback(DefaultTransactionStatus status) {}
        };
        var store = new StudentPhotoStorage(mapper, tx, temp.resolve("public").toString(), access, mock(com.ruoyi.system.mapper.SysDeptMapper.class));
        when(mapper.lockStudent(7L)).thenReturn(7L);
        when(access.selectTalkStudentByStudentId(7L)).thenThrow(new com.ruoyi.common.exception.ServiceException("无权访问"));
        assertThatThrownBy(() -> store.save(7L, new com.ruoyi.talk.utils.StudentPhotoArchive.Photo("班", "100", "小明", new byte[]{1}), true)).isInstanceOf(RuntimeException.class);
        verify(mapper, never()).upsert(any());
    }
    @Test void commitFailureKeepsPhotoWhenTransactionOutcomeIsUnknown() throws Exception {
        StudentPhotoMapper mapper = mock(StudentPhotoMapper.class);
        ITalkStudentService access = mock(ITalkStudentService.class);
        var departments = mock(com.ruoyi.system.mapper.SysDeptMapper.class);
        var tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus status) { throw new org.springframework.transaction.TransactionSystemException("simulated unknown commit"); }
            protected void doRollback(DefaultTransactionStatus status) {}
        };
        var store = new StudentPhotoStorage(mapper, tx, temp.resolve("public").toString(), access, departments);
        when(mapper.lockStudent(7L)).thenReturn(7L);
        var student = new com.ruoyi.talk.domain.TalkStudent(); student.setStudentCode("100"); student.setStudentName("小明"); student.setDeptId(9L);
        var dept = new com.ruoyi.common.core.domain.entity.SysDept(); dept.setDeptName("班");
        when(access.selectTalkStudentByStudentId(7L)).thenReturn(student); when(departments.selectDeptById(9L)).thenReturn(dept);
        assertThatThrownBy(() -> store.save(7L, new com.ruoyi.talk.utils.StudentPhotoArchive.Photo("班", "100", "小明", new byte[]{1}), true)).isInstanceOf(RuntimeException.class);
        try (var files = java.nio.file.Files.list(temp.resolve("public-student-photos"))) { assertThat(files.count()).isEqualTo(1); }
    }
    @Test void confirmedRollbackRemovesTheNewUnreferencedFile() throws Exception {
        StudentPhotoMapper mapper = mock(StudentPhotoMapper.class); ITalkStudentService access = mock(ITalkStudentService.class);
        var departments = mock(com.ruoyi.system.mapper.SysDeptMapper.class);
        var tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus status) {}
            protected void doRollback(DefaultTransactionStatus status) {}
        };
        var store = new StudentPhotoStorage(mapper, tx, temp.resolve("public").toString(), access, departments);
        when(mapper.lockStudent(7L)).thenReturn(7L);
        var student = new com.ruoyi.talk.domain.TalkStudent(); student.setStudentCode("100"); student.setStudentName("小明"); student.setDeptId(9L);
        var dept = new com.ruoyi.common.core.domain.entity.SysDept(); dept.setDeptName("班");
        when(access.selectTalkStudentByStudentId(7L)).thenReturn(student); when(departments.selectDeptById(9L)).thenReturn(dept);
        when(mapper.upsert(any())).thenThrow(new RuntimeException("simulated write failure"));
        assertThatThrownBy(() -> store.save(7L, new com.ruoyi.talk.utils.StudentPhotoArchive.Photo("班", "100", "小明", new byte[]{1}), true)).isInstanceOf(RuntimeException.class);
        try (var files = java.nio.file.Files.list(temp.resolve("public-student-photos"))) { assertThat(files.count()).isZero(); }
    }
}
