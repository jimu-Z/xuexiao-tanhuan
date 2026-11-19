package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.mapper.StudentPhotoMapper;
import com.ruoyi.talk.service.impl.StudentPhotoStorage;
import com.ruoyi.talk.utils.StudentPhotoArchive.Photo;

class StudentPhotoReplacementAuditTest {
    @TempDir Path temp;

    @Test void replacementLeavesPreviousFileInPrivateDirectory() throws Exception {
        var mapper = mock(StudentPhotoMapper.class);
        var access = mock(ITalkStudentService.class);
        var departments = mock(SysDeptMapper.class);
        var tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition d) { }
            protected void doCommit(DefaultTransactionStatus status) { }
            protected void doRollback(DefaultTransactionStatus status) { }
        };
        var currentKey = new AtomicReference<String>();
        when(mapper.lockStudent(7L)).thenReturn(7L);
        when(mapper.findKey(7L)).thenAnswer(inv -> currentKey.get());
        when(mapper.upsert(any())).thenAnswer(inv -> {
            StudentPhotoMapper.PhotoRow row = inv.getArgument(0);
            currentKey.set(row.getFileKey());
            return 1;
        });
        var student = new TalkStudent();
        student.setStudentCode("100"); student.setStudentName("模拟学生"); student.setDeptId(9L);
        var dept = new SysDept(); dept.setDeptName("模拟班级");
        when(access.selectTalkStudentByStudentId(7L)).thenReturn(student);
        when(departments.selectDeptById(9L)).thenReturn(dept);
        var store = new StudentPhotoStorage(mapper, tx, temp.resolve("public").toString(), access, departments);
        assertThat(store.save(7L, new Photo("模拟班级", "100", "模拟学生", new byte[]{1}), false)).isTrue();
        String previousKey = currentKey.get();
        assertThat(store.save(7L, new Photo("模拟班级", "100", "模拟学生", new byte[]{2}), true)).isTrue();
        assertThat(currentKey.get()).isNotEqualTo(previousKey);
        Path privateRoot = temp.resolve("public-student-photos");
        assertThat(Files.readAllBytes(privateRoot.resolve(previousKey))).containsExactly(1);
        assertThat(Files.readAllBytes(privateRoot.resolve(currentKey.get()))).containsExactly(2);
        assertThat(store.read(7L)).containsExactly(2);
    }
}
