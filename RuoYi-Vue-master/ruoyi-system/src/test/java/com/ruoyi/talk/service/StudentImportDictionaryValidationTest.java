package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.talk.mapper.TalkStudentMapper;
import com.ruoyi.talk.security.TalkPermissionUtils;
import com.ruoyi.talk.service.impl.TalkStudentServiceImpl;

class StudentImportDictionaryValidationTest {
    private TalkStudentServiceImpl service(TalkStudentMapper students) {
        TalkStudentServiceImpl service = new TalkStudentServiceImpl();
        ReflectionTestUtils.setField(service, "talkStudentMapper", students);
        ReflectionTestUtils.setField(service, "sysDeptMapper", mock(SysDeptMapper.class));
        StudentImportTestSupport.unrelatedRelations(service);
        return service;
    }

    @ParameterizedTest
    @CsvSource({"性别,gender", "政治面貌,political_status", "学籍状态,enrollment_status",
            "心理健康状态,mental_health_status", "贫困等级认定,poverty_level"})
    @SuppressWarnings("unchecked")
    void previewMarksInvalidPastedValuesOnTheOffendingRow(String header, String field) throws Exception {
        TalkStudentServiceImpl service = service(mock(TalkStudentMapper.class));
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            String[] headers = {"学号", "学生姓名", "学院", "年级", "班级", header};
            String[] values = {"QA001", "测试学生", "测试学院", "2026级", "测试班", "粘贴的非法值"};
            Row h = sheet.createRow(0), r = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                h.createCell(i).setCellValue(headers[i]); r.createCell(i).setCellValue(values[i]);
            }
            // A bad row must not prevent previewing the following valid row.
            Row next = sheet.createRow(2);
            for (int i = 0; i < 5; i++) next.createCell(i).setCellValue(i == 0 ? "QA002" : values[i]);
            workbook.write(output); bytes = output.toByteArray();
        }
        try (MockedStatic<TalkPermissionUtils> permissions = mockStatic(TalkPermissionUtils.class)) {
            permissions.when(TalkPermissionUtils::isGlobalAdmin).thenReturn(true);
            Map<String, Object> preview = service.importPreview(new MockMultipartFile("file", "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes));
            List<Map<String, Object>> rows = (List<Map<String, Object>>) preview.get("previewRows");
            assertThat(preview.get("errorCount")).isEqualTo(1);
            assertThat(rows.get(0).get("status")).isEqualTo("error");
            assertThat(rows.get(0).get("message").toString()).contains(header, "粘贴的非法值");
            assertThat(rows.get(1).get("status")).isEqualTo("ok");
        }
    }

    @ParameterizedTest
    @CsvSource({"gender,9", "political_status,'people,other'", "enrollment_status,unknown_code",
            "mental_health_status,随便填", "poverty_level,none;general"})
    void executeRejectsInvalidValuesBeforeAnyStudentOrAccountWrite(String field, String value) {
        TalkStudentMapper students = mock(TalkStudentMapper.class);
        TalkStudentServiceImpl service = service(students);
        Map<String, Object> row = new HashMap<>(Map.of("student_code", "QA001", "student_name", "测试学生",
                "college", "测试学院", "grade", "2026级", "class", "测试班"));
        row.put(field, value);
        Map<String, Object> result = service.importExecute(List.of(row), "update");
        assertThat(result.get("errorCount")).isEqualTo(1);
        assertThat(result.get("errors").toString()).contains("选项");
        verifyNoInteractions(students);
    }
}
