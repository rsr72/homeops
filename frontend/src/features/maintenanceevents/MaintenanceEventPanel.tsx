import { useMemo, useState } from 'react';
import {
  useDeleteMaintenanceEventMutation,
  useMaintenanceEventsQuery
} from '../../api/queries';
import { EmptyState } from '../../components/EmptyState';
import { ErrorBanner } from '../../components/ErrorBanner';
import { Panel } from '../../components/Panel';
import type { MaintenanceEventResponse, VehicleResponse } from '../../types';
import { MaintenanceEventForm } from './MaintenanceEventForm';
import { MaintenanceEventList } from './MaintenanceEventList';
import styles from './MaintenanceEventPanel.module.css';

interface MaintenanceEventPanelProps {
  householdId: string;
  vehicle: VehicleResponse;
}

export function MaintenanceEventPanel({ householdId, vehicle }: MaintenanceEventPanelProps) {
  const [selectedEvent, setSelectedEvent] = useState<MaintenanceEventResponse | undefined>();
  const [pendingDelete, setPendingDelete] = useState<MaintenanceEventResponse | undefined>();
  const [isFormOpen, setIsFormOpen] = useState(false);

  const maintenanceEventsQuery = useMaintenanceEventsQuery(householdId, vehicle.id);
  const deleteMutation = useDeleteMaintenanceEventMutation(householdId, vehicle.id);

  const eventCount = maintenanceEventsQuery.data?.length ?? 0;
  const panelTitle = useMemo(
    () => `${vehicle.make} ${vehicle.model} maintenance`,
    [vehicle.make, vehicle.model]
  );

  function startCreate() {
    setSelectedEvent(undefined);
    setIsFormOpen(true);
  }

  function startEdit(event: MaintenanceEventResponse) {
    setSelectedEvent(event);
    setIsFormOpen(true);
  }

  async function confirmDelete() {
    if (!pendingDelete) {
      return;
    }

    await deleteMutation.mutateAsync(pendingDelete.id);
    setPendingDelete(undefined);
    if (selectedEvent?.id === pendingDelete.id) {
      setSelectedEvent(undefined);
      setIsFormOpen(false);
    }
  }

  return (
    <Panel
      title={panelTitle}
      subtitle={eventCount > 0 ? `${eventCount} event${eventCount === 1 ? '' : 's'} recorded.` : 'No maintenance events yet for this vehicle.'}
      action={(
        <button className={styles.addButton} type="button" onClick={startCreate}>
          Add event
        </button>
      )}
    >
      <div className={styles.stack}>
        {maintenanceEventsQuery.error ? (
          <ErrorBanner
            message={maintenanceEventsQuery.error instanceof Error ? maintenanceEventsQuery.error.message : 'Could not load maintenance events.'}
            onRetry={() => maintenanceEventsQuery.refetch()}
          />
        ) : null}

        {deleteMutation.isError ? (
          <ErrorBanner
            title="Delete failed"
            message={deleteMutation.error instanceof Error ? deleteMutation.error.message : 'Could not delete maintenance event.'}
            onRetry={() => pendingDelete && confirmDelete()}
          />
        ) : null}

        {maintenanceEventsQuery.isLoading ? <div className={styles.loading}>Loading maintenance events...</div> : null}

        {!maintenanceEventsQuery.isLoading && !maintenanceEventsQuery.error && eventCount === 0 ? (
          <EmptyState
            title="No maintenance events yet"
            description="Add the first maintenance event for this vehicle. Entries are persisted through the backend API and shown in service-date order."
            actionLabel="Add event"
            onAction={startCreate}
          />
        ) : null}

        {!maintenanceEventsQuery.isLoading && !maintenanceEventsQuery.error && eventCount > 0 ? (
          <MaintenanceEventList
            events={maintenanceEventsQuery.data ?? []}
            onEdit={startEdit}
            onDelete={setPendingDelete}
          />
        ) : null}

        {isFormOpen ? (
          <div className={styles.formCard}>
            <h3 className={styles.formTitle}>{selectedEvent ? 'Edit maintenance event' : 'Create maintenance event'}</h3>
            <MaintenanceEventForm
              householdId={householdId}
              vehicleId={vehicle.id}
              event={selectedEvent}
              onSaved={() => setIsFormOpen(false)}
              onCancel={() => setIsFormOpen(false)}
            />
          </div>
        ) : null}

        {pendingDelete ? (
          <div className={styles.confirmDelete} role="status" aria-live="polite">
            <div>
              <strong>Delete maintenance event "{pendingDelete.description}"?</strong>
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
