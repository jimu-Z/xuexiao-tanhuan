package com.ruoyi.talk.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.*;
import com.ruoyi.talk.mapper.*;
import com.ruoyi.talk.security.TalkPermissionUtils;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HistoricalTalkImportService {
    @Autowired private TalkStudentMapper students;
    @Autowired private TalkTeacherMapper teachers;
    @Autowired private SysDeptMapper depts;
    @Autowired private TalkSessionParticipantMapper participants;
    @Autowired private TalkTagMapper tags;
    @Autowired private HistoricalTalkImportMapper duplicates;

    public record Preview(int rowNumber, String error, boolean duplicate, TalkSessionCreateRequest request) {}

    public static Set<Integer> resolvedFileDuplicateRows(List<Preview> rows) {
        Map<String, Set<Integer>> occurrences = new HashMap<>();
        for (Preview row : rows) {
            if (row.request() == null || !row.error().isEmpty()) continue;
            for (Long studentId : row.request().getStudentIds()) {
                String key = row.request().getTalkTime() + "\u0000" + studentId;
                occurrences.computeIfAbsent(key, ignored -> new HashSet<>()).add(row.rowNumber());
            }
        }
        Set<Integer> result = new HashSet<>();
        occurrences.values().stream().filter(group -> group.size() > 1).forEach(result::addAll);
        return result;
    }

    public Preview inspect(HistoricalTalkImportParser.Row row) {
        if (row.parseError() != null && !row.parseError().isEmpty()) return new Preview(row.rowNumber(), row.parseError(), false, null);
        try { return validate(row); }
        catch (ServiceException e) { return new Preview(row.rowNumber(), e.getMessage(), false, null); }
        catch (java.time.DateTimeException e) { return new Preview(row.rowNumber(), "谈话日期无效，请使用YYYY-MM-DD", false, null); }
    }
    private Preview validate(HistoricalTalkImportParser.Row row) {
        String type = switch (row.talkType()) {
            case "个别谈话" -> "individual";
            case "集体谈话" -> "group";
            default -> throw new ServiceException("谈话类型只能填写个别谈话或集体谈话");
        };
        LocalDate date = LocalDate.parse(row.talkDate());
        if (row.studentCodes().isEmpty() || new HashSet<>(row.studentCodes()).size() != row.studentCodes().size())
            throw new ServiceException("学生学号不能为空或重复");
        if (type.equals("individual") && row.studentCodes().size() != 1) throw new ServiceException("个别谈话只能填写一名学生");
        List<Long> studentIds = new ArrayList<>(); Long college = null;
        for (String code : row.studentCodes()) {
            TalkStudent student = students.selectTalkStudentByCode(code);
            if (student == null) throw new ServiceException("学生不存在或无权访问");
            Long studentCollege = collegeFor(student.getDeptId());
            TalkPermissionUtils.requireCollegeAccess(studentCollege);
            if (college != null && !college.equals(studentCollege)) throw new ServiceException("集体谈话学生必须属于同一学院");
            if (studentIds.contains(student.getStudentId())) throw new ServiceException("参与学生不能重复");
            college = studentCollege; studentIds.add(student.getStudentId());
        }
        if (row.teacherCodes().size() != row.teacherNames().size()) throw new ServiceException("系统谈话人工号与姓名数量不一致");
        if (new HashSet<>(row.teacherCodes()).size() != row.teacherCodes().size()) throw new ServiceException("系统谈话人工号重复");
        List<TalkSessionParticipant> speakers = new ArrayList<>();
        Set<Long> speakerUsers = new HashSet<>();
        for (int i = 0; i < row.teacherCodes().size(); i++) {
            TalkTeacher teacher = teachers.selectByTeacherCode(row.teacherCodes().get(i));
            if (teacher == null || teacher.getUserId() == null || !Objects.equals(college, teacher.getDeptId()))
                throw new ServiceException("谈话人工号不存在、未关联账号或不属于学生学院");
            if (!Objects.equals(teacher.getTeacherName(), row.teacherNames().get(i))) throw new ServiceException("谈话人工号与姓名不匹配");
            if (!speakerUsers.add(teacher.getUserId())) throw new ServiceException("多个工号关联了重复的系统谈话人账号");
            if (participants.selectActiveTeacherName(college, teacher.getUserId()) == null) throw new ServiceException("系统谈话人账号未启用或无效");
            TalkSessionParticipant p = new TalkSessionParticipant(); p.setUserId(teacher.getUserId()); speakers.add(p);
        }
        if (!row.otherSpeakers().isBlank()) {
            Set<String> seen = new HashSet<>();
            for (String raw : row.otherSpeakers().split("[;；]")) {
                String text = raw.trim(); if (text.isEmpty()) continue;
                var matcher = Pattern.compile("^([^（）()]+?)(?:[（(]([^（）()]*)[）)])?$").matcher(text);
                if (!matcher.matches()) throw new ServiceException("其他谈话人格式无效");
                String name = matcher.group(1).trim(); String identity = matcher.group(2) == null ? "" : matcher.group(2).trim();
                if (name.isEmpty() || name.length() > 100 || identity.length() > 100 || !seen.add(name + "\0" + identity))
                    throw new ServiceException("其他谈话人无效或重复");
                TalkSessionParticipant p = new TalkSessionParticipant(); p.setDisplayName(name); p.setCustomIdentity(identity); speakers.add(p);
            }
        }
        if (speakers.isEmpty()) throw new ServiceException("至少填写一名谈话人");
        TalkTag query = new TalkTag(); query.getParams().put("collegeDeptId", college);
        List<TalkTag> available = tags.selectActiveTags(query); List<String> tagKeys = new ArrayList<>();
        for (String name : row.tags()) {
            List<TalkTag> found = available.stream().filter(t -> name.equals(t.getTagName())).toList();
            if (found.size() != 1) throw new ServiceException("标签不存在或名称不唯一：" + name);
            if (tagKeys.contains(found.get(0).getTagKey())) throw new ServiceException("标签重复：" + name);
            tagKeys.add(found.get(0).getTagKey());
        }
        if (row.location().length() > 100) throw new ServiceException("谈话地点不能超过100字");
        TalkSessionCreateRequest request = new TalkSessionCreateRequest(); request.setTalkType(type); request.setTalkTime(date.toString());
        request.setStudentIds(studentIds); request.setParticipants(speakers); request.setTalkLocation(row.location());
        request.setTalkContent(row.content()); request.setTags(tagKeys);
        boolean duplicate = studentIds.stream().anyMatch(id -> duplicates.countSameStudentDay(id, date.toString()) > 0);
        return new Preview(row.rowNumber(), "", duplicate, request);
    }
    private Long collegeFor(Long id) {
        Set<Long> seen = new HashSet<>();
        while (id != null && id > 0 && seen.add(id) && seen.size() <= 32) {
            SysDept dept = depts.selectDeptById(id); if (dept == null) break;
            if ("college".equals(dept.getDeptType())) return dept.getDeptId();
            id = dept.getParentId();
        }
        throw new ServiceException("学生未归属有效学院");
    }
}
