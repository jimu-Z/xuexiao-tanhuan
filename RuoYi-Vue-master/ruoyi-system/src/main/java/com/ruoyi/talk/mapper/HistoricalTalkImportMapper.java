package com.ruoyi.talk.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface HistoricalTalkImportMapper {
    @Select("SELECT COUNT(*) FROM talk_student_record r JOIN talk_session s ON s.session_id=r.session_id " +
            "WHERE r.student_id=#{studentId} AND s.talk_time >= #{day} AND s.talk_time < DATE_ADD(#{day}, INTERVAL 1 DAY)")
    int countSameStudentDay(@Param("studentId") Long studentId, @Param("day") String day);

    @Select({"<script>", "SELECT student_id FROM talk_student WHERE student_id IN",
            "<foreach collection='studentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "ORDER BY student_id FOR UPDATE", "</script>"})
    List<Long> lockStudents(@Param("studentIds") List<Long> studentIds);
}
