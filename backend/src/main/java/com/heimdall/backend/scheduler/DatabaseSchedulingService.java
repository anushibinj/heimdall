package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.TargetDatabase;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.heimdall.backend.repository.TargetDatabaseRepository;
import java.util.List;

@Service
public class DatabaseSchedulingService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TargetDatabaseRepository repository;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleAllOnStartup() {
        List<TargetDatabase> databases = repository.findAll();
        for (TargetDatabase db : databases) {
            try {
                scheduleDatabaseBackup(db);
            } catch (SchedulerException e) {
                // Log and continue
                e.printStackTrace();
            }
        }
    }

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

    public void triggerDatabaseBackup(TargetDatabase database, boolean force) throws SchedulerException {
        JobKey jobKey = new JobKey("backupJob_" + database.getId(), "backups");
        if (!scheduler.checkExists(jobKey)) {
            scheduleDatabaseBackup(database);
        }
        
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("isForced", force);
        scheduler.triggerJob(jobKey, dataMap);
    }
}
