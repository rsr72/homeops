import type { VehicleResponse } from '../../types';
import styles from './VehicleList.module.css';

interface VehicleListProps {
  vehicles: VehicleResponse[];
  onEdit: (vehicle: VehicleResponse) => void;
  onDelete: (vehicle: VehicleResponse) => void;
  onManageMaintenance: (vehicle: VehicleResponse) => void;
  activeMaintenanceVehicleId?: string;
}

function money(value: number | null) {
  return value === null ? 'Not set' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function date(value: string | null) {
  return value ? new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(new Date(value)) : 'Not set';
}

export function VehicleList({ vehicles, onEdit, onDelete, onManageMaintenance, activeMaintenanceVehicleId }: VehicleListProps) {
  return (
    <div className={styles.list} aria-label="Vehicle list">
      {vehicles.map((vehicle) => (
        <article key={vehicle.id} className={styles.card} aria-label={`${vehicle.make} ${vehicle.model}`}>
          <div className={styles.header}>
            <div>
              <h3 className={styles.title}>{vehicle.make} {vehicle.model}</h3>
              <p className={styles.subtitle}>{vehicle.year} · {vehicle.vin ?? 'VIN not set'}</p>
            </div>
            <div className={styles.actions}>
              <button
                className={activeMaintenanceVehicleId === vehicle.id ? styles.active : styles.action}
                type="button"
                onClick={() => onManageMaintenance(vehicle)}
              >
                Maintenance
              </button>
              <button className={styles.action} type="button" onClick={() => onEdit(vehicle)}>Edit</button>
              <button className={styles.danger} type="button" onClick={() => onDelete(vehicle)}>Delete</button>
            </div>
          </div>

          <dl className={styles.details}>
            <div>
              <dt>Purchase date</dt>
              <dd>{date(vehicle.purchaseDate)}</dd>
            </div>
            <div>
              <dt>Purchase cost</dt>
              <dd>{money(vehicle.purchaseCost)}</dd>
            </div>
            <div>
              <dt>Mileage</dt>
              <dd>{vehicle.currentMileage ?? 'Not set'}</dd>
            </div>
            <div className={styles.notesBlock}>
              <dt>Notes</dt>
              <dd>{vehicle.notes ?? 'No notes yet'}</dd>
            </div>
          </dl>
        </article>
      ))}
    </div>
  );
}