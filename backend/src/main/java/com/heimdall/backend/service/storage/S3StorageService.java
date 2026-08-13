package com.heimdall.backend.service.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.net.URI;

/**
 * Storage service implementation for S3-compatible object stores
 * (AWS S3, SeaweedFS, MinIO, etc.).
 */
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final String endpoint;
    private final String region;
    private final String bucketName;
    private final String accessKey;
    private final String secretKey;
    private final boolean pathStyleAccess;
    private final boolean autoCreateBucket;

    private S3Client s3Client;

    public S3StorageService(
            @Value("${heimdall.storage.s3.endpoint:http://localhost:8333}") String endpoint,
            @Value("${heimdall.storage.s3.region:us-east-1}") String region,
            @Value("${heimdall.storage.s3.bucket-name:heimdall-backups}") String bucketName,
            @Value("${heimdall.storage.s3.access-key:dummy-access-key}") String accessKey,
            @Value("${heimdall.storage.s3.secret-key:dummy-secret-key}") String secretKey,
            @Value("${heimdall.storage.s3.path-style-access:true}") boolean pathStyleAccess,
            @Value("${heimdall.storage.s3.auto-create-bucket:true}") boolean autoCreateBucket) {
        this.endpoint = endpoint;
        this.region = region;
        this.bucketName = bucketName;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.pathStyleAccess = pathStyleAccess;
        this.autoCreateBucket = autoCreateBucket;
    }

    // For testing or custom injection
    public S3StorageService(S3Client s3Client, String bucketName) {
        this.endpoint = null;
        this.region = "us-east-1";
        this.bucketName = bucketName;
        this.accessKey = null;
        this.secretKey = null;
        this.pathStyleAccess = true;
        this.autoCreateBucket = false;
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() {
        if (this.s3Client == null) {
            S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(region != null && !region.isBlank() ? region : "us-east-1"));

            if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ));
            }

            if (endpoint != null && !endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }

            if (pathStyleAccess) {
                builder.forcePathStyle(true);
            }

            this.s3Client = builder.build();
            log.info("Initialized S3Client with endpoint='{}', region='{}', bucket='{}', pathStyle={}",
                    endpoint, region, bucketName, pathStyleAccess);
        }

        if (autoCreateBucket) {
            ensureBucketExists();
        }
    }

    public void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 Bucket '{}' exists and is ready.", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("S3 Bucket '{}' does not exist. Creating it...", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("S3 Bucket '{}' created successfully.", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("S3 Bucket '{}' not found (404). Creating it...", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("S3 Bucket '{}' created successfully.", bucketName);
            } else {
                log.warn("Failed to check/create S3 bucket '{}': {} (Status Code: {})", bucketName, e.getMessage(), e.statusCode());
            }
        } catch (Exception e) {
            log.warn("Could not verify or create S3 bucket '{}': {}", bucketName, e.getMessage());
        }
    }

    @Override
    public String uploadFile(String key, File file) {
        log.debug("Uploading file '{}' to S3 bucket '{}' with key '{}'", file.getAbsolutePath(), bucketName, key);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentLength(file.length())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromFile(file));
        log.info("Successfully uploaded '{}' ({} bytes) to S3: s3://{}/{}", file.getName(), file.length(), bucketName, key);
        return key;
    }

    @Override
    public File downloadToFile(String key, File destinationFile) {
        log.debug("Downloading S3 object s3://{}/{} to '{}'", bucketName, key, destinationFile.getAbsolutePath());
        if (destinationFile.getParentFile() != null && !destinationFile.getParentFile().exists()) {
            destinationFile.getParentFile().mkdirs();
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.getObject(getRequest, ResponseTransformer.toFile(destinationFile));
        log.info("Successfully downloaded s3://{}/{} to '{}' ({} bytes)", bucketName, key, destinationFile.getAbsolutePath(), destinationFile.length());
        return destinationFile;
    }

    @Override
    public void deleteFile(String key) {
        log.debug("Deleting S3 object s3://{}/{}", bucketName, key);
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.info("Successfully deleted S3 object s3://{}/{}", bucketName, key);
    }

    @Override
    public boolean exists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public long getFileSize(String key) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        HeadObjectResponse response = s3Client.headObject(headRequest);
        return response.contentLength() != null ? response.contentLength() : 0L;
    }

    public String getBucketName() {
        return bucketName;
    }

    public S3Client getS3Client() {
        return s3Client;
    }
}
