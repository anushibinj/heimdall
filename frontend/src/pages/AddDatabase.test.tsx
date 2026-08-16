import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import userEvent from '@testing-library/user-event';
import AddDatabase from './AddDatabase';

jest.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: { role: 'ADMIN', name: 'Admin' } })
}));

globalThis.fetch = jest.fn();

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

describe('AddDatabase', () => {
  beforeEach(() => {
    (globalThis.fetch as jest.Mock).mockClear();
    mockNavigate.mockClear();
  });

  it('renders form elements', () => {
    render(<BrowserRouter><AddDatabase /></BrowserRouter>);
    expect(screen.getByLabelText(/friendly name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/host/i)).toBeInTheDocument();
    expect(screen.getByText(/test connection/i)).toBeInTheDocument();
  });

  it('links back to the dashboard', () => {
    render(<BrowserRouter><AddDatabase /></BrowserRouter>);
    const backLink = screen.getByRole('link', { name: /back to dashboard/i });
    expect(backLink).toHaveAttribute('href', '/');
  });

  it('handles connection test', async () => {
    (globalThis.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ success: true })
    });

    render(<BrowserRouter><AddDatabase /></BrowserRouter>);
    const user = userEvent.setup();

    await user.click(screen.getByText(/test connection/i));

    await waitFor(() => {
      expect(screen.getByText(/connection successful/i)).toBeInTheDocument();
    });
    expect(globalThis.fetch).toHaveBeenCalledWith('http://localhost:8080/api/databases/test', expect.any(Object));
  });

  it('submits form successfully', async () => {
    (globalThis.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true
    });

    render(<BrowserRouter><AddDatabase /></BrowserRouter>);
    const user = userEvent.setup();

    await user.type(screen.getByLabelText(/friendly name/i), 'New DB');
    await user.type(screen.getByLabelText(/database name/i), 'postgres_db');
    await user.type(screen.getByLabelText(/password/i), 'mysecret');
    await user.click(screen.getByText(/save & schedule/i));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });
});
