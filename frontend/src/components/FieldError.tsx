import styles from './FieldError.module.css';

interface FieldErrorProps {
  message?: string;
}

export function FieldError({ message }: FieldErrorProps) {
  if (!message) {
    return null;
  }

  return <p className={styles.error}>{message}</p>;
}