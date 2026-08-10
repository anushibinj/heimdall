import { useEffect, useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { API_BASE_URL } from '../config';
import { useJobProgress, type JobProgress } from '../hooks/useJobProgress';

interface Snapshot {
  id: string;
  createdAt: string;
  status: string;
  checksum: string;
  filePath: string;
}

export default function SnapshotBrowser() {
  const { id } = useParams();
  const [snapshots, setSnapshots] = useState<Snapshot[]>([]);
  const [loading, setLoading] = useState(true);
  const [restoring, setRestoring] = useState(false);
  const [message, setMessage] = useState('');
  
  const activeJob = useJobProgress(id) as JobProgress | null;
  const prevActiveJobRef = useRef<JobProgress | null>(null);

  const fetchSnapshots = () => {
    fetch(`${API_BASE_URL}/api/snapshots?databaseId=${id}`)
      .then(res => res.json())
      .then(data => {
        setSnapshots(data);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchSnapshots();
  }, [id]);

  useEffect(() => {
    // If a job was active and is no longer active, refresh the list
    if (prevActiveJobRef.current && !activeJob) {
       fetchSnapshots();
       setMessage(prevActiveJobRef.current.jobType === 'BACKUP' ? 'Backup completed.' : 'Restore completed.');
       setTimeout(() => setMessage(''), 5000);
    }
    prevActiveJobRef.current = activeJob;
  }, [activeJob]);

  const handleRestore = async (snapId: string) => {
    if (!confirm("CRITICAL WARNING: This will forcefully restore the database and replace all current data. Proceed?")) return;
    
    setRestoring(true);
    setMessage('Restoring... please wait. Do not close this page.');
    try {
      const res = await fetch(`${API_BASE_URL}/api/snapshots/${snapId}/restore`, { method: 'POST' });
      const data = await res.json();
      if (res.ok && data.success) {
        setMessage('Restore completed successfully!');
      } else {
        setMessage('Restore failed.');
      }
    } catch (e) {
      setMessage('Error during restore.');
    } finally {
      setRestoring(false);
    }
  };

  const handleTakeSnapshot = async (force: boolean = false) => {
    setMessage(`Triggering snapshot${force ? ' (forced)' : ''}... please wait.`);
    try {
      const res = await fetch(`${API_BASE_URL}/api/databases/${id}/snapshot?force=${force}`, { method: 'POST' });
      const data = await res.json();
      if (res.ok && data.success) {
        setMessage('Snapshot triggered successfully! It will run in the background.');
      } else {
        setMessage('Failed to trigger snapshot.');
      }
    } catch (e) {
      setMessage('Error triggering snapshot.');
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
          <button onClick={() => handleTakeSnapshot(false)} className="primary" disabled={isJobActive || restoring}>Take Snapshot</button>
          <button onClick={() => handleTakeSnapshot(true)} className="danger" disabled={isJobActive || restoring}>Force Snapshot</button>
        </div>
      </div>

      {activeMessage ? (
        <div className="badge warning" style={{ marginBottom: '2rem', display: 'flex', padding: '0.75rem 1rem', animation: 'pulse 2s infinite' }}>
          {activeMessage}
        </div>
      ) : message && (
        <div className={`badge ${message.toLowerCase().includes('fail') || message.toLowerCase().includes('error') ? 'error' : 'success'}`} style={{ marginBottom: '2rem', display: 'flex', padding: '0.75rem 1rem' }}>
          {message}
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
                    <td style={{ paddingRight: '1.5rem', textAlign: 'right' }}>
                      {snap.status === 'SUCCESS' && (
                        <button className="button" onClick={() => handleRestore(snap.id)} disabled={restoring || isJobActive}>
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
    </div>
  );
}
