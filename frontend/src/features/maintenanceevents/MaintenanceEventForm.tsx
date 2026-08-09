import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import {
  useCreateMaintenanceEventMutation,
  useUpdateMaintenanceEventMutation
} from '../../api/queries';
import { FieldError } from '../../components/FieldError';
import type { MaintenanceEventRequest, MaintenanceEventResponse } from '../../types';
import styles from './MaintenanceEventForm.module.css';

interface MaintenanceEventFormProps {
  householdId: string;
  vehicleId: string;
  event?: MaintenanceEventResponse;
  onSaved: () => void;
  onCancel: () => void;
}

interface MaintenanceEventFormValues {
  serviceDate: string;
  description: string;
  mileage: string;
  cost: string;
  notes: string;
}

const emptyValues: MaintenanceEventFormValues = {
  serviceDate: '',
  description: '',
  mileage: '',
  cost: '',
  notes: ''
};

function toValues(event?: MaintenanceEventResponse): MaintenanceEventFormValues {
  if (!event) {
    return emptyValues;
  }

  return {
    serviceDate: event.serviceDate,
    description: event.description,
    mileage: event.mileage === null ? '' : String(event.mileage),
    cost: event.cost === null ? '' : String(event.cost),
    notes: event.notes ?? ''
  };
}

function optionalString(value: string) {
  const trimmedValue = value.trim();
  return trimmedValue ? trimmedValue : undefined;
}

function optionalNumber(value: string) {
  if (!value.trim()) {
    return undefined;
  }

  const parsedValue = Number(value);
  return Number.isFinite(parsedValue) ? parsedValue : undefined;
}

export function MaintenanceEventForm({ householdId, vehicleId, event, onSaved, onCancel }: MaintenanceEventFormProps) {
  const isEditMode = Boolean(event);
  const createMutation = useCreateMaintenanceEventMutation(householdId, vehicleId);
  const updateMutation = useUpdateMaintenanceEventMutation(householdId, vehicleId);
  const mutation = isEditMode ? updateMutation : createMutation;

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting }
  } = useForm<MaintenanceEventFormValues>({ defaultValues: toValues(event) });

  useEffect(() => {
    reset(toValues(event));
  }, [event, reset]);

  async function onSubmit(values: MaintenanceEventFormValues) {
    const payload: MaintenanceEventRequest = {
      serviceDate: values.serviceDate,
      description: values.description.trim(),
      mileage: optionalNumber(values.mileage),
      cost: optionalNumber(values.cost),
      notes: optionalString(values.notes)
    };

    try {
      if (event) {
        await updateMutation.mutateAsync({ eventId: event.id, payload });
      } else {
        await createMutation.mutateAsync(payload);
      }

      reset(emptyValues);
      onSaved();
    } catch (error) {
      if (error instanceof Error && 'validationErrors' in error) {
        const apiError = error as { validationErrors?: Array<{ field: string; message: string }> };
        apiError.validationErrors?.forEach((validationError) => {
          setError(validationError.field as keyof MaintenanceEventFormValues, { message: validationError.message });
        });
        return;
      }

      setError('root', {
        message: error instanceof Error ? error.message : 'Could not save maintenance event.'
      });
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
      <div className={styles.grid}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="maintenance-service-date">Service date</label>
          <input
            id="maintenance-service-date"
            className={styles.input}
            type="date"
            {...register('serviceDate', { required: 'serviceDate is required' })}
          />
          <FieldError message={errors.serviceDate?.message} />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="maintenance-mileage">Mileage</label>
          <input
            id="maintenance-mileage"
            className={styles.input}
            type="number"
            min="0"
            inputMode="numeric"
            {...register('mileage')}
          />
          <FieldError message={errors.mileage?.message} />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="maintenance-cost">Cost</label>
          <input
            id="maintenance-cost"
            className={styles.input}
            type="number"
            min="0"
            step="0.01"
            inputMode="decimal"
            {...register('cost')}
          />
          <FieldError message={errors.cost?.message} />
        </div>

        <div className={styles.fieldFull}>
          <label className={styles.label} htmlFor="maintenance-description">Description</label>
          <textarea
            id="maintenance-description"
            className={styles.textarea}
            rows={2}
            {...register('description', {
              required: 'description is required',
              maxLength: { value: 500, message: 'description must be 500 characters or fewer' }
            })}
          />
          <FieldError message={errors.description?.message} />
        </div>

        <div className={styles.fieldFull}>
          <label className={styles.label} htmlFor="maintenance-notes">Notes</label>
          <textarea
            id="maintenance-notes"
            className={styles.textarea}
            rows={3}
            {...register('notes', {
              maxLength: { value: 2000, message: 'notes must be 2000 characters or fewer' }
            })}
          />
          <FieldError message={errors.notes?.message} />
        </div>
      </div>

      <FieldError message={errors.root?.message} />

      <div className={styles.actions}>
        <button className={styles.primary} type="submit" disabled={isSubmitting || mutation.isPending}>
          {mutation.isPending ? 'Saving...' : isEditMode ? 'Save event' : 'Create event'}
        </button>
        <button className={styles.secondary} type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}
