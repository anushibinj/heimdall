import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { API_BASE_URL } from '../config';

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

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <h2>Target Databases</h2>
      {databases.length === 0 ? (
        <p>No databases configured. Add one to get started.</p>
      ) : (
        <div className="db-list">
          {databases.map(db => (
            <div key={db.id} className="card">
              <h3>{db.name}</h3>
              <p>Host: {db.host}:{db.port}</p>
              <p>Database: {db.dbName}</p>
              <p>Schedule: {db.cronSchedule}</p>
              <div style={{ marginTop: '1rem', display: 'flex', gap: '1rem' }}>
                <Link to={`/database/${db.id}`}>
                  <button>View Snapshots</button>
                </Link>
                <button className="danger" onClick={() => handleDelete(db.id)}>Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
