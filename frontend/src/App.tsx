import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import AddDatabase from './pages/AddDatabase';
import SnapshotBrowser from './pages/SnapshotBrowser';
import './index.css';

function App() {
  return (
    <BrowserRouter>
      <div className="container">
        <header className="app-header">
          <Link to="/" className="app-logo">Heimdall</Link>
          <nav>
            <Link to="/add">
              <button className="primary">Add Database</button>
            </Link>
          </nav>
        </header>
        
        <main>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/add" element={<AddDatabase />} />
            <Route path="/database/:id" element={<SnapshotBrowser />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
