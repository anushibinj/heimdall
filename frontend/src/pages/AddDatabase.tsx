import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiFetch } from '../utils/apiClient';
import CronPicker from '../components/CronPicker';

export default function AddDatabase() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    host: 'localhost',
    port: 5432,
    dbName: '',
    username: 'postgres',
    password: '',
    engine: 'POSTGRES',
    cronSchedule: '0 2 * * *'
  });
  const [error, setError] = useState('');
  const [testing, setTesting] = useState(false);
  const [testSuccess, setTestSuccess] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleTestConnection = async () => {
    setTesting(true);
    setError('');
    setTestSuccess(false);
    try {
      const res = await apiFetch(`/api/databases/test`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });
      const data = await res.json();
      if (res.ok && data.success) {
        setTestSuccess(true);
      } else {
        setError(data.message || 'Connection failed.');
      }
    } catch (err: any) {
      setError(err.message || 'Connection failed.');
    } finally {
      setTesting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await apiFetch(`/api/databases`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });
      if (res.ok) {
        navigate('/');
      } else {
        const text = await res.text();
        setError(text);
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="animate-slide-up" style={{ maxWidth: '640px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <Link to="/" style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', display: 'inline-flex', alignItems: 'center', gap: '0.25rem', marginBottom: '0.5rem' }}>
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6"/></svg>
          Back to Dashboard
        </Link>
        <h2>Configure Database</h2>
        <p style={{ color: 'var(--color-text-secondary)' }}>Add a new target database for Heimdall to monitor and protect.</p>
      </div>

      {error && <div className="badge error" style={{ marginBottom: '1.5rem', display: 'flex', padding: '0.75rem 1rem' }}>{error}</div>}
      {testSuccess && <div className="badge success" style={{ marginBottom: '1.5rem', display: 'flex', padding: '0.75rem 1rem' }}>Connection successful! Heimdall can see the database.</div>}
      
      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Friendly Name</label>
            <input id="name" type="text" name="name" value={formData.name} onChange={handleChange} required placeholder="e.g. Production Main" />
          </div>
          
          <div style={{ display: 'flex', gap: '1.5rem' }}>
            <div className="form-group" style={{ flex: 2 }}>
              <label htmlFor="host">Host</label>
              <input id="host" type="text" name="host" value={formData.host} onChange={handleChange} required placeholder="db.internal.example.com" />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label htmlFor="port">Port</label>
              <input id="port" type="number" name="port" value={formData.port} onChange={handleChange} required />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="dbName">Database Name</label>
            <input id="dbName" type="text" name="dbName" value={formData.dbName} onChange={handleChange} required />
          </div>

          <div style={{ display: 'flex', gap: '1.5rem' }}>
            <div className="form-group" style={{ flex: 1 }}>
              <label htmlFor="username">Username</label>
              <input id="username" type="text" name="username" value={formData.username} onChange={handleChange} required />
            </div>
            <div className="form-group" style={{ flex: 1 }}>
              <label htmlFor="password">Password</label>
              <input id="password" type="password" name="password" value={formData.password} onChange={handleChange} required />
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '0' }}>
            <label htmlFor="cronSchedule">Backup Frequency (UTC)</label>
            <CronPicker 
              value={formData.cronSchedule} 
              onChange={(val) => setFormData({ ...formData, cronSchedule: val })} 
            />
          </div>

          <div style={{ 
            display: 'flex', 
            gap: '1rem', 
            marginTop: '2.5rem', 
            paddingTop: '1.5rem', 
            borderTop: '1px solid var(--color-border)',
            justifyContent: 'flex-end'
          }}>
            <button type="button" onClick={handleTestConnection} disabled={testing}>
              {testing ? 'Testing Connection...' : 'Test Connection'}
            </button>
            <button type="submit" className="primary" disabled={testing}>
              Save & Schedule
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
