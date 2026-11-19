package com.ruoyi.talk.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class HistoricalTalkImportControllerTest {
    @Test void downloadsNineColumnWorkbook() throws Exception {
        var response = new MockHttpServletResponse();
        new HistoricalTalkImportController().template(response);
        assertThat(response.getContentType()).contains("spreadsheetml");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(4).getStringCellValue()).isEqualTo("系统谈话人姓名");
            assertThat(workbook.getSheet("填写说明")).isNotNull();
        }
    }
    @Test void emptyUploadIsRejectedWithoutPreview() {
        var response = new HistoricalTalkImportController().preview(new org.springframework.mock.web.MockMultipartFile("file", new byte[0]));
        assertThat(response.get("code")).isEqualTo(500);
    }

    @Test void executeRejectsMissingToken() {
        var request = new HistoricalTalkImportController.ExecuteRequest();
        var result = new HistoricalTalkImportController().execute(request);
        assertThat(result.get("code")).isEqualTo(500);
    }

}
