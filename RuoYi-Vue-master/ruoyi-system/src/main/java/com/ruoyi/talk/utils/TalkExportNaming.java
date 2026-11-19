package com.ruoyi.talk.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ruoyi.talk.domain.TalkAttachment;
import com.ruoyi.talk.domain.TalkSession;
import com.ruoyi.talk.domain.TalkStudentRecord;

/**
 * 谈话文书导出的统一命名规则。单条导出、单场 ZIP、批量 ZIP 共用同一套规则，
 * 保证同一份文书在不同导出入口下名称一致。
 */
public final class TalkExportNaming {

    /** ZIP 内附件目录。 */
    public static final String ATTACHMENT_DIR = "附件/";

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final int MAX_ORIGINAL_BASE = 80;

    private TalkExportNaming() {
    }

    /** 一个待命名的“学生 × 谈话”文书。 */
    public record DocxItem(TalkSession session, TalkStudentRecord record, String studentName, String studentCode) {
        public static DocxItem of(TalkSession session, TalkStudentRecord record) {
            return new DocxItem(session, record, record.getStudentName(), record.getStudentCode());
        }
    }

    /**
     * 为一个会话内的学生记录生成文件名，按传入顺序返回。重名时先补学号，仍重复再加 _2、_3。
     */
    public static List<String> docxNames(TalkSession session, List<TalkStudentRecord> records) {
        List<DocxItem> items = new ArrayList<>();
        if (records != null) {
            for (TalkStudentRecord record : records) {
                items.add(DocxItem.of(session, record));
            }
        }
        return docxNames(items);
    }

    /**
     * 为多个会话中的学生记录统一生成文件名（批量导出），按传入顺序返回。
     */
    public static List<String> docxNames(Collection<DocxItem> items) {
        List<String> names = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return names;
        }
        List<DocxItem> ordered = new ArrayList<>(items);
        List<String> baseKeys = new ArrayList<>(ordered.size());
        Map<String, Integer> baseCount = new HashMap<>();
        for (DocxItem item : ordered) {
            String key = studentLabel(item.studentName()) + '\u0000'
                    + personLabel(item.session()) + '\u0000'
                    + dayLabel(item.session().getTalkTime()) + '\u0000'
                    + typeLabel(item.session().getTalkType()) + '\u0000'
                    + minuteLabel(item.session().getTalkTime());
            baseKeys.add(key);
            baseCount.merge(key, 1, Integer::sum);
        }
        Map<String, Integer> rendered = new HashMap<>();
        for (int i = 0; i < baseKeys.size(); i++) {
            DocxItem item = ordered.get(i);
            String student = studentLabel(item.studentName());
            if (baseCount.get(baseKeys.get(i)) > 1) {
                student = student + "(" + codeLabel(item.studentCode()) + ")";
            }
            String name = student + "_" + personLabel(item.session()) + "_"
                    + dayLabel(item.session().getTalkTime()) + "_"
                    + typeLabel(item.session().getTalkType()) + "_"
                    + minuteLabel(item.session().getTalkTime()) + ".docx";
            int occurrence = rendered.merge(name, 1, Integer::sum);
            if (occurrence > 1) {
                name = appendSuffix(name, occurrence);
            }
            names.add(name);
        }
        return names;
    }

    /** 单场集体谈话的 ZIP 文件名，使用这场谈话的时间。 */
    public static String zipName(TalkSession session) {
        return "谈话记录_" + personLabel(session) + "_"
                + dayLabel(session.getTalkTime()) + "_"
                + minuteLabel(session.getTalkTime()) + ".zip";
    }

    /** 批量导出的 ZIP 文件名，使用导出时刻；跨多个谈话人时写“首位等 N 人”。 */
    public static String zipName(Collection<String> talkPersons, LocalDateTime exportTime) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        if (talkPersons != null) {
            for (String person : talkPersons) {
                labels.add(safe(person, "未填写"));
            }
        }
        String who;
        if (labels.isEmpty()) {
            who = "未填写";
        } else if (labels.size() == 1) {
            who = labels.iterator().next();
        } else {
            List<String> sorted = new ArrayList<>(labels);
            Collections.sort(sorted);
            who = sorted.get(0) + "等" + labels.size() + "人";
        }
        LocalDateTime time = exportTime == null ? LocalDateTime.now() : exportTime;
        return "谈话记录_" + who + "_" + STAMP.format(time) + ".zip";
    }

    /** 集体谈话汇总表文件名（保持原有命名口径，仅做文件名清洗）。 */
    public static String summaryName(TalkSession session) {
        return "集体谈话汇总表_" + personLabel(session) + ".docx";
    }

    /**
     * 附件名：原文件名主体在前，扩展名在最后；序号为该会话内按附件 ID 排序后的位置。
     */
    public static String attachmentName(TalkSession session, TalkAttachment attachment, int sequence) {
        String original = safe(attachment == null ? null : attachment.getFileName(), "附件");
        int dot = original.lastIndexOf('.');
        String base = dot > 0 ? original.substring(0, dot) : original;
        String extension = dot > 0 ? original.substring(dot + 1) : "";
        if (base.isEmpty()) {
            base = "附件";
        }
        if (base.length() > MAX_ORIGINAL_BASE) {
            base = base.substring(0, MAX_ORIGINAL_BASE);
        }
        extension = extension.replaceAll("[^A-Za-z0-9]", "");
        if (extension.length() > 12) {
            extension = extension.substring(0, 12);
        }
        return base + "_" + dayLabel(session.getTalkTime()) + "_"
                + personLabel(session) + "_" + typeLabel(session.getTalkType()) + "_"
                + sequence + (extension.isEmpty() ? "" : "." + extension);
    }

    /** 同一会话内的附件按附件 ID 稳定排序，供 ZIP 与单个下载共用序号。 */
    public static List<TalkAttachment> sortedAttachments(List<TalkAttachment> attachments) {
        List<TalkAttachment> sorted = attachments == null ? new ArrayList<>() : new ArrayList<>(attachments);
        sorted.sort(Comparator.comparing(TalkAttachment::getAttachmentId,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    /** 归档内重名兜底：在扩展名前追加 _2、_3。 */
    public static String uniqueName(String name, Set<String> used) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (used == null || used.add(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        int suffix = 2;
        String candidate;
        do {
            candidate = base + "_" + suffix + extension;
            suffix++;
        } while (!used.add(candidate));
        return candidate;
    }

    /** 导出时刻（批量 ZIP 名使用）。 */
    public static String exportStamp(LocalDateTime time) {
        return STAMP.format(time == null ? LocalDateTime.now() : time);
    }

    private static String appendSuffix(String name, int suffix) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name + "_" + suffix;
        }
        return name.substring(0, dot) + "_" + suffix + name.substring(dot);
    }

    private static String studentLabel(String value) {
        return safe(value, "未命名");
    }

    private static String codeLabel(String value) {
        return safe(value, "无学号");
    }

    private static String personLabel(TalkSession session) {
        return safe(session == null ? null : session.getTalkPerson(), "未填写");
    }

    private static String typeLabel(String talkType) {
        return "group".equals(talkType) ? "集体谈话" : "个别谈话";
    }

    private static String dayLabel(Date talkTime) {
        return talkTime == null ? "未知日期" : DAY.format(toLocal(talkTime));
    }

    private static String minuteLabel(Date talkTime) {
        return talkTime == null ? "未知时间" : MINUTE.format(toLocal(talkTime));
    }

    private static LocalDateTime toLocal(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** 去掉路径与文件名非法字符；空值返回给定兜底文本。 */
    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String cleaned = value.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).trim();
        cleaned = cleaned.replaceAll("[\\x00-\\x1f\\x7f<>:\"|?*]", "_");
        if (cleaned.isEmpty() || ".".equals(cleaned) || "..".equals(cleaned)) {
            return fallback;
        }
        return cleaned;
    }
}
