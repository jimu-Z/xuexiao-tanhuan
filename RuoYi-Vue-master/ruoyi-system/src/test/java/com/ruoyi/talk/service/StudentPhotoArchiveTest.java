package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import com.ruoyi.talk.utils.StudentPhotoArchive;

class StudentPhotoArchiveTest {
    private byte[] jpeg() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "jpg", out);
        return out.toByteArray();
    }
    private byte[] zip(String name, byte[] bytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) { z.putNextEntry(new ZipEntry(name)); z.write(bytes); z.closeEntry(); }
        return out.toByteArray();
    }
    @Test void acceptsValidPhotoAndParsesIdentity() throws Exception {
        var photos = StudentPhotoArchive.read(zip("23法学1班/23法学1班_202300301001_白立彤.jpg", jpeg()));
        assertThat(photos).hasSize(1);
        assertThat(photos.get(0).studentCode()).isEqualTo("202300301001");
        assertThat(photos.get(0).studentName()).isEqualTo("白立彤");
    }
    @Test void rejectsTraversalAndPretendImages() throws Exception {
        assertThatThrownBy(() -> StudentPhotoArchive.read(zip("../a_1_b.jpg", jpeg()))).isInstanceOf(RuntimeException.class);
        assertThat(StudentPhotoArchive.read(zip("a_1_b.jpg", "wrong".getBytes())).get(0).error()).isNotBlank();
    }
    @Test void matchRequiresExactCodeNameAndClass() {
        var photo = new StudentPhotoArchive.Photo("23法学1班", "202300301001", "白立彤", new byte[0]);
        assertThat(StudentPhotoArchive.matches(photo, "202300301001", "白立彤", "23法学1班")).isTrue();
        assertThat(StudentPhotoArchive.matches(photo, "202300301001", "白立彤", "另一班")).isFalse();
        assertThat(StudentPhotoArchive.matches(photo, "202300301001", "其他人", "23法学1班")).isFalse();
    }
    @Test void readsGbkNamesFromWindowsZip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out, java.nio.charset.Charset.forName("GBK"))) {
            z.putNextEntry(new ZipEntry("班级/班级_100_小明.jpg")); z.write(jpeg()); z.closeEntry();
        }
        assertThat(StudentPhotoArchive.read(out.toByteArray()).get(0).studentName()).isEqualTo("小明");
    }
    @Test void oversizedPhotoFailsIndividuallyAndNextPhotoCanStillBeImported() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry("班级_100_小明.jpg")); z.write(new byte[3 * 1024 * 1024 + 1]); z.closeEntry();
            z.putNextEntry(new ZipEntry("班级_101_小红.jpg")); z.write(jpeg()); z.closeEntry();
        }
        var rows = StudentPhotoArchive.read(out.toByteArray());
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).error()).isNotBlank();
        assertThat(rows.get(1).error()).isEmpty();
    }
}
