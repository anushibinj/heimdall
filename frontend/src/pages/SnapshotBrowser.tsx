import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiFetch } from '../utils/apiClient';
import { useJobProgressDetails, type JobProgress } from '../hooks/useJobProgress';
import { useAuth } from '../context/AuthContext';

interface Snapshot {
  id: string;
  createdAt: string;
  status: string;
  checksum: string;
  filePath: string;
}

interface JobFailure {
  title: string;
  reason: string;
  stacktrace?: string;
  snapshotId?: string;
}

export default function SnapshotBrowser() {
  const { user } = useAuth();
  const { id } = useParams();
  const [snapshots, setSnapshots] = useState<Snapshot[]>([]);
  const [loading, setLoading] = useState(true);
  const [restoring, setRestoring] = useState(false);
  const [message, setMessage] = useState('');
  const [jobFailure, setJobFailure] = useState<JobFailure | null>(null);
  
  const [logModalOpen, setLogModalOpen] = useState(false);
  const [selectedLog, setSelectedLog] = useState('');
  const [logLoading, setLogLoading] = useState(false);
  
  const { activeJob, lastEvent } = useJobProgressDetails(id);
  const prevActiveJobRef = useRef<JobProgress | null>(null);

  const fetchSnapshots = useCallback(async (): Promise<Snapshot[]> => {
    try {
      const res = await apiFetch(`/api/snapshots?databaseId=${id}`);
      if (res.ok) {
        const data: Snapshot[] = await res.json();
        setSnapshots(data);
        setLoading(false);
        return data;
      }
    } catch (err) {
      console.error('Failed to fetch snapshots', err);
    }
    setLoading(false);
    return [];
  }, [id]);

  useEffect(() => {
    fetchSnapshots();
  }, [fetchSnapshots]);

  useEffect(() => {
    // If a job was active and is no longer active, inspect result
    if (prevActiveJobRef.current && !activeJob) {
      const prevJob = prevActiveJobRef.current;

      const handleJobOutcome = async () => {
        const data = await fetchSnapshots();
        const latest = data && data.length > 0 ? data[0] : null;

        if (lastEvent && (lastEvent.status === 'FAILED' || lastEvent.status === 'TIMEOUT')) {
          let trace = '';
          if (latest && (latest.status === 'FAILED' || latest.status === 'TIMEOUT')) {
            try {
              const logRes = await apiFetch(`/api/snapshots/${latest.id}/log`);
              if (logRes.ok) trace = await logRes.text();
            } catch {
              // ignore
            }
          }
          const failureReason = lastEvent.message || 'Operation failed.';
          setMessage(`${prevJob.jobType === 'RESTORE' ? 'Restore' : 'Backup'} failed: ${failureReason}`);
          setJobFailure({
            title: `${prevJob.jobType === 'RESTORE' ? 'Restore' : 'Backup'} Failed`,
            reason: failureReason,
            stacktrace: trace || failureReason,
            snapshotId: latest?.id
          });
        } else if (latest && (latest.status === 'FAILED' || latest.status === 'TIMEOUT')) {
          let trace = '';
          try {
            const logRes = await apiFetch(`/api/snapshots/${latest.id}/log`);
            if (logRes.ok) trace = await logRes.text();
          } catch {
            // ignore
          }
          const firstLine = trace.split('\n')[0] || (latest.status === 'TIMEOUT' ? 'Operation timed out.' : 'Backup job failed.');
          setMessage(`Backup failed: ${firstLine}`);
          setJobFailure({
            title: 'Backup Failed',
            reason: firstLine,
            stacktrace: trace,
            snapshotId: latest.id
          });
        } else if (latest && latest.status === 'SKIPPED') {
          setJobFailure(null);
          setMessage('Backup skipped: Data checksum matches the latest snapshot.');
          setTimeout(() => setMessage(''), 5000);
        } else {
          setJobFailure(null);
          setMessage(`${prevJob.jobType === 'RESTORE' ? 'Restore' : 'Backup'} completed successfully!`);
          setTimeout(() => setMessage(''), 5000);
        }
      };

      handleJobOutcome();
    }
    prevActiveJobRef.current = activeJob;
  }, [activeJob, lastEvent, fetchSnapshots]);

  const handleRestore = async (snapId: string) => {
    if (!confirm("CRITICAL WARNING: This will forcefully restore the database and replace all current data. Proceed?")) return;
    
    setJobFailure(null);
    setRestoring(true);
    setMessage('Restoring... please wait. Do not close this page.');
    try {
      const res = await apiFetch(`/api/snapshots/${snapId}/restore`, { method: 'POST' });
      const data = await res.json();
      if (res.ok && data.success) {
        setMessage('Restore completed successfully!');
      } else {
        const errorMsg = data.message || 'Restore failed.';
        setMessage(`Restore failed: ${errorMsg}`);
        setJobFailure({
          title: 'Restore Failed',
          reason: errorMsg,
          snapshotId: snapId
        });
      }
    } catch {
      setMessage('Error during restore.');
      setJobFailure({
        title: 'Restore Failed',
        reason: 'Network or server error during restore request.'
      });
    } finally {
      setRestoring(false);
    }
  };

  const handleTakeSnapshot = async (force: boolean = false) => {
    setJobFailure(null);
    setMessage(`Triggering snapshot${force ? ' (forced)' : ''}... please wait.`);
    try {
      const res = await apiFetch(`/api/databases/${id}/snapshot?force=${force}`, { method: 'POST' });
      const data = await res.json();
      if (res.ok && data.success) {
        setMessage('Snapshot triggered successfully! Running in the background...');
      } else {
        const errMsg = data.message || 'Failed to trigger snapshot.';
        setMessage(`Failed to trigger snapshot: ${errMsg}`);
        setJobFailure({
          title: 'Snapshot Trigger Failed',
          reason: errMsg
        });
      }
    } catch {
      setMessage('Error triggering snapshot.');
      setJobFailure({
        title: 'Snapshot Trigger Failed',
        reason: 'Network or server communication error.'
      });
    }
  };

  const handleViewLog = async (snapId: string) => {
    setLogModalOpen(true);
    setLogLoading(true);
    setSelectedLog('');
    try {
      const res = await apiFetch(`/api/snapshots/${snapId}/log`);
      if (res.ok) {
        const text = await res.text();
        setSelectedLog(text);
      } else {
        setSelectedLog('Failed to load logs.');
      }
    } catch {
      setSelectedLog('Error loading logs.');
    } finally {
      setLogLoading(false);
    }
  };

  const isJobActive = !!activeJob;
  const activeMessage = activeJob ? (activeJob.jobType === 'BACKUP' ? 'Backing up... please wait.' : 'Restoring... please wait.') : null;

  if (loading) return <div className="animate-fade-in" style={{ color: 'var(--color-text-secondary)' }}>Loading snapshots...</div>;

  return (
    <div className="animate-slide-up" style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <Link to="/" style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', display: 'inline-flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.5rem' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6"/></svg>
            Back to Dashboard
          </Link>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <h2>Snapshots</h2>
            {isJobActive && (
              <span className="badge warning" style={{ animation: 'pulse 2s infinite' }}>
                {activeJob.jobType === 'BACKUP' ? 'Backing up...' : 'Restoring...'}
              </span>
            )}
          </div>
        </div>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          {(user?.role === 'ADMIN' || user?.role === 'EDITOR') && (
            <>
              <button onClick={() => handleTakeSnapshot(false)} className="primary" disabled={isJobActive || restoring}>Take Snapshot</button>
              <button onClick={() => handleTakeSnapshot(true)} className="danger" disabled={isJobActive || restoring}>Force Snapshot</button>
            </>
          )}
        </div>
      </div>

      {activeMessage ? (
        <div className="badge warning" style={{ marginBottom: '2rem', display: 'flex', padding: '0.75rem 1rem', animation: 'pulse 2s infinite' }}>
          {activeMessage}
        </div>
      ) : message && !jobFailure && (
        <div className={`badge ${message.toLowerCase().includes('fail') || message.toLowerCase().includes('error') ? 'error' : 'success'}`} style={{ marginBottom: '2rem', display: 'flex', padding: '0.75rem 1rem' }}>
          {message}
        </div>
      )}

      {jobFailure && (
        <div className="card animate-fade-in" style={{ border: '1px solid #ef4444', background: 'rgba(239, 68, 68, 0.08)', marginBottom: '2rem', padding: '1.25rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <span className="badge error">{jobFailure.title}</span>
                <strong style={{ color: '#ef4444', fontSize: '0.95rem' }}>{jobFailure.reason}</strong>
              </div>
              <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                The operation failed to complete. Error details and stacktrace are captured below:
              </p>
            </div>
            <button onClick={() => setJobFailure(null)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)', padding: '0.25rem' }} title="Dismiss">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
          </div>
          {jobFailure.stacktrace && (
            <div style={{ marginTop: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-secondary)' }}>Failure Reason & Stacktrace:</span>
                {jobFailure.snapshotId && (
                  <button className="button" onClick={() => handleViewLog(jobFailure.snapshotId!)} style={{ padding: '0.2rem 0.5rem', fontSize: '0.75rem' }}>
                    Open Full Log
                  </button>
                )}
              </div>
              <pre style={{ maxHeight: '220px', overflowY: 'auto', background: 'rgba(0, 0, 0, 0.5)', border: '1px solid rgba(239, 68, 68, 0.3)', padding: '0.75rem', borderRadius: '4px', margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all', fontSize: '0.8rem', color: '#fca5a5' }}>
                {jobFailure.stacktrace}
              </pre>
            </div>
          )}
        </div>
      )}

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {snapshots.length === 0 ? (
          <div style={{ padding: '3rem 2rem', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
            No snapshots available for this database yet.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ margin: 0 }}>
              <thead>
                <tr>
                  <th style={{ paddingLeft: '1.5rem' }}>Date Taken (UTC)</th>
                  <th>Status</th>
                  <th>Checksum</th>
                  <th style={{ paddingRight: '1.5rem', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {snapshots.map(snap => (
                  <tr key={snap.id}>
                    <td style={{ paddingLeft: '1.5rem' }}>
                      <span className="mono-text" style={{ background: 'transparent' }}>
                        {new Date(snap.createdAt).toLocaleString()}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${snap.status.toLowerCase()}`}>{snap.status}</span>
                    </td>
                    <td>
                      <code className="mono-text">{snap.checksum || 'N/A'}</code>
                    </td>
                    <td style={{ paddingRight: '1.5rem', textAlign: 'right', display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                      <button className="button" onClick={() => handleViewLog(snap.id)} style={{ padding: '0.25rem 0.75rem', fontSize: '0.875rem' }}>
                        View Log
                      </button>
                      {(user?.role === 'ADMIN' || user?.role === 'EDITOR') && snap.status === 'SUCCESS' && (
                        <button className="button" onClick={() => handleRestore(snap.id)} disabled={restoring || isJobActive} style={{ padding: '0.25rem 0.75rem', fontSize: '0.875rem' }}>
                          Revert
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {logModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.7)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(4px)' }}>
          <div className="card animate-slide-up" style={{ width: '80%', maxWidth: '900px', maxHeight: '80vh', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--color-border)', paddingBottom: '1rem', marginBottom: '1rem' }}>
              <h3 style={{ margin: 0 }}>Snapshot Log</h3>
              <button onClick={() => setLogModalOpen(false)} style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)', padding: '0.25rem' }}>
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
              </button>
            </div>
            {logLoading ? (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--color-text-secondary)' }}>Loading logs...</div>
            ) : (
              <pre style={{ flex: 1, overflowY: 'auto', background: 'var(--color-surface-hover)', padding: '1rem', borderRadius: '4px', margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all', fontSize: '0.875rem' }}>
                {selectedLog || 'No log output available.'}
              </pre>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
