import type { PropsWithChildren, ReactNode } from 'react';
import styles from './Shell.module.css';

interface ShellProps extends PropsWithChildren {
  sidebar: ReactNode;
}

export function Shell({ sidebar, children }: ShellProps) {
  return (
    <div className={styles.page}>
      <header className={styles.hero}>
        <p className={styles.kicker}>HomeOps</p>
        <h1 className={styles.title}>Household and vehicle records that stay usable.</h1>
        <p className={styles.subtitle}>
          Create a household, keep it selected in the URL, and manage the vehicle records already persisted in the backend.
        </p>
      </header>

      <div className={styles.grid}>
        <aside className={styles.sidebar}>{sidebar}</aside>
        <main className={styles.main}>{children}</main>
      </div>
    </div>
  );
}