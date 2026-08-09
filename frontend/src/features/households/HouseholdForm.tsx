import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useCreateHouseholdMutation } from '../../api/queries';
import type { HouseholdRequest, HouseholdResponse } from '../../types';
import { FieldError } from '../../components/FieldError';
import styles from './HouseholdForm.module.css';

interface HouseholdFormProps {
  onCreated?: (household: HouseholdResponse) => void;
}

interface HouseholdFormValues {
  name: string;
  notes: string;
}

export function HouseholdForm({ onCreated }: HouseholdFormProps) {
  const createHouseholdMutation = useCreateHouseholdMutation();
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<HouseholdFormValues>({
    defaultValues: { name: '', notes: '' }
  });

  useEffect(() => {
    if (createHouseholdMutation.isSuccess) {
      reset();
    }
  }, [createHouseholdMutation.isSuccess, reset]);

  async function onSubmit(values: HouseholdFormValues) {
    try {
      const payload: HouseholdRequest = {
        name: values.name.trim(),
        notes: values.notes.trim() || undefined
      };

      const createdHousehold = await createHouseholdMutation.mutateAsync(payload);
      onCreated?.(createdHousehold);
      reset();
    } catch (error) {
      if (error instanceof Error && 'validationErrors' in error) {
        const apiError = error as { validationErrors?: Array<{ field: string; message: string }>; message: string };
        apiError.validationErrors?.forEach((validationError) => {
          setError(validationError.field as keyof HouseholdFormValues, { message: validationError.message });
        });
        return;
      }

      setError('root', { message: error instanceof Error ? error.message : 'Could not create household.' });
    }
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)}>
      <div className={styles.field}>
        <label className={styles.label} htmlFor="household-name">Household name</label>
        <input id="household-name" className={styles.input} {...register('name', { required: 'Name is required', maxLength: { value: 200, message: 'Name must be 200 characters or fewer' } })} />
        <FieldError message={errors.name?.message} />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="household-notes">Notes</label>
        <textarea id="household-notes" className={styles.textarea} rows={3} {...register('notes', { maxLength: { value: 2000, message: 'Notes must be 2000 characters or fewer' } })} />
        <FieldError message={errors.notes?.message} />
      </div>

      <FieldError message={errors.root?.message} />

      <button className={styles.button} type="submit" disabled={isSubmitting || createHouseholdMutation.isPending}>
        {createHouseholdMutation.isPending ? 'Creating...' : 'Create household'}
      </button>
    </form>
  );
}