package com.heimdall.backend.dto;

import java.util.UUID;

public class JobProgressEvent {
    private UUID databaseId;
    private String jobType;
    private String status;
    private String message;

    public JobProgressEvent() {
    }

    public JobProgressEvent(UUID databaseId, String jobType, String status, String message) {
        this.databaseId = databaseId;
        this.jobType = jobType;
        this.status = status;
        this.message = message;
    }

    public UUID getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(UUID databaseId) {
        this.databaseId = databaseId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
