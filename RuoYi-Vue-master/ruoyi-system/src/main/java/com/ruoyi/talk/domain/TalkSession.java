package com.ruoyi.talk.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.talk.util.TalkExcelContentHandler;

/**
 * 谈话会话管理对象 talk_session
 * 
 * @author admin
 * @date 2026-05-27
 */
public class TalkSession extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long sessionId;
    private Long revision;
    public Long getRevision() { return revision; }
    public void setRevision(Long revision) { this.revision = revision; }

    /** 谈话发生时所属学院ID（历史归属快照） */
    private Long collegeDeptId;

    /** 谈话类型（individual 个体谈话 / group 集体谈话） */
    @Excel(name = "谈话类型", readConverterExp = "individual=个别谈话,group=集体谈话")
    private String talkType;

    /** 谈话时间 */
    @JsonFormat(pattern = "yyyy-MM-dd", lenient = com.fasterxml.jackson.annotation.OptBoolean.FALSE)
    @Excel(name = "谈话时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date talkTime;

    /** 谈话地点 */
    @Excel(name = "谈话地点")
    private String talkLocation;

    /** 谈话人 */
    @Excel(name = "谈话人")
    private String talkPerson;

    private java.util.List<TalkSessionParticipant> speakers;
    private Boolean currentUserParticipant;
    private Boolean mineOnly;
    public Boolean getMineOnly() { return mineOnly; }
    public void setMineOnly(Boolean value) { mineOnly = value; }
    public Boolean getCurrentUserParticipant() { return currentUserParticipant; }
    public void setCurrentUserParticipant(Boolean value) { currentUserParticipant = value; }
    public java.util.List<TalkSessionParticipant> getSpeakers() { return speakers; }
    public void setSpeakers(java.util.List<TalkSessionParticipant> speakers) { this.speakers = speakers; }

    /** 谈话内容（所有参与学生共享） */
    @Excel(name = "谈话内容", handler = TalkExcelContentHandler.class, wrapText = true)
    private String talkContent;

    /** 查询条件：学生学号或姓名、谈话标签 */
    private String studentKeyword;
    private String tagValue;

    public String getStudentKeyword() { return studentKeyword; }
    public void setStudentKeyword(String studentKeyword) { this.studentKeyword = studentKeyword; }
    public String getTagValue() { return tagValue; }
    public void setTagValue(String tagValue) { this.tagValue = tagValue; }

    public void setSessionId(Long sessionId) 
    {
        this.sessionId = sessionId;
    }

    public Long getSessionId() 
    {
        return sessionId;
    }

    public Long getCollegeDeptId()
    {
        return collegeDeptId;
    }

    public void setCollegeDeptId(Long collegeDeptId)
    {
        this.collegeDeptId = collegeDeptId;
    }

    public void setTalkType(String talkType) 
    {
        this.talkType = talkType;
    }

    public String getTalkType() 
    {
        return talkType;
    }

    public void setTalkTime(Date talkTime) 
    {
        this.talkTime = talkTime;
    }

    public Date getTalkTime() 
    {
        return talkTime;
    }

    public void setTalkLocation(String talkLocation) 
    {
        this.talkLocation = talkLocation;
    }

    public String getTalkLocation() 
    {
        return talkLocation;
    }

    public void setTalkPerson(String talkPerson) 
    {
        this.talkPerson = talkPerson;
    }

    public String getTalkPerson() 
    {
        return talkPerson;
    }

    public void setTalkContent(String talkContent) 
    {
        this.talkContent = talkContent;
    }

    public String getTalkContent() 
    {
        return talkContent;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("sessionId", getSessionId())
            .append("collegeDeptId", getCollegeDeptId())
            .append("talkType", getTalkType())
            .append("talkTime", getTalkTime())
            .append("talkLocation", getTalkLocation())
            .append("talkPerson", getTalkPerson())
            .append("talkContent", getTalkContent())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
