package com.heimdall.backend.service.storage;

import java.io.File;

/**
 * Common interface for snapshot backup storage implementations (e.g. S3, Local Filesystem).
 */
public interface StorageService {

    /**
     * Uploads a local file to storage under the specified key.
     *
     * @param key Storage key or relative path
     * @param file Local file to upload
     * @return The stored key/path
     */
    String uploadFile(String key, File file);

    /**
     * Downloads an object from storage to the specified local file destination.
     *
     * @param key Storage key or relative path
     * @param destinationFile Local destination file
     * @return The local destination file
     */
    File downloadToFile(String key, File destinationFile);

    /**
     * Deletes an object from storage.
     *
     * @param key Storage key or relative path
     */
    void deleteFile(String key);

    /**
     * Checks if an object exists in storage.
     *
     * @param key Storage key or relative path
     * @return true if exists, false otherwise
     */
    boolean exists(String key);

    /**
     * Retrieves the file size in bytes for a given key in storage.
     *
     * @param key Storage key or relative path
     * @return size in bytes
     */
    long getFileSize(String key);
}
