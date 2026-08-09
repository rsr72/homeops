import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import App from './App';
import { resetHomeOpsStore, seedHousehold, setVehicleValidationMode } from './test/handlers';

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  resetHomeOpsStore();
  window.history.pushState({}, '', '/');
});

describe('HomeOps web UI', () => {
  it('lets a user create a household, create a vehicle, edit it, delete it, and reload the household-scoped state', async () => {
    const user = userEvent.setup();
    renderApp();

    await screen.findByText('No households yet');
    await user.type(screen.getByLabelText(/household name/i), 'Alpha Household');
    await user.click(screen.getByRole('button', { name: /create household/i }));

    const householdButton = await screen.findByRole('button', { name: /alpha household/i });
    expect(householdButton).toBeInTheDocument();
    expect(window.location.search).toContain('householdId=');

    await screen.findAllByText('No vehicles yet');
    await user.click(screen.getAllByRole('button', { name: /add vehicle/i })[0]);

    await user.type(screen.getByLabelText(/^make$/i), 'Toyota');
    await user.type(screen.getByLabelText(/^model$/i), 'Camry');
    await user.type(screen.getByLabelText(/^year$/i), '2022');
    await user.click(screen.getByRole('button', { name: /create vehicle/i }));

    const vehicleCard = await screen.findByRole('article', { name: /toyota camry/i });
    expect(within(vehicleCard).getByText(/2022/)).toBeInTheDocument();

    await user.click(within(vehicleCard).getByRole('button', { name: /edit/i }));
    await user.clear(screen.getByLabelText(/^model$/i));
    await user.type(screen.getByLabelText(/^model$/i), 'Corolla');
    await user.click(screen.getByRole('button', { name: /save vehicle/i }));

    expect(await screen.findByText(/toyota corolla/i)).toBeInTheDocument();

    const updatedCard = await screen.findByRole('article', { name: /toyota corolla/i });
    await user.click(within(updatedCard).getByRole('button', { name: /delete/i }));
    await user.click(screen.getByRole('button', { name: /confirm delete/i }));

    await screen.findByText('No vehicles yet');
  });

  it('shows backend validation and server error states in a usable way', async () => {
    const user = userEvent.setup();
    seedHousehold('Beta Household');
    setVehicleValidationMode('rejectMake');
    renderApp();

    await screen.findByRole('button', { name: /beta household/i });
    await user.click(screen.getByRole('button', { name: /beta household/i }));
    await screen.findAllByText('No vehicles yet');

    await user.click(screen.getAllByRole('button', { name: /add vehicle/i })[0]);
    await user.type(screen.getByLabelText(/^make$/i), 'Validation failure');
    await user.type(screen.getByLabelText(/^model$/i), 'Civic');
    await user.type(screen.getByLabelText(/^year$/i), '2020');
    await user.click(screen.getByRole('button', { name: /create vehicle/i }));

    expect(await screen.findByText('make is required')).toBeInTheDocument();

    await user.clear(screen.getByLabelText(/^make$/i));
    await user.type(screen.getByLabelText(/^make$/i), 'Honda');
    await user.click(screen.getByRole('button', { name: /create vehicle/i }));

    expect(await screen.findByRole('article', { name: /honda civic/i })).toBeInTheDocument();
  });
});