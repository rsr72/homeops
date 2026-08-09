import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useCreateVehicleMutation, useUpdateVehicleMutation } from '../../api/queries';
import type { VehicleRequest, VehicleResponse } from '../../types';
import { FieldError } from '../../components/FieldError';
import styles from './VehicleForm.module.css';

interface VehicleFormProps {
  householdId: string;
  vehicle?: VehicleResponse;
  onSaved: () => void;
  onCancel: () => void;
}

interface VehicleFormValues {
  make: string;
  model: string;
  year: string;
  vin: string;
  notes: string;
  purchaseDate: string;
  purchaseCost: string;
  currentMileage: string;
}

const emptyValues: VehicleFormValues = {
  make: '',
  model: '',
  year: '',
  vin: '',
  notes: '',
  purchaseDate: '',
  purchaseCost: '',
  currentMileage: ''
};

function toValues(vehicle?: VehicleResponse): VehicleFormValues {
  if (!vehicle) {
    return emptyValues;
  }

  return {
    make: vehicle.make,
    model: vehicle.model,
    year: String(vehicle.year),
    vin: vehicle.vin ?? '',
    notes: vehicle.notes ?? '',
    purchaseDate: vehicle.purchaseDate ?? '',
    purchaseCost: vehicle.purchaseCost === null ? '' : String(vehicle.purchaseCost),
    currentMileage: vehicle.currentMileage === null ? '' : String(vehicle.currentMileage)
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

export function VehicleForm({ householdId, vehicle, onSaved, onCancel }: VehicleFormProps) {
  const isEditMode = Boolean(vehicle);
  const createVehicleMutation = useCreateVehicleMutation(householdId);
  const updateVehicleMutation = useUpdateVehicleMutation(householdId);
  const mutation = isEditMode ? updateVehicleMutation : createVehicleMutation;

  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<VehicleFormValues>({
    defaultValues: toValues(vehicle)
  });

  useEffect(() => {
    reset(toValues(vehicle));
  }, [reset, vehicle]);

  async function onSubmit(values: VehicleFormValues) {
    const payload: VehicleRequest = {
      make: values.make.trim(),
      model: values.model.trim(),
      year: Number(values.year),
      vin: optionalString(values.vin),
      notes: optionalString(values.notes),
      purchaseDate: optionalString(values.purchaseDate),
      purchaseCost: optionalNumber(values.purchaseCost),
      currentMileage: optionalNumber(values.currentMileage)
    };

    try {
      if (vehicle) {
        await updateVehicleMutation.mutateAsync({ vehicleId: vehicle.id, payload });
      } else {
        await createVehicleMutation.mutateAsync(payload);
      }

      reset(emptyValues);
      onSaved();
    } catch (error) {
      if (error instanceof Error && 'validationErrors' in error) {
        const apiError = error as { validationErrors?: Array<{ field: string; message: string }> };
        apiError.validationErrors?.forEach((validationError) => {
          setError(validationError.field as keyof VehicleFormValues, { message: validationError.message });
        });
        return;
      }

      setError('root', { message: error instanceof Error ? error.message : 'Could not save vehicle.' });
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
      <div className={styles.grid}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-make">Make</label>
          <input id="vehicle-make" className={styles.input} {...register('make', { required: 'Make is required', maxLength: { value: 200, message: 'Make must be 200 characters or fewer' } })} />
          <FieldError message={errors.make?.message} />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-model">Model</label>
          <input id="vehicle-model" className={styles.input} {...register('model', { required: 'Model is required', maxLength: { value: 200, message: 'Model must be 200 characters or fewer' } })} />
          <FieldError message={errors.model?.message} />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-year">Year</label>
          <input id="vehicle-year" className={styles.input} type="number" inputMode="numeric" {...register('year', {
            required: 'Year is required',
            min: { value: 1886, message: 'Year must be 1886 or later' },
            max: { value: 2100, message: 'Year must be 2100 or earlier' }
          })} />
          <FieldError message={errors.year?.message} />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-vin">VIN</label>
          <input id="vehicle-vin" className={styles.input} maxLength={17} {...register('vin', { maxLength: { value: 17, message: 'VIN must be 17 characters or fewer' } })} />
          <FieldError message={errors.vin?.message} />
        </div>
        <div className={styles.fieldFull}>
          <label className={styles.label} htmlFor="vehicle-notes">Notes</label>
          <textarea id="vehicle-notes" className={styles.textarea} rows={3} {...register('notes', { maxLength: { value: 2000, message: 'Notes must be 2000 characters or fewer' } })} />
          <FieldError message={errors.notes?.message} />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-purchase-date">Purchase date</label>
          <input id="vehicle-purchase-date" className={styles.input} type="date" {...register('purchaseDate')} />
          <FieldError message={errors.purchaseDate?.message} />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-purchase-cost">Purchase cost</label>
          <input id="vehicle-purchase-cost" className={styles.input} type="number" step="0.01" min="0" inputMode="decimal" {...register('purchaseCost')} />
          <FieldError message={errors.purchaseCost?.message} />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="vehicle-current-mileage">Current mileage</label>
          <input id="vehicle-current-mileage" className={styles.input} type="number" min="0" inputMode="numeric" {...register('currentMileage')} />
          <FieldError message={errors.currentMileage?.message} />
        </div>
      </div>

      <FieldError message={errors.root?.message} />

      <div className={styles.actions}>
        <button className={styles.primary} type="submit" disabled={isSubmitting || mutation.isPending}>
          {mutation.isPending ? 'Saving...' : isEditMode ? 'Save vehicle' : 'Create vehicle'}
        </button>
        <button className={styles.secondary} type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}