import styles from './ErrorBanner.module.css';

interface ErrorBannerProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export function ErrorBanner({ title = 'Something went wrong', message, onRetry }: ErrorBannerProps) {
  return (
    <div className={styles.banner} role="alert">
      <div>
        <strong className={styles.title}>{title}</strong>
        <p className={styles.message}>{message}</p>
      </div>
      {onRetry ? (
        <button className={styles.retry} type="button" onClick={onRetry}>
          Retry
        </button>
      ) : null}
    </div>
  );
}