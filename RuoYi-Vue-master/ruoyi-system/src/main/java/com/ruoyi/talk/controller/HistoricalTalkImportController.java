package com.ruoyi.talk.controller;

import com.ruoyi.talk.service.HistoricalTalkImportParser;
import com.ruoyi.talk.service.HistoricalTalkImportService;
import com.ruoyi.talk.service.ITalkSessionService;
import com.ruoyi.talk.mapper.HistoricalTalkImportMapper;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.SecurityUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ruoyi-system/talksession/import")
public class HistoricalTalkImportController extends BaseController {
    @Autowired private HistoricalTalkImportService service;
    @Autowired private RedisCache redis;
    @Autowired private ITalkSessionService sessions;
    @Autowired private TransactionTemplate transactions;
    @Autowired private HistoricalTalkImportMapper historicalMapper;

    public static class ExecuteRequest {
        private String token;
        private List<Integer> selectedRows = new ArrayList<>();
        private List<Integer> allowDuplicateRows = new ArrayList<>();
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public List<Integer> getSelectedRows() { return selectedRows; }
        public void setSelectedRows(List<Integer> rows) { this.selectedRows = rows; }
        public List<Integer> getAllowDuplicateRows() { return allowDuplicateRows; }
        public void setAllowDuplicateRows(List<Integer> rows) { this.allowDuplicateRows = rows; }
    }

    @PreAuthorize("@ss.hasPermi('talk:session:add')")
    @Log(title = "历史谈话导入", businessType = BusinessType.IMPORT, isSaveResponseData = false)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody ExecuteRequest input) {
        if (input == null || input.getToken() == null || !input.getToken().matches("[0-9a-fA-F-]{36}")) return error("导入预览已失效，请重新上传");
        if (input.getSelectedRows() == null || input.getSelectedRows().isEmpty()) return error("请选择要导入的记录");
        Set<Integer> selected = new HashSet<>(input.getSelectedRows());
        Set<Integer> allowed = input.getAllowDuplicateRows() == null ? Set.of() : new HashSet<>(input.getAllowDuplicateRows());
        if (selected.contains(null) || selected.size() > 2000 || !selected.containsAll(allowed)) return error("导入选择无效");
        String user = String.valueOf(SecurityUtils.getUserId());
        String key = "talk:historical:preview:" + user + ":" + input.getToken();
        String saved = redis.getCacheObject(key);
        if (saved == null) return error("导入预览已失效，请重新上传");
        var rows = new HistoricalTalkImportParser().parse(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(saved)));
        Set<Integer> existingRows = new HashSet<>(); rows.forEach(r -> existingRows.add(r.rowNumber()));
        if (!existingRows.containsAll(selected)) return error("导入选择包含不存在的行");
        String claim = "talk:historical:claim:" + user + ":" + input.getToken();
        Boolean claimed = redis.redisTemplate.opsForValue().setIfAbsent(claim, "1", 24, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(claimed)) return error("该预览已提交或正在处理，请查看结果后重新预览");
        List<HistoricalTalkImportService.Preview> allChecked = new ArrayList<>();
        for (var row : rows) allChecked.add(service.inspect(row));
        Set<Integer> fileDuplicates = HistoricalTalkImportService.resolvedFileDuplicateRows(allChecked);
        List<Map<String, Object>> outcomes = new ArrayList<>();
        for (var row : rows) {
            if (!selected.contains(row.rowNumber())) continue;
            Map<String, Object> result = new LinkedHashMap<>(); result.put("rowNumber", row.rowNumber());
            try {
                var checked = service.inspect(row);
                if (!checked.error().isEmpty()) { result.put("status", "error"); result.put("message", checked.error()); }
                else if ((checked.duplicate() || fileDuplicates.contains(row.rowNumber())) && !allowed.contains(row.rowNumber())) {
                    result.put("status", "skipped"); result.put("message", "疑似重复，未选择继续导入");
                } else {
                    List<Long> studentIds = new ArrayList<>(checked.request().getStudentIds());
                    Collections.sort(studentIds);
                    transactions.executeWithoutResult(status -> {
                        List<Long> lockedIds = historicalMapper.lockStudents(studentIds);
                        if (!studentIds.equals(lockedIds)) throw new com.ruoyi.common.exception.ServiceException("学生信息已变化，请重新预览");
                        var fresh = service.inspect(row);
                        if (!fresh.error().isEmpty()) {
                            result.put("status", "error"); result.put("message", fresh.error());
                        } else if (!new HashSet<>(studentIds).equals(new HashSet<>(fresh.request().getStudentIds()))) {
                            throw new com.ruoyi.common.exception.ServiceException("学生信息已变化，请重新预览");
                        } else if ((fresh.duplicate() || fileDuplicates.contains(row.rowNumber())) && !allowed.contains(row.rowNumber())) {
                            result.put("status", "skipped"); result.put("message", "疑似重复，未选择继续导入");
                        } else {
                            sessions.createTalkWithRecords(fresh.request());
                            result.put("status", "success"); result.put("message", "导入成功");
                        }
                    });
                }
            } catch (Exception e) {
                logger.warn("历史谈话导入行失败：{}", row.rowNumber(), e);
                result.put("status", "error"); result.put("message", e instanceof com.ruoyi.common.exception.ServiceException ? e.getMessage() : "导入失败，请检查该行数据后重试");
            }
            outcomes.add(result);
        }
        return success(outcomes);
    }

    @PreAuthorize("@ss.hasPermi('talk:session:add')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return error("请选择文件");
        if (file.getSize() > 5 * 1024 * 1024) return error("文件不能超过5MB");
        try {
            byte[] bytes = file.getBytes();
            var rows = new HistoricalTalkImportParser().parse(new java.io.ByteArrayInputStream(bytes));
            List<HistoricalTalkImportService.Preview> checkedRows = new ArrayList<>();
            for (var row : rows) checkedRows.add(service.inspect(row));
            Set<Integer> fileDuplicates = HistoricalTalkImportService.resolvedFileDuplicateRows(checkedRows);
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                var row = rows.get(i);
                var checked = checkedRows.get(i);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("row", row); entry.put("error", checked.error());
                entry.put("duplicate", checked.duplicate() || fileDuplicates.contains(row.rowNumber()));
                entry.put("fileDuplicate", fileDuplicates.contains(row.rowNumber()));
                results.add(entry);
            }
            String token = UUID.randomUUID().toString();
            String key = "talk:historical:preview:" + SecurityUtils.getUserId() + ":" + token;
            redis.setCacheObject(key, Base64.getEncoder().encodeToString(bytes), 30, TimeUnit.MINUTES);
            Map<String, Object> data = new LinkedHashMap<>(); data.put("token", token); data.put("rows", results);
            return success(data);
        } catch (IOException e) { return error("读取上传文件失败"); }
    }

    @PreAuthorize("@ss.hasPermi('talk:session:add')")
    @Log(title = "下载历史谈话导入模板", businessType = BusinessType.EXPORT)
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        byte[] content = new HistoricalTalkImportParser().template();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode("历史谈话导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20"));
        response.getOutputStream().write(content);
    }
}
