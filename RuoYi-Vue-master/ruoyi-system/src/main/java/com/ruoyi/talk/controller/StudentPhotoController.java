package com.ruoyi.talk.controller;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.talk.service.impl.StudentPhotoService;
import com.ruoyi.talk.utils.StudentPhotoArchive;

@RestController
@RequestMapping("/ruoyi-system/student-photo")
public class StudentPhotoController {
    private final StudentPhotoService service;
    public StudentPhotoController(StudentPhotoService service) { this.service = service; }
    private byte[] archive(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 50L * 1024 * 1024) throw new ServiceException("请选择不超过50MB的ZIP文件");
        return file.getBytes();
    }
    @PreAuthorize("@ss.hasPermi('talk:student:import')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestParam("file") MultipartFile file) throws IOException {
        return AjaxResult.success(service.preview(StudentPhotoArchive.read(archive(file))));
    }
    @PreAuthorize("@ss.hasPermi('talk:student:import')")
    @Log(title = "学生照片导入", businessType = BusinessType.IMPORT, isSaveResponseData = false)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "false") boolean replace) throws IOException {
        return AjaxResult.success(service.execute(StudentPhotoArchive.read(archive(file)), replace));
    }
    @PreAuthorize("@ss.hasPermi('talk:student:list')")
    @GetMapping("/{studentId}")
    public void read(@PathVariable long studentId, HttpServletResponse response) throws IOException {
        byte[] bytes = service.read(studentId);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (bytes == null) { response.setStatus(HttpServletResponse.SC_NOT_FOUND); return; }
        response.setContentType("image/jpeg");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
