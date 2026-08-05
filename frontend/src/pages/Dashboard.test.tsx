import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import Dashboard from './Dashboard';

globalThis.fetch = jest.fn();

describe('Dashboard', () => {
  beforeEach(() => {
    (globalThis.fetch as jest.Mock).mockClear();
    window.confirm = jest.fn(() => true);
  });

  it('renders loading state initially', () => {
    (globalThis.fetch as jest.Mock).mockImplementationOnce(() => new Promise(() => {}));
    render(<BrowserRouter><Dashboard /></BrowserRouter>);
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('renders list of databases', async () => {
    const mockDbs = [
      { id: '1', name: 'Test DB 1', host: 'localhost', port: 5432, dbName: 'test1', cronSchedule: '0 0 * * *' }
    ];
    (globalThis.fetch as jest.Mock).mockResolvedValueOnce({
      json: async () => mockDbs
    });

    render(<BrowserRouter><Dashboard /></BrowserRouter>);
    
    await waitFor(() => {
      expect(screen.getByText('Test DB 1')).toBeInTheDocument();
    });
    
    expect(screen.getByText(/localhost:5432/)).toBeInTheDocument();
  });

  it('deletes database on remove click', async () => {
    const mockDbs = [
      { id: '1', name: 'Test DB 1', host: 'localhost', port: 5432, dbName: 'test1', cronSchedule: '0 0 * * *' }
    ];
    (globalThis.fetch as jest.Mock)
      .mockResolvedValueOnce({ json: async () => mockDbs }) // initial fetch
      .mockResolvedValueOnce({ ok: true }); // delete request

    render(<BrowserRouter><Dashboard /></BrowserRouter>);
    
    await waitFor(() => {
      expect(screen.getByText('Test DB 1')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByText(/remove/i));

    expect(globalThis.fetch).toHaveBeenCalledWith('http://localhost:8080/api/databases/1', { method: 'DELETE' });
    
    await waitFor(() => {
      expect(screen.queryByText('Test DB 1')).not.toBeInTheDocument();
    });
  });
});
