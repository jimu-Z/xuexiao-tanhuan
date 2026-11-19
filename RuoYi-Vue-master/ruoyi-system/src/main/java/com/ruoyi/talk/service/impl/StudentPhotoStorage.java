package com.ruoyi.talk.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.talk.mapper.StudentPhotoMapper;
import com.ruoyi.talk.service.ITalkStudentService;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.utils.StudentPhotoArchive;

@Repository
public class StudentPhotoStorage implements StudentPhotoService.PhotoRepository {
    private final StudentPhotoMapper mapper;
    private final TransactionTemplate transaction;
    private final Path root;
    private final ITalkStudentService access;
    private final SysDeptMapper departments;
    public StudentPhotoStorage(StudentPhotoMapper mapper, PlatformTransactionManager manager, @Value("${ruoyi.profile}") String profile, ITalkStudentService access, SysDeptMapper departments) {
        this.mapper = mapper; this.transaction = new TransactionTemplate(manager);
        this.access = access; this.departments = departments;
        Path publicRoot = Path.of(profile).toAbsolutePath().normalize();
        if (publicRoot.getParent() == null) throw new IllegalArgumentException("上传目录不能是文件系统根目录");
        this.root = publicRoot.resolveSibling(publicRoot.getFileName() + "-student-photos");
    }
    public boolean exists(long studentId) { return mapper.findKey(studentId) != null; }
    public boolean save(long studentId, StudentPhotoArchive.Photo photo, boolean replace) {
        try {
            Boolean saved = transaction.execute(status -> {
                if (mapper.lockStudent(studentId) == null) throw new ServiceException("学生不存在");
                var student = access.selectTalkStudentByStudentId(studentId);
                var dept = student == null ? null : departments.selectDeptById(student.getDeptId());
                if (dept == null || !StudentPhotoArchive.matches(photo, student.getStudentCode(), student.getStudentName(), dept.getDeptName()))
                    throw new ServiceException("学生资料已变化，请重新预检查");
                if (mapper.findKey(studentId) != null && !replace) return false;
                try {
                    Files.createDirectories(root);
                    if (Files.isSymbolicLink(root)) throw new IOException("invalid root");
                    String key = UUID.randomUUID() + ".jpg";
                    Path path = root.resolve(key);
                    Files.createFile(path);
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCompletion(int completion) {
                            if (completion == STATUS_ROLLED_BACK) {
                                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                            }
                        }
                    });
                    Files.write(path, photo.bytes(), StandardOpenOption.WRITE);
                    mapper.upsert(new StudentPhotoMapper.PhotoRow(studentId, key));
                    return true;
                } catch (IOException ex) { throw new ServiceException("照片保存失败"); }
            });
            return Boolean.TRUE.equals(saved);
        } catch (RuntimeException ex) {
            // An exception during commit does not prove that the database rolled back.
            // Preserve the file unless the transaction manager confirms a rollback.
            throw ex;
        }
    }
    public byte[] read(long studentId) {
        String key = mapper.findKey(studentId);
        if (key == null) return null;
        if (!key.matches("[0-9a-f-]{36}\\.jpg")) throw new ServiceException("照片路径无效");
        Path file = root.resolve(key);
        try {
            if (Files.isSymbolicLink(root) || Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("missing photo");
            return Files.readAllBytes(file);
        } catch (IOException ex) { throw new ServiceException("照片文件不存在或无法读取"); }
    }
}
