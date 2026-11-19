package com.ruoyi.talk.domain;

/** A system account or named external person participating in one session. */
public class TalkSessionParticipant {
    private Long participantId;
    private Long sessionId;
    private Long userId;
    private String displayName;
    private String customIdentity;
    private Integer positionIndex;

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getCustomIdentity() { return customIdentity; }
    public void setCustomIdentity(String customIdentity) { this.customIdentity = customIdentity; }
    public Integer getPositionIndex() { return positionIndex; }
    public void setPositionIndex(Integer positionIndex) { this.positionIndex = positionIndex; }
}
