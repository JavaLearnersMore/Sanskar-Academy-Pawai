package com.sanskaracademy.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Saves uploaded files (homework, study material) under  app.upload-dir/files/ .
 * The file type is checked from the first bytes of the file, not from its name.
 * Files are always sent back as downloads, never shown by the browser itself.
 */
@Service
public class FileStoreService {

    public static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Set<String> OFFICE = Set.of("docx", "xlsx", "pptx");

    private final Path root;

    public FileStoreService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("files");
    }

    public static class Stored {
        private final String path, name;
        Stored(String path, String name) {
            this.path = path;
            this.name = name;
        }
        /** relative path inside the store, saved in the database */
        public String getPath() { return path; }
        /** cleaned original file name, for display */
        public String getName() { return name; }
    }

    /** returns null when no file was chosen */
    public Stored save(MultipartFile f, String folder) throws IOException {
        if (f == null || f.isEmpty()) return null;
        if (f.getSize() > MAX_BYTES) throw new IllegalArgumentException("The file must be 8 MB or smaller");
        byte[] head;
        try (InputStream in = f.getInputStream()) {
            head = in.readNBytes(12);
        }
        String original = f.getOriginalFilename() == null ? "file" : f.getOriginalFilename();
        String ext;
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) ext = "jpg";
        else if (head.length >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') ext = "png";
        else if (head.length >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F') ext = "pdf";
        else if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F' && head[8] == 'W' && head[9] == 'E') ext = "webp";
        else if (head.length >= 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4) {
            String low = original.toLowerCase(Locale.ROOT);
            String e = low.contains(".") ? low.substring(low.lastIndexOf('.') + 1) : "";
            if (!OFFICE.contains(e)) throw new IllegalArgumentException("Allowed files: PDF, JPG, PNG, WEBP, DOCX, XLSX, PPTX");
            ext = e;
        } else {
            throw new IllegalArgumentException("Allowed files: PDF, JPG, PNG, WEBP, DOCX, XLSX, PPTX");
        }
        String safeFolder = folder.replaceAll("[^A-Za-z0-9_-]", "");
        Path dir = root.resolve(safeFolder).normalize();
        if (!dir.startsWith(root)) throw new IllegalArgumentException("Bad path");
        Files.createDirectories(dir);
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try (InputStream in = f.getInputStream()) {
            Files.copy(in, dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        }
        String clean = original.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (clean.length() > 140) clean = clean.substring(clean.length() - 140);
        return new Stored(safeFolder + "/" + stored, clean);
    }

    public Resource load(String relative) {
        if (relative == null || relative.isBlank()) throw new IllegalArgumentException("File not found");
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) throw new IllegalArgumentException("File not found");
        return new PathResource(p);
    }

    public void delete(String relative) {
        if (relative == null || relative.isBlank()) return;
        try {
            Path p = root.resolve(relative).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
