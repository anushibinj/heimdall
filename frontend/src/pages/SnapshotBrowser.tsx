import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { API_BASE_URL } from '../config';

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

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/snapshots?databaseId=${id}`)
      .then(res => res.json())
      .then(data => {
        setSnapshots(data);
        setLoading(false);
      });
  }, [id]);

  const handleRestore = async (snapId: string) => {
    if (!confirm("WARNING: This will forcefully restore the database and replace all current data. Proceed?")) return;
    
    setRestoring(true);
    setMessage('Restoring... please wait.');
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

  const handleTakeSnapshot = async () => {
    setMessage('Triggering snapshot... please wait.');
    try {
      const res = await fetch(`${API_BASE_URL}/api/databases/${id}/snapshot`, { method: 'POST' });
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

  if (loading) return <div>Loading snapshots...</div>;

  return (
    <div className="card" style={{ maxWidth: 800, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Snapshots for DB {id}</h2>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
          <button onClick={handleTakeSnapshot} className="primary" style={{ padding: '0.5rem 1rem' }}>Take Snapshot Now</button>
          <Link to="/" className="button">Back to Dashboard</Link>
        </div>
      </div>

      {message && (
        <div className={`badge ${message.includes('failed') ? 'error' : 'success'}`} style={{ marginTop: '1rem', display: 'block' }}>
          {message}
        </div>
      )}

      {snapshots.length === 0 ? (
        <p style={{ marginTop: '1rem' }}>No snapshots available for this database.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Status</th>
              <th>Checksum</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {snapshots.map(snap => (
              <tr key={snap.id}>
                <td>{new Date(snap.createdAt).toLocaleString()}</td>
                <td>
                  <span className={`badge ${snap.status.toLowerCase()}`}>{snap.status}</span>
                </td>
                <td><code style={{ fontSize: '0.8rem' }}>{snap.checksum || 'N/A'}</code></td>
                <td>
                  {snap.status === 'SUCCESS' && (
                    <button className="primary" onClick={() => handleRestore(snap.id)} disabled={restoring}>
                      Revert to this
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
