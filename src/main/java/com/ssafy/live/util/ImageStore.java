package com.ssafy.live.util;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
/** 사진은 재배포 폴더 밖에 저장하고 DB에는 파일명만 저장한다. */
public final class ImageStore {
    private ImageStore() {
    }
    private static Path root() {
        return Path
                .of(System.getProperty("upload.dir",
                        System.getenv().getOrDefault("UPLOAD_DIR",
                                Path.of(System.getProperty("user.home"), ".enjoytrip", "uploads").toString())))
                .toAbsolutePath().normalize();
    }
    public static Path file(long member, String name) {
        if (name == null || !name.matches("[0-9a-f-]{36}\\.(png|jpg)"))
            throw new IllegalArgumentException("사진 경로가 올바르지 않습니다.");
        return root().resolve(Long.toString(member)).resolve(name);
    }
    public static boolean exists(long member, String name) {
        return Files.isRegularFile(file(member, name));
    }
    public static String save(long member, byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
            throw new IllegalArgumentException("사진은 5MB 이하로 선택해주세요.");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
                throw new IllegalArgumentException("PNG 또는 JPEG 사진을 선택해주세요.");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("png", "jpeg", "jpg").contains(format))
                    throw new IllegalArgumentException("PNG 또는 JPEG 사진을 선택해주세요.");
                reader.setInput(input);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 16000000)
                    throw new IllegalArgumentException("사진은 1,600만 화소 이하로 선택해주세요.");
                BufferedImage image = reader.read(0);
                String extension = format.equals("png") ? "png" : "jpg";
                String name = UUID.randomUUID() + "." + extension;
                Path file = file(member, name);
                Files.createDirectories(file.getParent());
                if (!ImageIO.write(image, extension, file.toFile()))
                    throw new IOException("사진 인코더가 없습니다.");
                return name;
            } finally {
                reader.dispose();
            }
        }
    }
    public static void delete(long member, String name) {
        if (name == null || name.isBlank())
            return;
        try {
            Files.deleteIfExists(file(member, name));
        } catch (IOException e) {
            System.err.println("여행 기록 사진 파일 정리가 지연되었습니다.");
        }
    }
}
