package com.ruoyi.talk.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.talk.domain.TalkStudentRecord;
import com.ruoyi.talk.service.ITalkAlertService;
import com.ruoyi.talk.service.ITalkStudentRecordService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 谈话记录管理Controller
 * 
 * @author admin
 * @date 2026-05-27
 */
@RestController
@RequestMapping("/ruoyi-system/talkrecord")
public class TalkStudentRecordController extends BaseController {
    @PreAuthorize("@ss.hasPermi('talk:session:add') or @ss.hasPermi('talk:record:list')")
    @PostMapping("/preview/text")
    public AjaxResult previewText(@RequestBody java.util.Map<String, String> input) {
        String content = input.getOrDefault("content", "");
        if (content == null || content.length() > 1000000) throw new com.ruoyi.common.exception.ServiceException("谈话内容过长");
        return success((Object) com.ruoyi.talk.service.TalkDocxService.stripTalkRecord(content));
    }
    @Autowired
    private com.ruoyi.talk.service.ITalkStudentService talkStudentService;
    @Autowired
    private com.ruoyi.system.mapper.SysDictDataMapper previewDictMapper;

    private String previewLabel(String type, String value, java.util.Map<String, String> fallback) {
        if (value == null || value.isBlank()) return "";
        String key = value.trim();
        String label = previewDictMapper.selectDictLabel(type, key);
        if (label != null && !label.isBlank()) return label;
        if (fallback.containsKey(key)) return fallback.get(key);
        return key.matches(".*[\\u3400-\\u9fff].*") ? key : "未知状态";
    }

    @PreAuthorize("@ss.hasPermi('talk:student:list') and @ss.hasPermi('talk:record:list')")
    @GetMapping("/preview/student/{studentId}")
    public AjaxResult previewStudent(@PathVariable Long studentId) {
        com.ruoyi.talk.domain.TalkStudent student = talkStudentService.selectTalkStudentByStudentId(studentId);
        if (student == null) throw new com.ruoyi.common.exception.ServiceException("学生不存在或无权访问");
        TalkStudentRecord filter = new TalkStudentRecord(); filter.setStudentId(studentId);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("student", student);
        java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("politicalStatus", previewLabel("political_status", student.getPoliticalStatus(), com.ruoyi.talk.constant.TalkConstants.POLITICAL_STATUS_LABELS));
        labels.put("enrollmentStatus", previewLabel("enrollment_status", student.getEnrollmentStatus(), com.ruoyi.talk.constant.TalkConstants.ENROLLMENT_STATUS_LABELS));
        labels.put("mentalHealthStatus", previewLabel("mental_health_status", student.getMentalHealthStatus(), com.ruoyi.talk.constant.TalkConstants.MENTAL_HEALTH_LABELS));
        labels.put("povertyLevel", previewLabel("poverty_level", student.getPovertyLevel(), com.ruoyi.talk.constant.TalkConstants.POVERTY_LEVEL_LABELS));
        data.put("labels", labels);
        java.util.List<java.util.Map<String, Object>> history = new java.util.ArrayList<>();
        for (TalkStudentRecord record : talkStudentRecordService.selectTalkStudentRecordList(filter)) {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("talkTime", record.getTalkTime()); item.put("talkType", record.getTalkType()); item.put("talkPerson", record.getTalkPerson());
            history.add(item);
        }
        data.put("history", history);
        return success(data);
    }
    @Autowired
    private ITalkStudentRecordService talkStudentRecordService;

    @Autowired
    private ITalkAlertService talkAlertService;

    /**
     * 查询当前学生自己的谈话记录（学生视角）或辅导员发起的谈话记录（辅导员视角）
     */
    @PreAuthorize("@ss.hasPermi('talk:record:list')")
    @GetMapping("/myRecords")
    public TableDataInfo myRecords(TalkStudentRecord talkStudentRecord) {
        startPage();
        List<TalkStudentRecord> list = talkStudentRecordService.selectTalkStudentRecordList(talkStudentRecord);
        return getDataTable(list);
    }

    /**
     * 查询谈话记录管理列表
     */
    @PreAuthorize("@ss.hasPermi('talk:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(TalkStudentRecord talkStudentRecord) {
        startPage();
        List<TalkStudentRecord> list = talkStudentRecordService.selectTalkStudentRecordList(talkStudentRecord);
        return getDataTable(list);
    }

    /**
     * 导出谈话记录管理列表
     */
    @PreAuthorize("@ss.hasPermi('talk:record:export')")
    @Log(title = "谈话记录管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TalkStudentRecord talkStudentRecord) {
        List<TalkStudentRecord> list = talkStudentRecordService.selectTalkStudentRecordList(talkStudentRecord);
        ExcelUtil<TalkStudentRecord> util = new ExcelUtil<TalkStudentRecord>(TalkStudentRecord.class);
        util.exportExcel(response, list, "谈话记录管理数据");
    }

    /** 获取谈话记录管理详细信息 */
    @PreAuthorize("@ss.hasPermi('talk:record:query')")
    @GetMapping(value = "/{recordId:\\d+}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId) {
        return success(talkStudentRecordService.selectTalkStudentRecordByRecordId(recordId));
    }

    /**
     * 新增谈话记录管理
     */
    @PreAuthorize("@ss.hasPermi('talk:record:add')")
    @Log(title = "谈话记录管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TalkStudentRecord talkStudentRecord) {
        return toAjax(talkStudentRecordService.insertTalkStudentRecord(talkStudentRecord));
    }

    /**
     * 学生提交反馈
     */
    @Log(title = "谈话记录管理", businessType = BusinessType.UPDATE)
    @PutMapping("/submitFeedback")
    public AjaxResult submitFeedback(@RequestBody TalkStudentRecord talkStudentRecord) {
        String username = SecurityUtils.getUsername();
        TalkStudentRecord existing = talkStudentRecordService
                .selectTalkStudentRecordByRecordId(talkStudentRecord.getRecordId());
        if (existing == null || !username.equals(existing.getStudentCode())) {
            return error("无权限操作此记录");
        }
        TalkStudentRecord update = new TalkStudentRecord();
        update.setRecordId(talkStudentRecord.getRecordId());
        update.setStudentFeedback(talkStudentRecord.getStudentFeedback());
        update.setOriginalStudentFeedback(talkStudentRecord.getStudentFeedback());
        update.setTeacherNotified(0);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = talkStudentRecordService.updateTalkStudentRecord(update);
        // 触发敏感词检测
        talkAlertService.checkStudentFeedbackForKeywords(existing.getStudentId(), talkStudentRecord.getStudentFeedback());
        return toAjax(rows);
    }

    /**
     * 修改谈话记录管理
     */
    @PreAuthorize("@ss.hasPermi('talk:record:edit')")
    @Log(title = "谈话记录管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TalkStudentRecord talkStudentRecord) {
        return toAjax(talkStudentRecordService.updateTalkStudentRecord(talkStudentRecord));
    }

    /**
     * 删除谈话记录管理
     */
    @PreAuthorize("@ss.hasPermi('talk:record:remove')")
    @Log(title = "谈话记录管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{recordIds:[\\d,]+}")
    public AjaxResult remove(@PathVariable Long[] recordIds) {
        return toAjax(talkStudentRecordService.deleteTalkStudentRecordByRecordIds(recordIds));
    }
}
