package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.talk.controller.TalkStudentController;
import com.ruoyi.talk.service.impl.StudentImportOptions;

class StudentImportOptionsTest {
    private StudentImportOptions options(SysDictDataMapper mapper) {
        StudentImportOptions options = new StudentImportOptions();
        ReflectionTestUtils.setField(options, "dictDataMapper", mapper);
        return options;
    }

    @Test
    void enlistedChineseLabelAndCodeUseTheCurrentDictionaryAndAppearInTemplate() throws Exception {
        SysDictDataMapper mapper = mock(SysDictDataMapper.class);
        when(mapper.selectDictDataByType(anyString())).thenReturn(StudentImportTestSupport.entries("test", "测试选项"));
        when(mapper.selectDictDataByType("enrollment_status")).thenReturn(StudentImportTestSupport.entries("enlisted", "入伍"));
        StudentImportOptions options = options(mapper);
        var snapshot = options.load();
        for (String value : List.of("入伍", "enlisted")) {
            Map<String, String> row = new HashMap<>(Map.of("enrollment_status", value));
            options.normalize(row, snapshot);
            assertThat(row.get("enrollment_status")).isEqualTo("enlisted");
        }
        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(Path.of("..", "ruoyi-admin", "src", "main", "resources", "template", "student_import_template.xlsx")))) {
            options.configureTemplate(workbook, snapshot);
            assertThat(workbook.getSheet("导入选项").getRow(0).getCell(2).getStringCellValue()).isEqualTo("入伍");
        }
    }

    @Test
    void downloadUsesLiveChineseLabelsAndKeepsOriginalFormAndIdentifiers() throws Exception {
        TalkStudentController controller = new TalkStudentController();
        SysDictDataMapper mapper = StudentImportTestSupport.dictionary(controller);
        // A long, comma-containing dictionary label must work without Excel's inline-list limit.
        String currentLabel = "学院当前选项，" + "说明".repeat(140);
        when(mapper.selectDictDataByType("poverty_level")).thenReturn(StudentImportTestSupport.entries("custom", currentLabel));
        Path template = Path.of("..", "ruoyi-admin", "src", "main", "resources", "template", "student_import_template.xlsx");
        Path resource = Path.of("target", "test-classes", "template", "student_import_template.xlsx");
        Files.createDirectories(resource.getParent());
        Files.copy(template, resource, StandardCopyOption.REPLACE_EXISTING);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.importTemplate(response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
             Workbook original = WorkbookFactory.create(Files.newInputStream(template))) {
            Sheet sheet = workbook.getSheetAt(0), source = original.getSheetAt(0);
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.isSheetHidden(1)).isTrue();
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getNumMergedRegions()).isEqualTo(source.getNumMergedRegions());
            assertThat(sheet.getDataValidations()).hasSize(5);
            int[] columns = {5, 7, 23, 24, 25};
            String[] labels = {"男", "群众", "在读", "正常", currentLabel};
            for (int i = 0; i < columns.length; i++) {
                DataValidation validation = sheet.getDataValidations().get(i);
                assertThat(validation.getEmptyCellAllowed()).isTrue();
                assertThat(validation.getShowErrorBox()).isTrue();
                assertThat(validation.getErrorStyle()).isEqualTo(DataValidation.ErrorStyle.STOP);
                assertThat(validation.getRegions().getCellRangeAddresses()[0].getLastRow()).isEqualTo(1048575);
                assertThat(validation.getRegions().getCellRangeAddresses()[0].getFirstColumn()).isEqualTo(columns[i]);
                Name name = workbook.getName(validation.getValidationConstraint().getFormula1());
                assertThat(name).isNotNull();
                assertThat(name.getRefersToFormula()).contains("导入选项");
                assertThat(workbook.getSheetAt(1).getRow(0).getCell(i).getStringCellValue()).isEqualTo(labels[i]);
            }
            for (int i = 0; i < 27; i++) {
                assertThat(sheet.getRow(1).getCell(i).getStringCellValue()).isEqualTo(source.getRow(1).getCell(i).getStringCellValue());
                assertThat(sheet.getColumnStyle(i).getDataFormatString()).isEqualTo("@");
                assertThat(sheet.getColumnWidth(i)).isEqualTo(source.getColumnWidth(i));
            }
        }
    }

    @Test
    void blanksRemainBlankEvenWithUnavailableDictionariesAndNoDefaults() {
        StudentImportOptions options = options(mock(SysDictDataMapper.class));
        Map<String, String> row = new HashMap<>(Map.of("gender", "  ", "political_status", "", "poverty_level", ""));
        options.normalize(row, options.load());
        assertThat(row).hasSize(5);
        assertThat(row.values()).containsOnly("");
    }

    @Test
    void exactActiveLabelsAndCodesWorkButDisabledUnknownAndMultipleValuesFail() {
        SysDictDataMapper mapper = mock(SysDictDataMapper.class);
        var enabled = StudentImportTestSupport.entries("current", "当前中文选项", "disabled", "停用选项");
        enabled.get(1).setStatus("1");
        when(mapper.selectDictDataByType("political_status")).thenReturn(enabled);
        StudentImportOptions options = options(mapper);
        var snapshot = options.load();
        for (String value : List.of(" 当前中文选项 ", "current")) {
            Map<String, String> row = new HashMap<>(Map.of("political_status", value));
            options.normalize(row, snapshot);
            assertThat(row.get("political_status")).isEqualTo("current");
        }
        for (String value : List.of("disabled", "停用选项", "旧中文选项", "current,disabled", "当前中文选项,停用选项")) {
            assertThatThrownBy(() -> options.normalize(new HashMap<>(Map.of("political_status", value)), snapshot))
                    .isInstanceOf(ServiceException.class).hasMessageContaining("政治面貌").hasMessageContaining("选项");
        }
        // Re-download/re-execute sees a changed dictionary, rather than the earlier snapshot.
        when(mapper.selectDictDataByType("political_status")).thenReturn(StudentImportTestSupport.entries("new", "新选项"));
        assertThatThrownBy(() -> options.normalize(new HashMap<>(Map.of("political_status", "current")), options.load()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void nonblankInputAndTemplateFailClearlyWhenDictionaryIsMissing() throws Exception {
        StudentImportOptions options = options(mock(SysDictDataMapper.class));
        var snapshot = options.load();
        assertThatThrownBy(() -> options.normalize(new HashMap<>(Map.of("political_status", "群众")), snapshot))
                .hasMessageContaining("政治面貌").hasMessageContaining("字典为空");
        try (Workbook workbook = WorkbookFactory.create(Files.newInputStream(Path.of("..", "ruoyi-admin", "src", "main", "resources", "template", "student_import_template.xlsx")))) {
            assertThatThrownBy(() -> options.configureTemplate(workbook, snapshot)).hasMessageContaining("字典为空");
        }
    }
}
