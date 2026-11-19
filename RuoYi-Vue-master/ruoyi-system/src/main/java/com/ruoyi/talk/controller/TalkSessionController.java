package com.ruoyi.talk.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.domain.TalkSessionCreateRequest;
import com.ruoyi.talk.domain.TalkSessionTag;
import com.ruoyi.talk.domain.TalkStudentRecord;
import com.ruoyi.talk.mapper.TalkStudentRecordMapper;
import com.ruoyi.talk.service.ITalkSessionService;
import com.ruoyi.talk.service.TalkDocxService;
import com.ruoyi.talk.utils.TalkExportNaming;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 谈话会话管理Controller
 * 
 * @author admin
 * @date 2026-05-27
 */
@RestController
@RequestMapping("/ruoyi-system/talksession")
public class TalkSessionController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(TalkSessionController.class);

    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Autowired
    private ITalkSessionService talkSessionService;

    @Autowired
    private TalkDocxService talkDocxService;

    @Autowired
    private TalkStudentRecordMapper talkStudentRecordMapper;

    @PreAuthorize("@ss.hasPermi('talk:session:add')")
    @GetMapping("/speaker-options")
    public AjaxResult speakerOptions(@RequestParam Long studentId) {
        return success(talkSessionService.selectSpeakerOptions(studentId));
    }

    /**
     * 查询谈话会话管理列表
     */
    @PreAuthorize("@ss.hasPermi('talk:session:list')")
    @GetMapping("/list")
    public TableDataInfo list(TalkSession talkSession) {
        startPage();
        List<TalkSession> list = talkSessionService.selectTalkSessionList(talkSession);
        return getDataTable(list);
    }

    /**
     * 导出谈话会话管理列表
     */
    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "导出谈话列表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TalkSession talkSession) {
        List<TalkSession> list = talkSessionService.selectTalkSessionList(talkSession);
        ExcelUtil<TalkSession> util = new ExcelUtil<TalkSession>(TalkSession.class);
        util.exportExcel(response, list, "谈话会话管理数据");
    }

    /**
     * 导出单个会话：个别谈话返回一个 docx，集体谈话返回整场扁平 ZIP。
     */
    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "导出单场谈话文书", businessType = BusinessType.EXPORT)
    @GetMapping("/exportDocx/{sessionId}")
    public void exportDocx(@PathVariable Long sessionId, HttpServletResponse response) throws Exception {
        TalkSession session = talkSessionService.selectTalkSessionBySessionId(sessionId);
        if (session == null) {
            writeJsonError(response, "会话不存在");
            return;
        }
        List<TalkStudentRecord> records = sessionRecords(sessionId);
        if (records.isEmpty()) {
            throw new ServiceException("该谈话没有参与学生，无法导出");
        }
        byte[] data = talkDocxService.generateDocxBySession(sessionId);
        if ("group".equals(session.getTalkType())) {
            writeDownload(response, data, TalkExportNaming.zipName(session), true);
        } else {
            List<String> names = TalkExportNaming.docxNames(session, records);
            writeDownload(response, data, names.get(0), false);
        }
    }

    /**
     * 导出单个学生的谈话记录为 .docx
     */
    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "导出单个学生谈话文书", businessType = BusinessType.EXPORT)
    @GetMapping("/exportDocx/{sessionId}/student/{studentId}")
    public void exportDocxForStudent(@PathVariable Long sessionId, @PathVariable Long studentId,
            HttpServletResponse response) throws Exception {
        TalkSession session = talkSessionService.selectTalkSessionBySessionId(sessionId);
        if (session == null) {
            writeJsonError(response, "会话不存在");
            return;
        }
        List<TalkStudentRecord> records = sessionRecords(sessionId);
        int index = -1;
        for (int i = 0; i < records.size(); i++) {
            if (Objects.equals(records.get(i).getStudentId(), studentId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ServiceException("当前会话没有该学生的谈话记录");
        }
        List<String> names = TalkExportNaming.docxNames(session, records);
        byte[] docxBytes = talkDocxService.generateDocxByStudent(studentId, sessionId);
        writeDownload(response, docxBytes, names.get(index), false);
    }

    /**
     * 发起谈话（集体/个体）— 创建 session + N条 record
     */
    @PreAuthorize("@ss.hasPermi('talk:session:add')")
    @Log(title = "发起谈话", businessType = BusinessType.INSERT)
    @RepeatSubmit(message = "请勿重复创建谈话")
    @PostMapping("/create")
    public AjaxResult createTalk(@RequestBody TalkSessionCreateRequest request) {
        try {
            TalkSession session = talkSessionService.createTalkWithRecords(request);
            return success(session);
        } catch (ServiceException rejected) {
            AjaxResult result = AjaxResult.error(rejected.getMessage());
            result.put("writeRejected", true);
            return result;
        }
    }

    /**
     * 批量导出：顶层一个扁平 ZIP，内部每个学生一个 docx，附件放入 附件/ 目录。
     */
    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "批量导出谈话文书", businessType = BusinessType.EXPORT)
    @PostMapping("/exportDocx/batch")
    public void exportDocxBatch(@RequestBody Map<String, List<Long>> body, HttpServletResponse response) throws Exception {
        List<Long> sessionIds = body == null ? null : body.get("sessionIds");
        if (sessionIds == null || sessionIds.isEmpty()) {
            throw new ServiceException("sessionIds不能为空");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long sessionId : sessionIds) {
            if (sessionId == null) {
                throw new ServiceException("谈话会话不存在");
            }
            uniqueIds.add(sessionId);
        }
        List<TalkSession> sessions = new ArrayList<>();
        Map<Long, List<TalkStudentRecord>> recordsBySession = new LinkedHashMap<>();
        for (Long sid : uniqueIds) {
            TalkSession session = talkSessionService.selectTalkSessionBySessionId(sid);
            if (session == null) {
                throw new ServiceException("谈话会话不存在");
            }
            List<TalkStudentRecord> records = sessionRecords(sid);
            if (!records.isEmpty()) {
                sessions.add(session);
                recordsBySession.put(sid, records);
            }
        }
        List<TalkExportNaming.DocxItem> items = new ArrayList<>();
        for (TalkSession session : sessions) {
            for (TalkStudentRecord record : recordsBySession.get(session.getSessionId())) {
                items.add(TalkExportNaming.DocxItem.of(session, record));
            }
        }
        if (items.isEmpty()) {
            throw new ServiceException("没有可导出的学生谈话记录");
        }
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zip)) {
            List<String> names = TalkExportNaming.docxNames(items);
            for (int i = 0; i < items.size(); i++) {
                TalkExportNaming.DocxItem item = items.get(i);
                byte[] docxBytes = talkDocxService.generateDocxForRecord(item.session(), item.record());
                zos.putNextEntry(new ZipEntry(names.get(i)));
                zos.write(docxBytes);
                zos.closeEntry();
            }
            Set<String> usedAttachmentNames = new HashSet<>();
            for (TalkSession session : sessions) {
                talkDocxService.writeAttachmentsToZip(zos, session, usedAttachmentNames);
            }
        }
        List<String> talkPersons = new ArrayList<>();
        for (TalkSession session : sessions) {
            talkPersons.add(session.getTalkPerson());
        }
        writeDownload(response, zip.toByteArray(),
                TalkExportNaming.zipName(talkPersons, LocalDateTime.now()), true);
    }

    /**
     * 按选中的学生记录导出，不扩展到会话内其他学生；顶层一个扁平 ZIP。
     */
    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "批量导出学生谈话记录", businessType = BusinessType.EXPORT)
    @PostMapping("/exportDocx/records/batch")
    public void exportDocxRecordsBatch(@RequestBody Map<String, List<Long>> body,
            HttpServletResponse response) throws Exception {
        List<Long> recordIds = body == null ? null : body.get("recordIds");
        if (recordIds == null || recordIds.isEmpty()) {
            throw new ServiceException("请选择需要导出的学生谈话记录");
        }
        Map<Long, TalkStudentRecord> selectedRecords = new LinkedHashMap<>();
        for (Long recordId : new LinkedHashSet<>(recordIds)) {
            TalkStudentRecord record = recordId == null ? null
                    : talkStudentRecordMapper.selectTalkStudentRecordByRecordId(recordId);
            if (record == null) {
                throw new ServiceException("学生谈话记录不存在");
            }
            // 在生成任一文档之前校验全部会话的读取范围。
            if (talkSessionService.selectTalkSessionBySessionId(record.getSessionId()) == null
                    || record.getStudentId() == null) {
                throw new ServiceException("谈话记录关联的会话或学生不存在");
            }
            selectedRecords.put(recordId, record);
        }
        Map<Long, TalkSession> sessions = new LinkedHashMap<>();
        List<TalkExportNaming.DocxItem> items = new ArrayList<>();
        for (TalkStudentRecord record : selectedRecords.values()) {
            TalkSession session = sessions.get(record.getSessionId());
            if (session == null) {
                session = talkSessionService.selectTalkSessionBySessionId(record.getSessionId());
                if (session == null) {
                    throw new ServiceException("谈话记录关联的会话或学生不存在");
                }
                sessions.put(record.getSessionId(), session);
            }
            items.add(TalkExportNaming.DocxItem.of(session, record));
        }
        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(zip)) {
            List<String> names = TalkExportNaming.docxNames(items);
            for (int i = 0; i < items.size(); i++) {
                TalkExportNaming.DocxItem item = items.get(i);
                byte[] document = talkDocxService.generateDocxForRecord(item.session(), item.record());
                output.putNextEntry(new ZipEntry(names.get(i)));
                output.write(document);
                output.closeEntry();
            }
            Set<String> usedAttachmentNames = new HashSet<>();
            for (TalkSession session : sessions.values()) {
                talkDocxService.writeAttachmentsToZip(output, session, usedAttachmentNames);
            }
        }
        List<String> talkPersons = new ArrayList<>();
        for (TalkSession session : sessions.values()) {
            talkPersons.add(session.getTalkPerson());
        }
        writeDownload(response, zip.toByteArray(),
                TalkExportNaming.zipName(talkPersons, LocalDateTime.now()), true);
    }

    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "导出学生谈话记录", businessType = BusinessType.EXPORT)
    @GetMapping("/exportDocx/record/{recordId}")
    public void exportDocxForRecord(@PathVariable Long recordId, HttpServletResponse response) throws Exception {
        TalkStudentRecord record = talkStudentRecordMapper.selectTalkStudentRecordByRecordId(recordId);
        if (record == null) {
            throw new ServiceException("学生谈话记录不存在");
        }
        TalkSession session = talkSessionService.selectTalkSessionBySessionId(record.getSessionId());
        if (session == null) {
            throw new ServiceException("会话不存在");
        }
        List<TalkStudentRecord> records = sessionRecords(record.getSessionId());
        int index = -1;
        for (int i = 0; i < records.size(); i++) {
            if (Objects.equals(records.get(i).getRecordId(), recordId)) {
                index = i;
                break;
            }
        }
        List<String> names = TalkExportNaming.docxNames(session, records);
        byte[] document = talkDocxService.generateDocxByRecord(recordId);
        String fileName = index >= 0 ? names.get(index) : "学生谈话记录_" + recordId + ".docx";
        writeDownload(response, document, fileName, false);
    }

    /**
     * 导出集体谈话汇总表 (.docx)
     */
    @PreAuthorize("@ss.hasPermi('talk:session:export')")
    @Log(title = "导出集体谈话汇总", businessType = BusinessType.EXPORT)
    @GetMapping("/exportGroupSummary/{sessionId}")
    public void exportGroupSummary(@PathVariable Long sessionId, HttpServletResponse response) throws Exception {
        TalkSession session = talkSessionService.selectTalkSessionBySessionId(sessionId);
        if (session == null) {
            writeJsonError(response, "会话不存在");
            return;
        }
        byte[] docxBytes = talkDocxService.generateGroupSummaryDocx(sessionId);
        writeDownload(response, docxBytes, TalkExportNaming.summaryName(session), false);
    }

    private List<TalkStudentRecord> sessionRecords(Long sessionId) {
        List<TalkStudentRecord> records = talkStudentRecordMapper.selectTalkStudentRecordBySessionId(sessionId);
        return records == null ? new ArrayList<>() : records;
    }

    private void writeDownload(HttpServletResponse response, byte[] data, String fileName, boolean isZip)
            throws Exception {
        response.setContentType(isZip ? "application/zip" : DOCX_CONTENT_TYPE);
        FileUtils.setAttachmentResponseHeader(response, fileName);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }

    private void writeJsonError(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter()
                .write("{\"msg\":\"" + msg.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"code\":500}");
    }

    @PreAuthorize("@ss.hasPermi('talk:session:query')")
    @GetMapping("/detail/{sessionId:\\d+}")
    public AjaxResult getInfo(@PathVariable("sessionId") Long sessionId) {
        TalkSession session = talkSessionService.selectTalkSessionBySessionId(sessionId);
        return session == null ? error("谈话会话不存在") : success(session);
    }

    /**
     * 批量获取多个会话的标签（解决N+1查询问题）
     */
    @PreAuthorize("@ss.hasPermi('talk:session:query')")
    @GetMapping("/session-tags/batch")
    public AjaxResult getTagsBatch(@RequestParam String sessionIds) {
        return success(talkSessionService.selectTalkSessionTagsBatch(sessionIds));
    }

    @PreAuthorize("@ss.hasPermi('talk:session:query')")
    @GetMapping("/tags/{sessionId:\\d+}")
    public AjaxResult getTags(@PathVariable("sessionId") Long sessionId) {
        if (talkSessionService.selectTalkSessionBySessionId(sessionId) == null) {
            return error("谈话会话不存在");
        }
        return success(talkSessionService.selectTalkSessionTags(sessionId));
    }

    @PreAuthorize("@ss.hasPermi('talk:session:query')")
    @GetMapping("/participants/{sessionId:\\d+}")
    public AjaxResult getParticipants(@PathVariable("sessionId") Long sessionId) {
        if (talkSessionService.selectTalkSessionBySessionId(sessionId) == null) {
            return error("谈话会话不存在");
        }
        List<TalkStudentRecord> records = talkStudentRecordMapper.selectTalkStudentRecordBySessionId(sessionId);
        records.sort(Comparator.comparing(TalkStudentRecord::getStudentCode, Comparator.nullsLast(String::compareTo)));
        return success(records);
    }

    /**
     * 新增谈话会话管理
     */
    @PreAuthorize("@ss.hasPermi('talk:session:add')")
    @Log(title = "谈话会话管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TalkSession talkSession) {
        return error("请通过发起谈话创建会话并选择参与学生");
    }

    /**
     * 修改谈话会话管理
     */
    @PreAuthorize("@ss.hasPermi('talk:session:edit')")
    @Log(title = "谈话会话管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TalkSession talkSession) {
        return toAjax(talkSessionService.updateTalkSession(talkSession));
    }

    /**
     * 删除谈话会话管理
     */
    @PreAuthorize("@ss.hasPermi('talk:session:remove')")
    @Log(title = "谈话会话管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{sessionIds:[\\d,]+}")
    public AjaxResult remove(@PathVariable Long[] sessionIds) {
        return toAjax(talkSessionService.deleteTalkSessionBySessionIds(sessionIds));
    }
}
