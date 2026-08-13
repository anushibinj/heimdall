package com.heimdall.backend.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService localStorageService;

    @BeforeEach
    void setUp() {
        localStorageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    void testUploadDownloadDelete() throws Exception {
        File sourceFile = tempDir.resolve("test_source.txt").toFile();
        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write("Local storage test content");
        }

        String key = "subfolder/test_stored.txt";
        String returnedKey = localStorageService.uploadFile(key, sourceFile);
        assertThat(returnedKey).isEqualTo(key);
        assertThat(localStorageService.exists(key)).isTrue();
        assertThat(localStorageService.getFileSize(key)).isEqualTo(sourceFile.length());

        File destinationFile = tempDir.resolve("downloaded/test_downloaded.txt").toFile();
        File downloaded = localStorageService.downloadToFile(key, destinationFile);
        assertThat(downloaded.exists()).isTrue();
        assertThat(Files.readString(downloaded.toPath())).isEqualTo("Local storage test content");

        localStorageService.deleteFile(key);
        assertThat(localStorageService.exists(key)).isFalse();
    }
}
