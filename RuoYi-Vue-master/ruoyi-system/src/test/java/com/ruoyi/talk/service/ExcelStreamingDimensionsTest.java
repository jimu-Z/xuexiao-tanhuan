package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.poi.ExcelSheet;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletResponse;

class ExcelStreamingDimensionsTest {
    public static class Sample {
        @Excel(name = "值")
        public String value;
        Sample(String value) { this.value = value; }
    }

    private void assertReadableBounds(byte[] bytes, int sheetNumber, int lastRow) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                if (!entry.getName().equals("xl/worksheets/sheet" + sheetNumber + ".xml")) continue;
                String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(xml).contains("r=\"A" + lastRow + "\"");
                Matcher dimension = Pattern.compile("<dimension\\s+ref=\"([^\"]+)\"").matcher(xml);
                if (dimension.find()) {
                    assertThat(CellRangeAddress.valueOf(dimension.group(1)).getLastRow()).isGreaterThanOrEqualTo(lastRow - 1);
                }
                return;
            }
        }
        throw new AssertionError("missing worksheet " + sheetNumber);
    }

    @Test void singleSheetDoesNotLimitStreamingReadersToTheFirstCell() throws Exception {
        List<Sample> rows = IntStream.range(0, 550).mapToObj(i -> new Sample("row" + i)).toList();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExcelUtil<>(Sample.class).exportExcel(response, rows, "样本");
        assertReadableBounds(response.getContentAsByteArray(), 1, 551);
    }

    @Test void everySheetInMultiSheetExportHasReadableBounds() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtil.exportMultiSheet(response, List.of(
                new ExcelSheet<>("一", List.of(new Sample("a")), Sample.class),
                new ExcelSheet<>("二", List.of(new Sample("b"), new Sample("c")), Sample.class)));
        assertReadableBounds(response.getContentAsByteArray(), 1, 2);
        assertReadableBounds(response.getContentAsByteArray(), 2, 3);
    }

    @Test void fileExportAndMultiSheetFileExportHaveReadableBounds() throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "qa-excel-");
        Path singlePath = null;
        Path multiPath = null;
        try (MockedStatic<RuoYiConfig> config = Mockito.mockStatic(RuoYiConfig.class)) {
            config.when(RuoYiConfig::getDownloadPath).thenReturn(directory.toString() + java.io.File.separator);
            AjaxResult single = new ExcelUtil<>(Sample.class).exportExcel(List.of(new Sample("a")), "单表");
            assertThat(single.isSuccess()).isTrue();
            singlePath = directory.resolve((String) single.get(AjaxResult.MSG_TAG)).normalize();
            assertThat(singlePath.getParent()).isEqualTo(directory);
            assertReadableBounds(Files.readAllBytes(singlePath), 1, 2);

            AjaxResult multi = ExcelUtil.exportMultiSheet(List.of(
                    new ExcelSheet<>("一", List.of(new Sample("a")), Sample.class),
                    new ExcelSheet<>("二", List.of(new Sample("b"), new Sample("c")), Sample.class)));
            assertThat(multi.isSuccess()).isTrue();
            multiPath = directory.resolve((String) multi.get(AjaxResult.MSG_TAG)).normalize();
            assertThat(multiPath.getParent()).isEqualTo(directory);
            byte[] bytes = Files.readAllBytes(multiPath);
            assertReadableBounds(bytes, 1, 2);
            assertReadableBounds(bytes, 2, 3);
        } finally {
            if (singlePath != null && directory.equals(singlePath.getParent())) Files.deleteIfExists(singlePath);
            if (multiPath != null && directory.equals(multiPath.getParent())) Files.deleteIfExists(multiPath);
            Files.deleteIfExists(directory);
        }
    }
}
