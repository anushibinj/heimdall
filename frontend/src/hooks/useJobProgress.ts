import { useEffect, useState } from 'react';
import { API_BASE_URL } from '../config';
import { apiFetch } from '../utils/apiClient';

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
        const res = await apiFetch(`/api/events/active`);
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
      const token = localStorage.getItem('token');
      const tokenParam = token ? `?token=${encodeURIComponent(token)}` : '';
      sse = new EventSource(`${API_BASE_URL}/api/events/subscribe${tokenParam}`);

      sse.onmessage = () => {
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

export function useJobProgressDetails(databaseId?: string) {
  const [activeJob, setActiveJob] = useState<JobProgress | null>(null);
  const [lastEvent, setLastEvent] = useState<JobProgress | null>(null);

  useEffect(() => {
    if (!databaseId) return;

    let sse: EventSource | null = null;
    let fallbackInterval: ReturnType<typeof setInterval> | null = null;
    let usingFallback = false;

    const fetchActiveJobs = async () => {
      try {
        const res = await apiFetch(`/api/events/active`);
        if (res.ok) {
          const jobs: JobProgress[] = await res.json();
          const current = jobs.find(j => j.databaseId === databaseId);
          setActiveJob(current || null);
        }
      } catch (e) {
        console.error("Polling active jobs failed", e);
      }
    };

    const setupSSE = () => {
      const token = localStorage.getItem('token');
      const tokenParam = token ? `?token=${encodeURIComponent(token)}` : '';
      sse = new EventSource(`${API_BASE_URL}/api/events/subscribe${tokenParam}`);

      sse.onmessage = () => {
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
          if (job.databaseId === databaseId) {
            setLastEvent(job);
            if (job.status === 'IN_PROGRESS') {
              setActiveJob(job);
            } else {
              setActiveJob(null);
            }
          }
        } catch (e) {
          console.error("Failed to parse SSE event", e);
        }
      });

      sse.onerror = () => {
        console.warn("SSE connection error, falling back to polling");
        sse?.close();
        if (!usingFallback) {
          usingFallback = true;
          fetchActiveJobs();
          fallbackInterval = setInterval(fetchActiveJobs, 3000);
        }
      };
    };

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
  }, [databaseId]);

  return { activeJob, lastEvent };
}
