package com.heimdall.backend.config;

import com.heimdall.backend.service.storage.LocalStorageService;
import com.heimdall.backend.service.storage.S3StorageService;
import com.heimdall.backend.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "heimdall.storage.type", havingValue = "s3", matchIfMissing = true)
    public StorageService s3StorageService(
            @Value("${heimdall.storage.s3.endpoint:http://localhost:8333}") String endpoint,
            @Value("${heimdall.storage.s3.region:us-east-1}") String region,
            @Value("${heimdall.storage.s3.bucket-name:heimdall-backups}") String bucketName,
            @Value("${heimdall.storage.s3.access-key:}") String accessKey,
            @Value("${heimdall.storage.s3.secret-key:}") String secretKey,
            @Value("${heimdall.storage.s3.path-style-access:true}") boolean pathStyleAccess,
            @Value("${heimdall.storage.s3.auto-create-bucket:true}") boolean autoCreateBucket) {
        return new S3StorageService(endpoint, region, bucketName, accessKey, secretKey, pathStyleAccess, autoCreateBucket);
    }

    @Bean
    @ConditionalOnProperty(name = "heimdall.storage.type", havingValue = "local")
    public StorageService localStorageService(
            @Value("${heimdall.backup.dump-dir:./heimdall-data/dumps}") String dumpDir) {
        return new LocalStorageService(dumpDir);
    }
}
