package com.heimdall.backend.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem implementation of StorageService.
 */
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final String baseDir;

    public LocalStorageService(@Value("${heimdall.backup.dump-dir:./heimdall-data/dumps}") String baseDir) {
        this.baseDir = baseDir;
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public String uploadFile(String key, File file) {
        File targetFile = resolveFile(key);
        if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        try {
            // If the file is already at the target location, return key
            if (file.getCanonicalPath().equals(targetFile.getCanonicalPath())) {
                return key;
            }
            Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file locally at '{}' (key: '{}')", targetFile.getAbsolutePath(), key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally: " + key, e);
        }
    }

    @Override
    public File downloadToFile(String key, File destinationFile) {
        File sourceFile = resolveFile(key);
        if (!sourceFile.exists()) {
            throw new RuntimeException("Local source file does not exist: " + sourceFile.getAbsolutePath());
        }

        if (destinationFile.getParentFile() != null && !destinationFile.getParentFile().exists()) {
            destinationFile.getParentFile().mkdirs();
        }

        try {
            if (!sourceFile.getCanonicalPath().equals(destinationFile.getCanonicalPath())) {
                Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return destinationFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy local file from " + sourceFile + " to " + destinationFile, e);
        }
    }

    @Override
    public void deleteFile(String key) {
        File targetFile = resolveFile(key);
        try {
            Files.deleteIfExists(targetFile.toPath());
            log.info("Deleted local file: {}", targetFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to delete local file: {}", targetFile.getAbsolutePath(), e);
        }
    }

    @Override
    public boolean exists(String key) {
        return resolveFile(key).exists();
    }

    @Override
    public long getFileSize(String key) {
        File file = resolveFile(key);
        return file.exists() ? file.length() : 0L;
    }

    private File resolveFile(String key) {
        Path path = Paths.get(key);
        if (path.isAbsolute()) {
            return path.toFile();
        }
        return new File(baseDir, key);
    }
}
