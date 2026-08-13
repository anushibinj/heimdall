package com.heimdall.backend.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class S3StorageServiceTest {

    private S3Client s3Client;
    private S3StorageService s3StorageService;
    private final String bucketName = "test-bucket";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        s3StorageService = new S3StorageService(s3Client, bucketName);
    }

    @Test
    void testUploadFile() throws Exception {
        File file = tempDir.resolve("sample.txt").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Hello S3 Storage");
        }

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = "backups/sample.txt";
        String resultKey = s3StorageService.uploadFile(key, file);

        assertThat(resultKey).isEqualTo(key);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(bucketName);
        assertThat(capturedRequest.key()).isEqualTo(key);
        assertThat(capturedRequest.contentLength()).isEqualTo(file.length());
    }

    @Test
    void testDownloadToFile() {
        File destinationFile = tempDir.resolve("downloaded.txt").toFile();
        String key = "backups/sample.txt";

        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenReturn(GetObjectResponse.builder().build());

        File result = s3StorageService.downloadToFile(key, destinationFile);

        assertThat(result).isEqualTo(destinationFile);

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(requestCaptor.capture(), any(ResponseTransformer.class));

        GetObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(bucketName);
        assertThat(capturedRequest.key()).isEqualTo(key);
    }

    @Test
    void testDeleteFile() {
        String key = "backups/sample.txt";

        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        s3StorageService.deleteFile(key);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo(bucketName);
        assertThat(capturedRequest.key()).isEqualTo(key);
    }

    @Test
    void testExistsTrue() {
        String key = "backups/sample.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertThat(s3StorageService.exists(key)).isTrue();
    }

    @Test
    void testExistsFalseOnNoSuchKey() {
        String key = "backups/sample.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        assertThat(s3StorageService.exists(key)).isFalse();
    }

    @Test
    void testGetFileSize() {
        String key = "backups/sample.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(2048L).build());

        assertThat(s3StorageService.getFileSize(key)).isEqualTo(2048L);
    }

    @Test
    void testEnsureBucketExistsWhenBucketMissing() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("Bucket not found").build());
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());

        s3StorageService.ensureBucketExists();

        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }
}
