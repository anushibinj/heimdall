import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import AddDatabase from './pages/AddDatabase';
import EditDatabase from './pages/EditDatabase';
import SnapshotBrowser from './pages/SnapshotBrowser';
import Login from './pages/Login';
import { ProtectedRoute } from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import './index.css';

const AppContent = () => {
  const { user, logout } = useAuth();

  return (
    <>
      {user && (
        <header className="app-header">
          <div className="header-container">
            <Link to="/" className="app-logo">Heimdall</Link>
            <nav style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              {(user.role === 'ADMIN' || user.role === 'EDITOR') && (
                <Link to="/add" className="button primary">
                  + Add Database
                </Link>
              )}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#94a3b8' }}>
                {user.avatarUrl && <img src={user.avatarUrl} alt="avatar" style={{ width: '32px', height: '32px', borderRadius: '50%' }} />}
                <span>{user.name} ({user.role})</span>
                <button onClick={logout} style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', marginLeft: '0.5rem' }}>Logout</button>
              </div>
            </nav>
          </div>
        </header>
      )}
      
      <div className="container animate-fade-in">
        <main>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
            <Route path="/add" element={<ProtectedRoute allowedRoles={['ADMIN', 'EDITOR']}><AddDatabase /></ProtectedRoute>} />
            <Route path="/edit/:id" element={<ProtectedRoute allowedRoles={['ADMIN', 'EDITOR']}><EditDatabase /></ProtectedRoute>} />
            <Route path="/database/:id" element={<ProtectedRoute><SnapshotBrowser /></ProtectedRoute>} />
          </Routes>
        </main>
      </div>
    </>
  );
};

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;
