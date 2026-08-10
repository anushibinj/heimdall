import { useEffect, useState } from 'react';
import { API_BASE_URL } from '../config';

export interface JobProgress {
  databaseId: string;
  jobType: string;
  status: string;
  message: string;
}

export function useJobProgress(databaseId?: string) {
  const [activeJobs, setActiveJobs] = useState<Record<string, JobProgress>>({});

  useEffect(() => {
    let sse: EventSource | null = null;
    let fallbackInterval: ReturnType<typeof setInterval> | null = null;
    let usingFallback = false;

    const fetchActiveJobs = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/events/active`);
        if (res.ok) {
          const jobs: JobProgress[] = await res.json();
          const jobMap: Record<string, JobProgress> = {};
          jobs.forEach(job => {
            jobMap[job.databaseId] = job;
          });
          setActiveJobs(jobMap);
        }
      } catch (e) {
        console.error("Polling active jobs failed", e);
      }
    };

    const setupSSE = () => {
      sse = new EventSource(`${API_BASE_URL}/api/events/subscribe`);

      sse.onmessage = (event) => {
        // Fallback is no longer needed if we receive a message
        if (usingFallback) {
          usingFallback = false;
          if (fallbackInterval) {
            clearInterval(fallbackInterval);
            fallbackInterval = null;
          }
        }
      };

      sse.addEventListener('jobProgress', (event) => {
        try {
          const job: JobProgress = JSON.parse(event.data);
          setActiveJobs(prev => {
            const next = { ...prev };
            if (job.status === 'IN_PROGRESS') {
              next[job.databaseId] = job;
            } else {
              delete next[job.databaseId];
            }
            return next;
          });
        } catch (e) {
          console.error("Failed to parse SSE event", e);
        }
      });

      sse.onerror = () => {
        console.warn("SSE connection error, falling back to polling");
        sse?.close();
        if (!usingFallback) {
          usingFallback = true;
          fetchActiveJobs(); // Fetch immediately
          fallbackInterval = setInterval(fetchActiveJobs, 3000);
        }
      };
    };

    // Initial setup
    fetchActiveJobs().then(() => {
      setupSSE();
    });

    return () => {
      if (sse) {
        sse.close();
      }
      if (fallbackInterval) {
        clearInterval(fallbackInterval);
      }
    };
  }, []);

  if (databaseId) {
    return activeJobs[databaseId] || null;
  }

  return activeJobs;
}
