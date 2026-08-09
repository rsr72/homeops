import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ErrorBanner } from '../components/ErrorBanner';
import { HouseholdPanel } from '../features/households/HouseholdPanel';
import { VehiclePanel } from '../features/vehicles/VehiclePanel';
import { Shell } from '../components/Shell';
import { Panel } from '../components/Panel';
import { useHouseholdsQuery } from '../api/queries';
import type { HouseholdResponse } from '../types';

export function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedHouseholdId = searchParams.get('householdId') ?? undefined;
  const householdsQuery = useHouseholdsQuery();

  const households = householdsQuery.data ?? [];
  const selectedHousehold = households.find((household) => household.id === selectedHouseholdId);

  useEffect(() => {
    if (!householdsQuery.isSuccess || households.length === 0) {
      return;
    }

    const selectedHouseholdExists = selectedHouseholdId ? households.some((household) => household.id === selectedHouseholdId) : false;

    if (!selectedHouseholdId || !selectedHouseholdExists) {
      setSearchParams({ householdId: households[0].id }, { replace: true });
    }
  }, [households, householdsQuery.isSuccess, selectedHouseholdId, setSearchParams]);

  function selectHousehold(householdId: string) {
    setSearchParams({ householdId }, { replace: false });
  }

  async function handleCreatedHousehold(household: HouseholdResponse) {
    setSearchParams({ householdId: household.id }, { replace: false });
  }

  const summary = selectedHousehold ? (
    <Panel title="Selected household" subtitle="This selection lives in the URL so refreshes and sharing behave predictably.">
      <div>
        <h3 style={{ margin: 0 }}>{selectedHousehold.name}</h3>
        <p style={{ margin: '0.5rem 0 0', color: 'var(--muted)', lineHeight: 1.6 }}>
          {selectedHousehold.notes ?? 'No notes for this household yet.'}
        </p>
      </div>
    </Panel>
  ) : (
    <Panel title="Selected household" subtitle="Create or choose a household to start the vehicle workflow.">
      <p style={{ margin: 0, color: 'var(--muted)', lineHeight: 1.6 }}>
        The app starts from an empty database and uses the backend REST APIs directly.
      </p>
    </Panel>
  );

  return (
    <Shell
      sidebar={(
        <HouseholdPanel
          households={households}
          selectedHouseholdId={selectedHouseholdId}
          onSelectHousehold={selectHousehold}
          onCreatedHousehold={handleCreatedHousehold}
          isLoading={householdsQuery.isLoading}
          errorMessage={householdsQuery.error instanceof Error ? householdsQuery.error.message : undefined}
        />
      )}
    >
      {householdsQuery.error ? (
        <ErrorBanner
          message={householdsQuery.error instanceof Error ? householdsQuery.error.message : 'Could not load households.'}
          onRetry={() => householdsQuery.refetch()}
        />
      ) : null}

      {summary}

      <VehiclePanel householdId={selectedHousehold?.id} householdName={selectedHousehold?.name} />
    </Shell>
  );
}