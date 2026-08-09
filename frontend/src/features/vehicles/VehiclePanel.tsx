import { useMemo, useState } from 'react';
import { Panel } from '../../components/Panel';
import { EmptyState } from '../../components/EmptyState';
import { ErrorBanner } from '../../components/ErrorBanner';
import { useDeleteVehicleMutation, useVehiclesQuery } from '../../api/queries';
import type { VehicleResponse } from '../../types';
import { VehicleForm } from './VehicleForm';
import { VehicleList } from './VehicleList';
import styles from './VehiclePanel.module.css';

interface VehiclePanelProps {
  householdId?: string;
  householdName?: string;
}

export function VehiclePanel({ householdId, householdName }: VehiclePanelProps) {
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleResponse | undefined>();
  const [pendingDelete, setPendingDelete] = useState<VehicleResponse | undefined>();
  const [isFormOpen, setIsFormOpen] = useState(false);

  const vehiclesQuery = useVehiclesQuery(householdId);
  const deleteVehicleMutation = useDeleteVehicleMutation(householdId ?? '');

  const vehicleCount = vehiclesQuery.data?.length ?? 0;
  const panelTitle = useMemo(() => {
    if (!householdName) {
      return 'Vehicles';
    }

    return `${householdName} vehicles`;
  }, [householdName]);

  function startCreate() {
    setSelectedVehicle(undefined);
    setIsFormOpen(true);
  }

  function startEdit(vehicle: VehicleResponse) {
    setSelectedVehicle(vehicle);
    setIsFormOpen(true);
  }

  async function confirmDelete() {
    if (!householdId || !pendingDelete) {
      return;
    }

    await deleteVehicleMutation.mutateAsync(pendingDelete.id);
    setPendingDelete(undefined);
    if (selectedVehicle?.id === pendingDelete.id) {
      setSelectedVehicle(undefined);
    }
  }

  if (!householdId) {
    return (
      <Panel title="Vehicles" subtitle="Choose a household to manage its vehicles.">
        <EmptyState
          title="No household selected"
          description="Create or select a household in the sidebar to see vehicle records."
        />
      </Panel>
    );
  }

  return (
    <Panel
      title={panelTitle}
      subtitle={vehicleCount > 0 ? `${vehicleCount} vehicle${vehicleCount === 1 ? '' : 's'} available.` : 'No vehicles yet in this household.'}
      action={(
        <button className={styles.addButton} type="button" onClick={startCreate}>
          Add vehicle
        </button>
      )}
    >
      <div className={styles.stack}>
        {vehiclesQuery.error ? (
          <ErrorBanner
            message={vehiclesQuery.error instanceof Error ? vehiclesQuery.error.message : 'Could not load vehicles.'}
            onRetry={() => vehiclesQuery.refetch()}
          />
        ) : null}

        {deleteVehicleMutation.isError ? (
          <ErrorBanner
            title="Delete failed"
            message={deleteVehicleMutation.error instanceof Error ? deleteVehicleMutation.error.message : 'Could not delete vehicle.'}
            onRetry={() => pendingDelete && confirmDelete()}
          />
        ) : null}

        {vehiclesQuery.isLoading ? <div className={styles.loading}>Loading vehicles...</div> : null}

        {!vehiclesQuery.isLoading && !vehiclesQuery.error && vehicleCount === 0 ? (
          <EmptyState
            title="No vehicles yet"
            description="Add the first vehicle for this household. The form uses the backend validation rules, and the list updates immediately after save."
            actionLabel="Add vehicle"
            onAction={startCreate}
          />
        ) : null}

        {!vehiclesQuery.isLoading && !vehiclesQuery.error && vehicleCount > 0 ? (
          <VehicleList vehicles={vehiclesQuery.data ?? []} onEdit={startEdit} onDelete={setPendingDelete} />
        ) : null}

        {isFormOpen ? (
          <div className={styles.formCard}>
            <h3 className={styles.formTitle}>{selectedVehicle ? 'Edit vehicle' : 'Create vehicle'}</h3>
            <VehicleForm
              householdId={householdId}
              vehicle={selectedVehicle}
              onSaved={() => setIsFormOpen(false)}
              onCancel={() => setIsFormOpen(false)}
            />
          </div>
        ) : null}

        {pendingDelete ? (
          <div className={styles.confirmDelete} role="status" aria-live="polite">
            <div>
              <strong>Delete {pendingDelete.make} {pendingDelete.model}?</strong>
              <p>This cannot be undone.</p>
            </div>
            <div className={styles.confirmActions}>
              <button className={styles.confirmPrimary} type="button" onClick={confirmDelete}>
                Confirm delete
              </button>
              <button className={styles.confirmSecondary} type="button" onClick={() => setPendingDelete(undefined)}>
                Cancel
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </Panel>
  );
}