package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.TargetDatabase;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSchedulingService {

    @Autowired
    private Scheduler scheduler;

    public void scheduleDatabaseBackup(TargetDatabase database) throws SchedulerException {
        String jobKeyStr = "backupJob_" + database.getId();
        String triggerKeyStr = "backupTrigger_" + database.getId();

        JobKey jobKey = new JobKey(jobKeyStr, "backups");
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }

        JobDetail jobDetail = JobBuilder.newJob(BackupJob.class)
                .withIdentity(jobKey)
                .usingJobData("databaseId", database.getId().toString())
                .storeDurably()
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKeyStr, "backups")
                .withSchedule(CronScheduleBuilder.cronSchedule(database.getCronSchedule()))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
    
    public void unscheduleDatabaseBackup(TargetDatabase database) throws SchedulerException {
        JobKey jobKey = new JobKey("backupJob_" + database.getId(), "backups");
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    public void triggerDatabaseBackup(TargetDatabase database) throws SchedulerException {
        JobKey jobKey = new JobKey("backupJob_" + database.getId(), "backups");
        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey);
        } else {
            throw new SchedulerException("Job not found for database " + database.getId());
        }
    }
}
