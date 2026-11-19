package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class HistoricalTalkImportParserTest {
    @Test void readsStudentCodesAsTextAndPreservesMultipleParticipants() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("历史谈话");
            var header = sheet.createRow(0);
            String[] names = {"谈话类型", "谈话日期", "学生学号", "系统谈话人工号", "系统谈话人姓名", "其他谈话人", "谈话地点", "谈话内容", "标签"};
            for (int i = 0; i < names.length; i++) header.createCell(i).setCellValue(names[i]);
            var row = sheet.createRow(1);
            String[] values = {"集体谈话", "2026-01-02", "001,002", "T01,T02", "王老师,李老师", "家长（家长）", "教室", "谈话内容", "学业"};
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            workbook.write(out);
            var rows = new HistoricalTalkImportParser().parse(new ByteArrayInputStream(out.toByteArray()));
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).studentCodes()).containsExactly("001", "002");
            assertThat(rows.get(0).teacherCodes()).containsExactly("T01", "T02");
            assertThat(rows.get(0).teacherNames()).containsExactly("王老师", "李老师");
            assertThat(rows.get(0).rowNumber()).isEqualTo(2);
        }
    }
    @Test void templateUsesTextColumnsAndIncludesInstructions() throws Exception {
        byte[] bytes = new HistoricalTalkImportParser().template();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue()).isEqualTo("学生学号");
            assertThat(workbook.getSheetAt(0).getColumnStyle(2).getDataFormatString()).isEqualTo("@");
            assertThat(workbook.getSheetAt(0).getColumnStyle(3).getDataFormatString()).isEqualTo("@");
            assertThat(workbook.getSheet("填写说明")).isNotNull();
        }
    }

    @Test void identifiesOverlappingResolvedStudentsOnSameDayInsideFile() {
        var first = new com.ruoyi.talk.domain.TalkSessionCreateRequest(); first.setTalkTime("2026-01-02"); first.setStudentIds(java.util.List.of(1L));
        var second = new com.ruoyi.talk.domain.TalkSessionCreateRequest(); second.setTalkTime("2026-01-02"); second.setStudentIds(java.util.List.of(1L, 2L));
        var third = new com.ruoyi.talk.domain.TalkSessionCreateRequest(); third.setTalkTime("2026-01-03"); third.setStudentIds(java.util.List.of(1L));
        var a = new HistoricalTalkImportService.Preview(2, "", false, first);
        var b = new HistoricalTalkImportService.Preview(3, "", false, second);
        var c = new HistoricalTalkImportService.Preview(4, "", false, third);
        assertThat(HistoricalTalkImportService.resolvedFileDuplicateRows(java.util.List.of(a, b, c))).containsExactlyInAnyOrder(2, 3);
    }
    @Test void numericStudentCodeIsAReportedRowErrorWithoutDiscardingOtherRows() throws Exception {
        byte[] template = new HistoricalTalkImportParser().template();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(template)); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheetAt(0);
            var bad = sheet.createRow(1); bad.createCell(0).setCellValue("个别谈话"); bad.createCell(1).setCellValue("2026-01-02"); bad.createCell(2).setCellValue(123);
            var good = sheet.createRow(2); good.createCell(0).setCellValue("个别谈话"); good.createCell(1).setCellValue("2026-01-03"); good.createCell(2).setCellValue("001");
            workbook.write(out);
            var rows = new HistoricalTalkImportParser().parse(new ByteArrayInputStream(out.toByteArray()));
            assertThat(rows).hasSize(2); assertThat(rows.get(0).parseError()).contains("文本");
            assertThat(rows.get(1).parseError()).isEmpty(); assertThat(rows.get(1).studentCodes()).containsExactly("001");
        }
    }

}
