package com.ruoyi.talk.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.ruoyi.common.exception.ServiceException;

/** Bounded ZIP parsing; entries are never extracted to a filesystem path. */
public final class StudentPhotoArchive {
    private static final int FILE_LIMIT = 3 * 1024 * 1024;
    private static final int TOTAL_LIMIT = 100 * 1024 * 1024;
    private static final int COUNT_LIMIT = 2000;
    private StudentPhotoArchive() {}
    public record Photo(String className, String studentCode, String studentName, byte[] bytes, String error) {
        public Photo(String className, String studentCode, String studentName, byte[] bytes) { this(className, studentCode, studentName, bytes, ""); }
    }

    public static boolean matches(Photo photo, String code, String name, String className) {
        return photo.studentCode().equals(code) && photo.studentName().equals(name) && photo.className().equals(className);
    }

    public static List<Photo> read(byte[] archive) {
        if (archive == null || archive.length > 50 * 1024 * 1024) throw new ServiceException("压缩包超过50MB");
        try { return readWithCharset(archive, StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ex) { return readWithCharset(archive, Charset.forName("GBK")); }
    }

    private static List<Photo> readWithCharset(byte[] archive, Charset charset) {
        List<Photo> photos = new ArrayList<>();
        int count = 0, total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), charset)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > COUNT_LIMIT) throw new ServiceException("压缩包文件数超过2000");
                String name = entry.getName();
                String normalized = name.replace('\\', '/');
                if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*") ||
                        java.util.Arrays.stream(normalized.split("/", -1)).anyMatch(".."::equals))
                    throw new ServiceException("压缩包包含非法路径");
                if (entry.isDirectory()) continue;
                String base = normalized.substring(normalized.lastIndexOf('/') + 1);
                boolean jpeg = base.toLowerCase(java.util.Locale.ROOT).matches(".*\\.jpe?g");
                String[] parts = jpeg ? base.substring(0, base.lastIndexOf('.')).split("_", -1) : new String[0];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192]; int n; boolean oversized = false;
                while ((n = zip.read(buffer)) != -1) {
                    total += n;
                    if (total > TOTAL_LIMIT) throw new ServiceException("照片解压总量超限");
                    if (out.size() + n > FILE_LIMIT) oversized = true;
                    if (!oversized) out.write(buffer, 0, n);
                }
                byte[] bytes = out.toByteArray();
                if (!jpeg || parts.length != 3 || java.util.Arrays.stream(parts).anyMatch(String::isBlank)) {
                    photos.add(new Photo("", "", base, bytes, jpeg ? "照片命名应为班级_学号_姓名.jpg" : "仅允许JPEG照片"));
                } else {
                    String error = oversized ? "照片超过3MB" : "";
                    if (!oversized) try { validateJpeg(bytes); } catch (ServiceException ex) { error = ex.getMessage(); }
                    photos.add(new Photo(parts[0].trim(), parts[1].trim(), parts[2].trim(), bytes, error));
                }
            }
            if (photos.isEmpty()) throw new ServiceException("压缩包没有照片");
            return photos;
        } catch (java.io.IOException ex) { throw new ServiceException("照片压缩包损坏或无法读取"); }
    }
    private static void validateJpeg(byte[] bytes) {
        if (bytes.length < 4 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216)
            throw new ServiceException("照片不是有效JPEG");
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new ServiceException("照片无法读取");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 6000 || height > 6000 || (long)width * height > 16000000)
                    throw new ServiceException("照片尺寸超限");
                if (reader.read(0) == null) throw new ServiceException("照片无法解码");
            } finally { reader.dispose(); }
        } catch (java.io.IOException ex) { throw new ServiceException("照片无法解码"); }
    }
}
