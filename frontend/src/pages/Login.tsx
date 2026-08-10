import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';

const Login: React.FC = () => {
  const { user, loading, login } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: '#0f172a', color: '#fff' }}>
        Loading...
      </div>
    );
  }

  // If already logged in, redirect to dashboard
  if (user) {
    return <Navigate to="/" replace />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: '#0f172a', color: '#f8fafc' }}>
      <div style={{ padding: '2rem', backgroundColor: '#1e293b', borderRadius: '8px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)', textAlign: 'center' }}>
        <h1 style={{ marginBottom: '1.5rem', fontWeight: 600 }}>Heimdall</h1>
        <p style={{ marginBottom: '2rem', color: '#94a3b8' }}>Database Snapshot Management</p>
        <button
          onClick={login}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100%',
            padding: '0.75rem 1.5rem',
            backgroundColor: '#fff',
            color: '#1e293b',
            border: 'none',
            borderRadius: '4px',
            fontSize: '1rem',
            fontWeight: 500,
            cursor: 'pointer',
            transition: 'background-color 0.2s'
          }}
        >
          <img src="https://upload.wikimedia.org/wikipedia/commons/5/53/Google_%22G%22_Logo.svg" alt="Google logo" style={{ width: '1.25rem', height: '1.25rem', marginRight: '0.75rem' }} />
          Sign in with Google
        </button>
      </div>
    </div>
  );
};

export default Login;
