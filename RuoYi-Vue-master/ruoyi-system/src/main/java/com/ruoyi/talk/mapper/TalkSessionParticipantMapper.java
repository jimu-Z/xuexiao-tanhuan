package com.ruoyi.talk.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.talk.domain.TalkSessionParticipant;

public interface TalkSessionParticipantMapper {
    List<TalkSessionParticipant> selectBySessionId(@Param("sessionId") Long sessionId);
    int insertParticipant(TalkSessionParticipant participant);
    int deleteBySessionId(@Param("sessionId") Long sessionId);
    int countUser(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
    String selectActiveTeacherName(@Param("collegeId") Long collegeId, @Param("userId") Long userId);
    List<TalkSessionParticipant> selectActiveTeachers(@Param("collegeId") Long collegeId);
}
