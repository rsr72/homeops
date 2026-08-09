import type { MaintenanceEventResponse } from '../../types';
import styles from './MaintenanceEventList.module.css';

interface MaintenanceEventListProps {
  events: MaintenanceEventResponse[];
  onEdit: (event: MaintenanceEventResponse) => void;
  onDelete: (event: MaintenanceEventResponse) => void;
}

function money(value: number | null) {
  return value === null ? 'Not set' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function serviceDate(value: string) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(new Date(value));
}

export function MaintenanceEventList({ events, onEdit, onDelete }: MaintenanceEventListProps) {
  return (
    <div className={styles.list} aria-label="Maintenance event list">
      {events.map((event) => (
        <article key={event.id} className={styles.card} aria-label={event.description}>
          <div className={styles.header}>
            <div>
              <h3 className={styles.title}>{event.description}</h3>
              <p className={styles.subtitle}>{serviceDate(event.serviceDate)}</p>
            </div>
            <div className={styles.actions}>
              <button className={styles.action} type="button" onClick={() => onEdit(event)}>Edit</button>
              <button className={styles.danger} type="button" onClick={() => onDelete(event)}>Delete</button>
            </div>
          </div>

          <dl className={styles.details}>
            <div>
              <dt>Mileage</dt>
              <dd>{event.mileage ?? 'Not set'}</dd>
            </div>
            <div>
              <dt>Cost</dt>
              <dd>{money(event.cost)}</dd>
            </div>
            <div className={styles.notesBlock}>
              <dt>Notes</dt>
              <dd>{event.notes ?? 'No notes yet'}</dd>
            </div>
          </dl>
        </article>
      ))}
    </div>
  );
}
