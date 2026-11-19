package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.domain.TalkStudent;
import com.ruoyi.talk.mapper.TalkStudentMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkStudentServiceImpl;
import com.ruoyi.talk.service.impl.StudentImportRelationsService;

class TalkStudentImportValidationTest {
    @Test @SuppressWarnings("unchecked")
    void previewValidatesTeacherAgainstExactCollegeWhenNamesOverlap() throws Exception {
        SysDeptMapper departments = mock(SysDeptMapper.class);
        SysDept similar = new SysDept(); similar.setDeptId(201L); similar.setDeptType("college"); similar.setDeptName("信息学院分院"); similar.setParentId(100L);
        SysDept nested = new SysDept(); nested.setDeptId(203L); nested.setDeptType("college"); nested.setDeptName("信息学院"); nested.setParentId(999L);
        SysDept exact = new SysDept(); exact.setDeptId(202L); exact.setDeptType("college"); exact.setDeptName("信息学院"); exact.setParentId(100L);
        when(departments.selectDeptList(any(SysDept.class))).thenReturn(List.of(similar, nested, exact));
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class), departments);
        StudentImportRelationsService relations = mock(StudentImportRelationsService.class);
        when(relations.resolveTeachers(eq(201L), anyMap())).thenThrow(new com.ruoyi.common.exception.ServiceException("教师不在该学院"));
        ReflectionTestUtils.setField(service, "importRelations", relations);
        byte[] bytes;
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(); Row header = sheet.createRow(0); Row row = sheet.createRow(1);
            String[] labels = {"学号", "学生姓名", "学院", "年级", "班级", "辅导员"};
            String[] values = {"QA001", "模拟学生", "信息学院", "2026级", "测试班", "模拟老师"};
            for (int i = 0; i < labels.length; i++) { header.createCell(i).setCellValue(labels[i]); row.createCell(i).setCellValue(values[i]); }
            workbook.write(output); bytes = output.toByteArray();
        }
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            Map<String, Object> result = service.importPreview(new MockMultipartFile("file", "students.xlsx", null, bytes));
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("previewRows");
            assertThat(rows.get(0).get("status")).isEqualTo("ok");
            verify(relations).resolveTeachers(eq(202L), anyMap());
            when(relations.resolveTeachers(eq(202L), anyMap())).thenThrow(new com.ruoyi.common.exception.ServiceException("教师不在该学院"));
            result = service.importPreview(new MockMultipartFile("file", "students.xlsx", null, bytes));
            rows = (List<Map<String, Object>>) result.get("previewRows");
            assertThat(rows.get(0).get("status")).isEqualTo("error");
            assertThat(rows.get(0).get("message").toString()).contains("教师不在该学院");
        }
    }

    @Test @SuppressWarnings("unchecked")
    void collegeAdminPreviewUsesOwnCollegeWhenCollegeNamesAreIdentical() throws Exception {
        SysDeptMapper departments = mock(SysDeptMapper.class);
        SysDept other = new SysDept(); other.setDeptId(201L); other.setDeptType("college"); other.setDeptName("同名学院");
        SysDept own = new SysDept(); own.setDeptId(202L); own.setDeptType("college"); own.setDeptName("同名学院");
        when(departments.selectDeptList(any(SysDept.class))).thenReturn(List.of(other, own));
        when(departments.selectDeptById(202L)).thenReturn(own);
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class), departments);
        StudentImportRelationsService relations = mock(StudentImportRelationsService.class);
        when(relations.resolveTeachers(eq(201L), anyMap())).thenThrow(new com.ruoyi.common.exception.ServiceException("教师不在该学院"));
        ReflectionTestUtils.setField(service, "importRelations", relations);
        byte[] bytes;
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(); Row header = sheet.createRow(0); Row row = sheet.createRow(1);
            String[] labels = {"学号", "学生姓名", "学院", "年级", "班级", "辅导员"};
            String[] values = {"QA002", "模拟学生", "同名学院", "2026级", "测试班", "模拟老师"};
            for (int i = 0; i < labels.length; i++) { header.createCell(i).setCellValue(labels[i]); row.createCell(i).setCellValue(values[i]); }
            workbook.write(output); bytes = output.toByteArray();
        }
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::currentCollegeId).thenReturn(202L);
            permissions.when(TalkPermissionUtils::isCollegeAdmin).thenReturn(true);
            permissions.when(TalkPermissionUtils::isTeacher).thenReturn(true);
            Map<String, Object> result = service.importPreview(new MockMultipartFile("file", "students.xlsx", null, bytes));
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("previewRows");
            assertThat(rows.get(0).get("status")).isEqualTo("ok");
            verify(relations).resolveTeachers(eq(202L), anyMap());
        }
    }

    @Test
    void importRejectsSameNameGradeWithWrongDepartmentTypeBeforeWriting() {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        SysDeptMapper departments = mock(SysDeptMapper.class);
        TalkStudentServiceImpl service = service(students, departments);
        when(departments.selectDeptList(any(SysDept.class))).thenAnswer(call -> {
            SysDept query = call.getArgument(0);
            SysDept match = new SysDept(); match.setDeptName(query.getDeptName()); match.setParentId(query.getParentId());
            if ("测试学院".equals(query.getDeptName())) { match.setDeptId(201L); match.setDeptType("college"); return List.of(match); }
            if ("2026级".equals(query.getDeptName())) { match.setDeptId(202L); match.setDeptType("class"); return List.of(match); }
            return List.of();
        });
        Map<String, Object> row = new HashMap<>(Map.of("student_code", "QA003", "student_name", "模拟学生",
                "college", "测试学院", "grade", "2026级", "class", "测试班"));
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class);
             MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            security.when(SecurityUtils::getUsername).thenReturn("qa-importer");
            Map<String, Object> result = service.importExecute(List.of(row), "skip");
            assertThat(result.get("errorCount")).isEqualTo(1);
            assertThat(result.get("errors").toString()).contains("组织类型");
            verify(departments, never()).insertDept(any(SysDept.class));
            verify(students, never()).insertTalkStudent(any(TalkStudent.class));
        }
    }

    @Test @SuppressWarnings("unchecked")
    void teacherPreviewReportsWrongGradeTypeOnTheAffectedRow() throws Exception {
        SysDeptMapper departments = mock(SysDeptMapper.class);
        SysDept college = new SysDept(); college.setDeptId(201L); college.setDeptName("测试学院"); college.setDeptType("college");
        SysDept wrongGrade = new SysDept(); wrongGrade.setDeptId(202L); wrongGrade.setParentId(201L); wrongGrade.setDeptName("2026级"); wrongGrade.setDeptType("class");
        when(departments.selectDeptById(201L)).thenReturn(college);
        when(departments.selectDeptList(any(SysDept.class))).thenReturn(List.of(wrongGrade));
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class), departments);
        byte[] bytes;
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(); Row header = sheet.createRow(0); Row row = sheet.createRow(1);
            String[] labels = {"学号", "学生姓名", "学院", "年级", "班级"};
            String[] values = {"QA004", "模拟学生", "测试学院", "2026级", "测试班"};
            for (int i = 0; i < labels.length; i++) { header.createCell(i).setCellValue(labels[i]); row.createCell(i).setCellValue(values[i]); }
            workbook.write(output); bytes = output.toByteArray();
        }
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::currentCollegeId).thenReturn(201L);
            permissions.when(TalkPermissionUtils::isTeacher).thenReturn(true);
            Map<String, Object> result = service.importPreview(new MockMultipartFile("file", "students.xlsx", null, bytes));
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("previewRows");
            assertThat(rows.get(0).get("status")).isEqualTo("error");
            assertThat(rows.get(0).get("message").toString()).contains("组织类型");
        }
    }

    @Test @SuppressWarnings("unchecked")
    void globalAdminPreviewReportsWrongGradeTypeOnTheAffectedRow() throws Exception {
        SysDeptMapper departments = mock(SysDeptMapper.class);
        when(departments.selectDeptList(any(SysDept.class))).thenAnswer(call -> {
            SysDept query = call.getArgument(0); SysDept match = new SysDept();
            match.setDeptName(query.getDeptName()); match.setParentId(query.getParentId());
            if ("测试学院".equals(query.getDeptName())) { match.setDeptId(201L); match.setDeptType("college"); return List.of(match); }
            if ("2026级".equals(query.getDeptName())) { match.setDeptId(202L); match.setDeptType("class"); return List.of(match); }
            return List.of();
        });
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class), departments);
        byte[] bytes;
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(); Row header = sheet.createRow(0); Row row = sheet.createRow(1);
            String[] labels = {"学号", "学生姓名", "学院", "年级", "班级"};
            String[] values = {"QA005", "模拟学生", "测试学院", "2026级", "测试班"};
            for (int i = 0; i < labels.length; i++) { header.createCell(i).setCellValue(labels[i]); row.createCell(i).setCellValue(values[i]); }
            workbook.write(output); bytes = output.toByteArray();
        }
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            permissions.when(TalkPermissionUtils::isTeacher).thenReturn(true); // 多角色不降低全局管理员权限
            Map<String, Object> result = service.importPreview(new MockMultipartFile("file", "students.xlsx", null, bytes));
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("previewRows");
            assertThat(rows.get(0).get("status")).isEqualTo("error");
            assertThat(rows.get(0).get("message").toString()).contains("组织类型");
            when(departments.selectDeptList(any(SysDept.class))).thenAnswer(call -> {
                SysDept query = call.getArgument(0);
                if (!"测试学院".equals(query.getDeptName())) return List.of();
                SysDept college = new SysDept(); college.setDeptId(201L); college.setDeptName("测试学院");
                college.setDeptType("college"); college.setParentId(100L); return List.of(college);
            });
            result = service.importPreview(new MockMultipartFile("file", "students.xlsx", null, bytes));
            rows = (List<Map<String, Object>>) result.get("previewRows");
            assertThat(rows.get(0).get("status")).isEqualTo("ok");
        }
    }
    @Test
    void previewExplainsInvalidAndHeaderOnlyFiles() throws Exception {
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class), mock(SysDeptMapper.class));
        assertThatThrownBy(() -> service.importPreview(new MockMultipartFile("file", "bad.xlsx", null, "invalid".getBytes())))
            .isInstanceOf(com.ruoyi.common.exception.ServiceException.class)
            .hasMessageContaining("有效的 Excel");
        byte[] bytes;
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet().createRow(0).createCell(0).setCellValue("学号");
            workbook.write(output);
            bytes = output.toByteArray();
        }
        assertThatThrownBy(() -> service.importPreview(new MockMultipartFile("file", "header.xlsx", null, bytes)))
            .isInstanceOf(com.ruoyi.common.exception.ServiceException.class)
            .hasMessageContaining("表头行和一行数据");
    }
    private TalkStudentServiceImpl service(TalkStudentMapper students, SysDeptMapper departments) {
        TalkStudentServiceImpl service = new TalkStudentServiceImpl();
        ReflectionTestUtils.setField(service, "talkStudentMapper", students);
        ReflectionTestUtils.setField(service, "sysDeptMapper", departments);
        StudentImportTestSupport.unrelatedRelations(service);
        return service;
    }

    @Test @SuppressWarnings("unchecked")
    void previewRejectsMissingRequiredStudentName() throws Exception {
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class), mock(SysDeptMapper.class));
        byte[] bytes;
        try (InputStream input = Files.newInputStream(Path.of("..", "ruoyi-admin", "src", "main", "resources", "template", "student_import_template.xlsx"));
             Workbook workbook = WorkbookFactory.create(input); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.createRow(2);
            Map<String,String> values = Map.of("学号","QA001", "学院","测试学院", "年级","2026级", "班级","测试班");
            Row headers = sheet.getRow(1);
            for (int i=0; i<headers.getLastCellNum(); i++) {
                String value = values.get(headers.getCell(i).getStringCellValue());
                if (value != null) row.createCell(i).setCellValue(value);
            }
            workbook.write(output); bytes = output.toByteArray();
        }
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            Map<String,Object> result = service.importPreview(new MockMultipartFile("file","students.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes));
            List<Map<String,Object>> rows = (List<Map<String,Object>>) result.get("previewRows");
            assertThat(result.get("errorCount")).isEqualTo(1);
            assertThat(rows.get(0).get("status")).isEqualTo("error");
            assertThat(rows.get(0).get("message").toString()).contains("姓名");
        }
    }

    @Test @SuppressWarnings("unchecked")
    void existingStudentPreviewExplainsBothDuplicateModes() throws Exception {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        when(students.selectTalkStudentList(any(TalkStudent.class))).thenReturn(List.of(new TalkStudent()));
        TalkStudentServiceImpl service = service(students, mock(SysDeptMapper.class));
        byte[] bytes;
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(); Row headers=sheet.createRow(0); Row data=sheet.createRow(1);
            String[] labels={"学号","学生姓名","学院","年级","班级"};
            String[] values={"QA001","测试姓名","测试学院","2026级","测试班"};
            for(int i=0;i<labels.length;i++){headers.createCell(i).setCellValue(labels[i]);data.createCell(i).setCellValue(values[i]);}
            workbook.write(output); bytes=output.toByteArray();
        }
        try(MockedStatic<TalkPermissionUtils> permissions=mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            Map<String,Object> result=service.importPreview(new MockMultipartFile("file","students.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes));
            List<Map<String,Object>> rows=(List<Map<String,Object>>)result.get("previewRows");
            assertThat(rows.get(0).get("message").toString()).contains("跳过","覆盖","更新");
        }
    }
    @Test
    void updateModeSkipsLaterDuplicateAndPreservesFirstOccurrence() {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        SysDeptMapper departments = mock(SysDeptMapper.class);
        TalkStudentServiceImpl service = service(students, departments);
        TalkStudent existing = new TalkStudent(); existing.setStudentId(5L); existing.setStudentCode("QA001"); existing.setDeptId(203L);
        when(students.selectTalkStudentList(any(TalkStudent.class))).thenReturn(List.of(existing));
        when(students.selectTalkStudentByCode("QA001")).thenReturn(existing);
        SysDept clazz=new SysDept();clazz.setDeptId(203L);clazz.setDeptType("class");
        when(departments.selectDeptById(203L)).thenReturn(clazz);
        when(departments.selectDeptList(any(SysDept.class))).thenAnswer(call -> {
            SysDept query = call.getArgument(0);
            SysDept match = new SysDept(); match.setDeptName(query.getDeptName()); match.setParentId(query.getParentId());
            match.setDeptType("测试学院".equals(query.getDeptName()) ? "college" : "2026级".equals(query.getDeptName()) ? "grade" : "class");
            match.setDeptId("测试学院".equals(query.getDeptName()) ? 201L : "2026级".equals(query.getDeptName()) ? 202L : 203L);
            return List.of(match);
        });
        List<String> savedNames = new ArrayList<>();
        when(students.updateTalkStudent(any(TalkStudent.class))).thenAnswer(call -> { savedNames.add(((TalkStudent)call.getArgument(0)).getStudentName()); return 1; });
        Map<String,Object> first = new HashMap<>(Map.of("student_code","QA001","student_name","第一条","college","测试学院","grade","2026级","class","测试班"));
        Map<String,Object> duplicate = new HashMap<>(first); duplicate.put("student_name","重复行不应覆盖");
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class);
             MockedStatic<SecurityUtils> security=mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUsername).thenReturn("qa-importer");
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            Map<String,Object> result = service.importExecute(List.of(first,duplicate),"update");
            assertThat(result.get("successCount")).isEqualTo(1);
            assertThat(result.get("skipCount")).isEqualTo(1);
            assertThat(savedNames).containsExactly("第一条");
            assertThat(result.get("errors").toString()).contains("重复");
        }
    }
}
