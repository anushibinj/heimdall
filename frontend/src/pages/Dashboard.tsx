import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { API_BASE_URL } from '../config';
import { useJobProgress } from '../hooks/useJobProgress';

export interface TargetDatabase {
  id: string;
  name: string;
  host: string;
  port: number;
  dbName: string;
  username: string;
  engine: string;
  cronSchedule: string;
}

export default function Dashboard() {
  const [databases, setDatabases] = useState<TargetDatabase[]>([]);
  const [loading, setLoading] = useState(true);
  const activeJobs = useJobProgress() as Record<string, import('../hooks/useJobProgress').JobProgress>;

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/databases`)
      .then(res => res.json())
      .then(data => {
        setDatabases(data);
        setLoading(false);
      })
      .catch(err => {
        console.error("Failed to fetch databases", err);
        setLoading(false);
      });
  }, []);

  const handleDelete = async (id: string) => {
    if (confirm("Are you sure you want to remove this database configuration?")) {
      await fetch(`${API_BASE_URL}/api/databases/${id}`, { method: 'DELETE' });
      setDatabases(databases.filter(db => db.id !== id));
    }
  };

  if (loading) return <div className="animate-fade-in" style={{ color: 'var(--color-text-secondary)' }}>Initializing Sentinel...</div>;

  return (
    <div className="animate-slide-up">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h2>Monitored Databases</h2>
      </div>
      
      {databases.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <h3 style={{ color: 'var(--color-text-secondary)', marginBottom: '1rem' }}>No databases are currently monitored.</h3>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: '2rem' }}>Add a database configuration to start capturing snapshots.</p>
          <Link to="/add" className="button primary">Add Configuration</Link>
        </div>
      ) : (
        <div className="db-grid">
          {databases.map((db, index) => {
            const activeJob = activeJobs[db.id];
            
            return (
            <div key={db.id} className="card db-card" style={{ animationDelay: `${index * 0.1}s` }}>
              <div className="db-card-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <h3 className="db-card-title">{db.name}</h3>
                  {activeJob && (
                    <span className="badge warning" style={{ animation: 'pulse 2s infinite' }}>
                      {activeJob.jobType === 'BACKUP' ? 'Backing up...' : 'Restoring...'}
                    </span>
                  )}
                </div>
                <div className="status-dot" title="Active" />
              </div>
              
              <div style={{ marginBottom: '1.5rem' }}>
                <div className="db-meta">
                  <strong>Target:</strong> <span>{db.dbName} <span style={{ color: 'var(--color-text-secondary)' }}>on</span> {db.host}:{db.port}</span>
                </div>
                <div className="db-meta">
                  <strong>Engine:</strong> <span className="badge skipped">{db.engine}</span>
                </div>
                <div className="db-meta">
                  <strong>Schedule:</strong> <span className="mono-text">{db.cronSchedule}</span>
                </div>
              </div>
              
              <div style={{ display: 'flex', gap: '0.75rem', borderTop: '1px solid var(--color-border)', paddingTop: '1rem' }}>
                <Link to={`/database/${db.id}`} className="button" style={{ flex: 1 }}>
                  View Snapshots
                </Link>
                <Link to={`/edit/${db.id}`} className="button" aria-label="Edit Database" title="Edit Database" style={{ padding: '0.5rem' }}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
                </Link>
                <button className="danger" onClick={() => handleDelete(db.id)} aria-label="Remove Database" title="Remove Database">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"></path><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"></path><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"></path></svg>
                </button>
              </div>
            </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
