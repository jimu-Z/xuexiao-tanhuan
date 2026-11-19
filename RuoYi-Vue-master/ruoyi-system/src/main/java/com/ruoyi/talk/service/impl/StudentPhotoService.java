package com.ruoyi.talk.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.mapper.TalkStudentMapper;
import com.ruoyi.talk.service.ITalkStudentService;
import com.ruoyi.talk.utils.StudentPhotoArchive;
import com.ruoyi.talk.utils.StudentPhotoArchive.Photo;

@Service
public class StudentPhotoService {
    public interface PhotoRepository {
        boolean exists(long studentId);
        boolean save(long studentId, Photo photo, boolean replace);
        byte[] read(long studentId);
    }
    public record Preview(String className, String studentCode, String studentName, String status, String message, Long studentId) {}
    private final TalkStudentMapper students;
    private final SysDeptMapper departments;
    private final ITalkStudentService access;
    private final PhotoRepository photos;

    public StudentPhotoService(TalkStudentMapper students, SysDeptMapper departments, ITalkStudentService access, PhotoRepository photos) {
        this.students = students; this.departments = departments; this.access = access; this.photos = photos;
    }
    public List<Preview> preview(List<Photo> entries) {
        Map<String, Integer> counts = new HashMap<>();
        for (Photo p : entries) counts.merge(p.studentCode(), 1, Integer::sum);
        List<Preview> result = new ArrayList<>();
        for (Photo p : entries) {
            String status = "error", message = "学生不存在、无权访问或班级姓名不匹配";
            Long id = null;
            if (p.error() != null && !p.error().isBlank()) message = p.error();
            else if (counts.get(p.studentCode()) > 1) message = "压缩包中学号重复";
            else {
                try {
                    TalkStudent s = students.selectTalkStudentByCode(p.studentCode());
                    if (s != null && access.selectTalkStudentByStudentId(s.getStudentId()) != null) {
                        SysDept dept = departments.selectDeptById(s.getDeptId());
                        if (dept != null && StudentPhotoArchive.matches(p, s.getStudentCode(), s.getStudentName(), dept.getDeptName())) {
                            id = s.getStudentId(); status = photos.exists(id) ? "existing" : "ready"; message = "";
                        }
                    }
                } catch (com.ruoyi.common.exception.ServiceException denied) { /* generic failure hides cross-college data */ }
            }
            result.add(new Preview(p.className(), p.studentCode(), p.studentName(), status, message, id));
        }
        return result;
    }
    public List<Preview> execute(List<Photo> entries, boolean replace) {
        List<Preview> checks = preview(entries);
        List<Preview> result = new ArrayList<>();
        for (int i = 0; i < checks.size(); i++) {
            Preview row = checks.get(i);
            if (row.studentId() == null) { result.add(row); continue; }
            if ("existing".equals(row.status()) && !replace) {
                result.add(new Preview(row.className(), row.studentCode(), row.studentName(), "skipped", "已有照片", row.studentId()));
                continue;
            }
            try {
                boolean saved = photos.save(row.studentId(), entries.get(i), replace);
                result.add(new Preview(row.className(), row.studentCode(), row.studentName(), saved ? "saved" : "skipped", saved ? "" : "已有照片", row.studentId()));
            } catch (RuntimeException ex) {
                result.add(new Preview(row.className(), row.studentCode(), row.studentName(), "error", "保存失败或结果未知，请重新预检查", row.studentId()));
            }
        }
        return result;
    }
    public byte[] read(long studentId) {
        access.selectTalkStudentByStudentId(studentId);
        return photos.read(studentId);
    }
}
