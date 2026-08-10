import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { API_BASE_URL } from '../config';
import CronPicker from '../components/CronPicker';

export default function EditDatabase() {
  const navigate = useNavigate();
  const { id } = useParams();
  
  const [formData, setFormData] = useState({
    name: '',
    host: '',
    port: 5432,
    dbName: '',
    username: '',
    password: '',
    engine: 'POSTGRES',
    cronSchedule: '0 0 2 * * ?'
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [testing, setTesting] = useState(false);
  const [testSuccess, setTestSuccess] = useState(false);

  useEffect(() => {
    fetch(`${API_BASE_URL}/api/databases/${id}`)
      .then(res => {
        if (!res.ok) throw new Error('Failed to load database config');
        return res.json();
      })
      .then(data => {
        setFormData({
          ...data,
          password: '' // Explicitly clear password so we don't accidentally send it back if it happens to be provided by the server
        });
        setLoading(false);
      })
      .catch(err => {
        setError(err.message);
        setLoading(false);
      });
  }, [id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleTestConnection = async () => {
    setTesting(true);
    setError('');
    setTestSuccess(false);
    
    // In edit mode, if password is empty, the backend will now use the existing password.
    try {
      const res = await fetch(`${API_BASE_URL}/api/databases/${id}/test`, {
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
      const res = await fetch(`${API_BASE_URL}/api/databases/${id}`, {
        method: 'PUT',
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

  if (loading) return <div className="animate-fade-in" style={{ color: 'var(--color-text-secondary)', textAlign: 'center', marginTop: '4rem' }}>Loading configuration...</div>;

  return (
    <div className="animate-slide-up" style={{ maxWidth: '640px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h2>Edit Database Configuration</h2>
        <p style={{ color: 'var(--color-text-secondary)' }}>Update target database configuration.</p>
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
              <label htmlFor="password">Password <span style={{ color: 'var(--color-text-secondary)', fontSize: '0.8rem', fontWeight: 'normal', marginLeft: '0.5rem' }}>(Leave blank to keep current)</span></label>
              <input id="password" type="password" name="password" value={formData.password} onChange={handleChange} placeholder="Leave blank to keep current" />
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
              Save & Update Schedule
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
