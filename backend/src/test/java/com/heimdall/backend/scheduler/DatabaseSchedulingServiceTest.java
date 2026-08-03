package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.TargetDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatabaseSchedulingServiceTest {

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private DatabaseSchedulingService schedulingService;

    @Test
    void testScheduleDatabaseBackup() throws SchedulerException {
        TargetDatabase db = new TargetDatabase();
        db.setId(UUID.randomUUID());
        db.setCronSchedule("0 0 12 * * ?");
        
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        schedulingService.scheduleDatabaseBackup(db);

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
    
    @Test
    void testScheduleDatabaseBackup_JobExists() throws SchedulerException {
        TargetDatabase db = new TargetDatabase();
        db.setId(UUID.randomUUID());
        db.setCronSchedule("0 0 12 * * ?");
        
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        schedulingService.scheduleDatabaseBackup(db);

        verify(scheduler).deleteJob(any(JobKey.class));
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void testUnscheduleDatabaseBackup() throws SchedulerException {
        TargetDatabase db = new TargetDatabase();
        db.setId(UUID.randomUUID());
        
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        schedulingService.unscheduleDatabaseBackup(db);

        verify(scheduler).deleteJob(any(JobKey.class));
    }
}
