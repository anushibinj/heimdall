import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import AddDatabase from './pages/AddDatabase';
import EditDatabase from './pages/EditDatabase';
import SnapshotBrowser from './pages/SnapshotBrowser';
import './index.css';

function App() {
  return (
    <BrowserRouter>
      <header className="app-header">
        <div className="header-container">
          <Link to="/" className="app-logo">Heimdall</Link>
          <nav>
            <Link to="/add" className="button primary">
              + Add Database
            </Link>
          </nav>
        </div>
      </header>
      
      <div className="container animate-fade-in">
        <main>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/add" element={<AddDatabase />} />
            <Route path="/edit/:id" element={<EditDatabase />} />
            <Route path="/database/:id" element={<SnapshotBrowser />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
