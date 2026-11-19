package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.utils.poi.ExcelSheet;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class ExcelStreamingTempFilesTest {
    public static class Sample {
        @Excel(name="值") public String value = "QA temporary content";
    }

    private void assertNoStreamingFiles(Runnable export) throws Exception {
        Path directory=Files.createTempDirectory(Path.of("target").toAbsolutePath(), "qa-poi-");
        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(directory.toFile()));
        try {
            export.run();
            try (Stream<Path> files=Files.list(directory)) {
                assertThat(files.filter(p -> p.getFileName().toString().startsWith("poi-sxssf-sheet")).count()).isZero();
            }
        } finally {
            TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy());
            try (Stream<Path> files=Files.list(directory)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(directory);
        }
    }

    @Test void singleSheetResponseDeletesStreamingFiles() throws Exception {
        assertNoStreamingFiles(() -> new ExcelUtil<>(Sample.class)
                .exportExcel(new MockHttpServletResponse(), List.of(new Sample()), "测试"));
    }

    @Test void multiSheetResponseDeletesStreamingFiles() throws Exception {
        assertNoStreamingFiles(() -> ExcelUtil.exportMultiSheet(new MockHttpServletResponse(), List.of(
                new ExcelSheet<>("一", List.of(new Sample()), Sample.class),
                new ExcelSheet<>("二", List.of(new Sample()), Sample.class))));
    }

    @Test void failedResponseDeletesStreamingFiles() throws Exception {
        MockHttpServletResponse response=new MockHttpServletResponse() {
            @Override public ServletOutputStream getOutputStream() {
                return new ServletOutputStream() {
                    @Override public boolean isReady() { return true; }
                    @Override public void setWriteListener(WriteListener listener) { }
                    @Override public void write(int value) throws IOException { throw new IOException("QA simulated output failure"); }
                };
            }
        };
        assertNoStreamingFiles(() -> new ExcelUtil<>(Sample.class).exportExcel(response, List.of(new Sample()), "测试"));
    }

    @Test void failedMultiSheetConstructionDeletesStreamingFiles() throws Exception {
        assertNoStreamingFiles(() -> assertThatThrownBy(() -> ExcelUtil.exportMultiSheet(new MockHttpServletResponse(), List.of(
                new ExcelSheet<>("重复", List.of(new Sample()), Sample.class),
                new ExcelSheet<>("重复", List.of(new Sample()), Sample.class))))
                .isInstanceOf(IllegalArgumentException.class));
    }
}
