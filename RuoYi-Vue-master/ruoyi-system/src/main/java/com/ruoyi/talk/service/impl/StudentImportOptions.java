package com.ruoyi.talk.service.impl;

import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.mapper.SysDictDataMapper;

/** One current dictionary snapshot per download/preview/execute, shared by template and validation. */
@Component
public class StudentImportOptions {
    @Autowired private SysDictDataMapper dictDataMapper;

    private record Field(String key, String title, String dictType, int column) {}
    private static final List<Field> FIELDS = List.of(
            new Field("gender", "性别", "sys_user_sex", 5),
            new Field("political_status", "政治面貌", "political_status", 7),
            new Field("enrollment_status", "学籍状态", "enrollment_status", 23),
            new Field("mental_health_status", "心理健康状态", "mental_health_status", 24),
            new Field("poverty_level", "贫困等级认定", "poverty_level", 25));

    public Map<String, Map<String, String>> load() {
        Map<String, Map<String, String>> snapshot = new LinkedHashMap<>();
        for (Field field : FIELDS) {
            Map<String, String> choices = new LinkedHashMap<>();
            List<SysDictData> entries = dictDataMapper.selectDictDataByType(field.dictType());
            if (entries != null) for (SysDictData entry : entries) {
                if ("0".equals(entry.getStatus()) && entry.getDictValue() != null && entry.getDictLabel() != null
                        && !entry.getDictValue().isBlank() && !entry.getDictLabel().isBlank()) {
                    choices.put(entry.getDictValue(), entry.getDictLabel());
                }
            }
            snapshot.put(field.key(), choices);
        }
        return snapshot;
    }

    public void normalize(Map<String, String> data, Map<String, Map<String, String>> snapshot) {
        for (Field field : FIELDS) {
            String value = Objects.toString(data.get(field.key()), "").trim();
            if (value.isEmpty()) { data.put(field.key(), ""); continue; }
            Map<String, String> choices = snapshot.get(field.key());
            if (choices.isEmpty()) throw new ServiceException(field.title() + "选项字典为空，请管理员配置后重试");
            if (choices.containsKey(value)) { data.put(field.key(), value); continue; }
            List<String> matches = choices.entrySet().stream().filter(e -> e.getValue().equals(value))
                    .map(Map.Entry::getKey).toList();
            if (matches.size() != 1) throw new ServiceException(field.title() + "选项无效或不唯一：[" + value
                    + "]，请选择当前模板中的单个选项，也可留空");
            data.put(field.key(), matches.get(0));
        }
    }

    public void configureTemplate(Workbook workbook, Map<String, Map<String, String>> snapshot) {
        XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);
        // The packaged template's only validations are the five enum lists being replaced.
        if (sheet.getCTWorksheet().isSetDataValidations()) sheet.getCTWorksheet().unsetDataValidations();
        Sheet lists = workbook.createSheet("导入选项");
        for (int index = 0; index < FIELDS.size(); index++) {
            Field field = FIELDS.get(index);
            Collection<String> labels = snapshot.get(field.key()).values();
            if (labels.isEmpty()) throw new ServiceException(field.title() + "选项字典为空，无法生成模板");
            int rowIndex = 0;
            for (String label : labels) {
                Row row = lists.getRow(rowIndex);
                if (row == null) row = lists.createRow(rowIndex);
                row.createCell(index).setCellValue(label);
                rowIndex++;
            }
            String column = CellReference.convertNumToColString(index);
            Name name = workbook.createName();
            name.setNameName("student_" + field.key());
            name.setRefersToFormula("'导入选项'!$" + column + "$1:$" + column + "$" + rowIndex);
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidation validation = helper.createValidation(helper.createFormulaListConstraint(name.getNameName()),
                    new CellRangeAddressList(2, 1048575, field.column(), field.column()));
            validation.setEmptyCellAllowed(true);
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.setSuppressDropDownArrow(true);
            validation.createErrorBox("选项无效", "请选择下拉框中的单个中文选项，未知信息可留空。");
            sheet.addValidationData(validation);
            Cell header = sheet.getRow(1).getCell(field.column());
            if (header.getCellComment() != null) header.getCellComment().setString(workbook.getCreationHelper()
                    .createRichTextString("可留空；请选择中文下拉选项，选项来自下载时系统启用的字典。"));
        }
        workbook.setSheetHidden(workbook.getSheetIndex(lists), true);
    }
}
