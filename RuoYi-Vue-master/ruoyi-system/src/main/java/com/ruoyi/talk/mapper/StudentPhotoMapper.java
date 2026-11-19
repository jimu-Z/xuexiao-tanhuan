package com.ruoyi.talk.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface StudentPhotoMapper {
    @Select("select student_id from talk_student where student_id = #{studentId} for update")
    Long lockStudent(@Param("studentId") long studentId);
    @Select("select file_key from talk_student_photo where student_id = #{studentId}")
    String findKey(@Param("studentId") long studentId);
    @Insert("insert into talk_student_photo(student_id, file_key) values(#{studentId}, #{fileKey}) on duplicate key update file_key = values(file_key)")
    @Options(useGeneratedKeys = true, keyProperty = "photoId")
    int upsert(PhotoRow row);
    class PhotoRow {
        private Long photoId;
        private long studentId;
        private String fileKey;
        public PhotoRow(long studentId, String fileKey) { this.studentId = studentId; this.fileKey = fileKey; }
        public Long getPhotoId() { return photoId; }
        public void setPhotoId(Long photoId) { this.photoId = photoId; }
        public long getStudentId() { return studentId; }
        public String getFileKey() { return fileKey; }
    }
}
