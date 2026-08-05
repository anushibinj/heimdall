import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { API_BASE_URL } from '../config';

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
    cronSchedule: '0 0 2 * * ?'
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
      const res = await fetch(`${API_BASE_URL}/api/databases/test`, {
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
      const res = await fetch(`${API_BASE_URL}/api/databases`, {
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
    <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
      <h2>Add Database Configuration</h2>
      {error && <div className="badge error" style={{ marginBottom: '1rem', display: 'block' }}>{error}</div>}
      {testSuccess && <div className="badge success" style={{ marginBottom: '1rem', display: 'block' }}>Connection successful!</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Friendly Name</label>
          <input id="name" type="text" name="name" value={formData.name} onChange={handleChange} required placeholder="Production DB" />
        </div>
        
        <div style={{ display: 'flex', gap: '1rem' }}>
          <div className="form-group" style={{ flex: 2 }}>
            <label htmlFor="host">Host</label>
            <input id="host" type="text" name="host" value={formData.host} onChange={handleChange} required />
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

        <div style={{ display: 'flex', gap: '1rem' }}>
          <div className="form-group" style={{ flex: 1 }}>
            <label htmlFor="username">Username</label>
            <input id="username" type="text" name="username" value={formData.username} onChange={handleChange} required />
          </div>
          <div className="form-group" style={{ flex: 1 }}>
            <label htmlFor="password">Password</label>
            <input id="password" type="password" name="password" value={formData.password} onChange={handleChange} required />
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="cronSchedule">Cron Schedule</label>
          <input id="cronSchedule" type="text" name="cronSchedule" value={formData.cronSchedule} onChange={handleChange} required />
        </div>

        <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
          <button type="button" onClick={handleTestConnection} disabled={testing}>
            {testing ? 'Testing...' : 'Test Connection'}
          </button>
          <button type="submit" className="primary" disabled={testing}>Save & Schedule</button>
        </div>
      </form>
    </div>
  );
}
