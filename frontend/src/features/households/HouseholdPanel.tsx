import { Panel } from '../../components/Panel';
import { EmptyState } from '../../components/EmptyState';
import { HouseholdForm } from './HouseholdForm';
import type { HouseholdResponse } from '../../types';
import styles from './HouseholdPanel.module.css';

interface HouseholdPanelProps {
  households: HouseholdResponse[];
  selectedHouseholdId?: string;
  onSelectHousehold: (householdId: string) => void;
  onCreatedHousehold: (household: HouseholdResponse) => void;
  isLoading: boolean;
  errorMessage?: string;
}

export function HouseholdPanel({ households, selectedHouseholdId, onSelectHousehold, onCreatedHousehold, isLoading, errorMessage }: HouseholdPanelProps) {
  return (
    <Panel title="Households" subtitle="Create or choose the household that stays selected in the URL.">
      <div className={styles.stack}>
        {errorMessage ? <div className={styles.error}>{errorMessage}</div> : null}

        {isLoading ? <div className={styles.loading}>Loading households...</div> : null}

        {!isLoading && households.length === 0 ? (
          <EmptyState
            title="No households yet"
            description="Create the first household to start using the browser UI from an empty database."
          />
        ) : null}

        {households.length > 0 ? (
          <div className={styles.list} aria-label="Household list">
            {households.map((household) => {
              const isActive = household.id === selectedHouseholdId;

              return (
                <button
                  key={household.id}
                  type="button"
                  className={`${styles.householdItem} ${isActive ? styles.active : ''}`}
                  onClick={() => onSelectHousehold(household.id)}
                >
                  <span className={styles.name}>{household.name}</span>
                  {household.notes ? <span className={styles.notes}>{household.notes}</span> : <span className={styles.notesMuted}>No notes</span>}
                </button>
              );
            })}
          </div>
        ) : null}

        <div className={styles.createBlock}>
          <h3 className={styles.createTitle}>Create household</h3>
          <HouseholdForm onCreated={onCreatedHousehold} />
        </div>
      </div>
    </Panel>
  );
}