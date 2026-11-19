package com.ruoyi.talk.service;

import com.ruoyi.common.exception.ServiceException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/** Parses the explicit historical-import template; authorization is handled by the import service. */
public class HistoricalTalkImportParser {
    private static final List<String> HEADERS = List.of("谈话类型", "谈话日期", "学生学号", "系统谈话人工号", "系统谈话人姓名", "其他谈话人", "谈话地点", "谈话内容", "标签");
    public record Row(int rowNumber, String talkType, String talkDate, List<String> studentCodes,
                      List<String> teacherCodes, List<String> teacherNames, String otherSpeakers, String location, String content, List<String> tags, String parseError) {
        public Row(int rowNumber, String talkType, String talkDate, List<String> studentCodes, List<String> teacherCodes,
                   List<String> teacherNames, String otherSpeakers, String location, String content, List<String> tags) {
            this(rowNumber, talkType, talkDate, studentCodes, teacherCodes, teacherNames, otherSpeakers, location, content, tags, "");
        }
    }

    public byte[] template() {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var output = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("历史谈话");
            var header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
                sheet.setColumnWidth(i, (i == 7 ? 50 : 22) * 256);
            }
            var textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            sheet.setDefaultColumnStyle(2, textStyle);
            sheet.setDefaultColumnStyle(3, textStyle);
            var instructions = workbook.createSheet("填写说明");
            String[] notes = {"每行一场谈话；第一行表头请勿修改。", "谈话类型填写个别谈话或集体谈话；日期填写YYYY-MM-DD。", "学生学号、系统谈话人工号用文本填写，多人以英文逗号分隔。", "工号与姓名按顺序一一对应，多位老师用英文逗号分隔。", "系统外人员填写姓名或姓名（身份），多人以英文分号分隔。", "标签填写标签名称，多项用英文分号分隔；请先在系统创建标签。", "上传后先预览；疑似重复需由用户选择是否导入。"};
            instructions.setColumnWidth(0, 100 * 256);
            for (int i = 0; i < notes.length; i++) instructions.createRow(i).createCell(0).setCellValue(notes[i]);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) { throw new ServiceException("生成导入模板失败"); }
    }

    public List<Row> parse(InputStream input) {
        try (var workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) throw new ServiceException("模板缺少工作表");
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            var formatter = new DataFormatter();
            if (header == null) throw new ServiceException("模板缺少表头");
            for (int i = 0; i < HEADERS.size(); i++) {
                if (!HEADERS.get(i).equals(formatter.formatCellValue(header.getCell(i)).trim()))
                    throw new ServiceException("模板表头不匹配，请重新下载模板");
            }
            if (sheet.getLastRowNum() > 2000) throw new ServiceException("单次最多导入2000行");
            List<Row> result = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                String[] values = new String[HEADERS.size()];
                boolean blank = true;
                String rowError = "";
                for (int j = 0; j < values.length; j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        rowError = "不支持公式，请粘贴为文本";
                        values[j] = ""; blank = false; continue;
                    }
                    if ((j == 2 || j == 3) && cell != null && cell.getCellType() == CellType.NUMERIC)
                        rowError = "学号和工号必须使用文本格式";
                    if (j == 1 && cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
                        values[j] = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    else values[j] = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    blank &= values[j].isEmpty();
                }
                if (!blank) result.add(new Row(i + 1, values[0], values[1], split(values[2]), split(values[3]), split(values[4]), values[5], values[6], values[7], split(values[8]), rowError));
            }
            if (result.isEmpty()) throw new ServiceException("模板没有数据行");
            return result;
        } catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException("文件无法解析，请上传有效的Excel模板"); }
    }
    private static List<String> split(String value) {
        if (value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,，;；\\n]", -1)).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }
}
