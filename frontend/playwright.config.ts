import { defineConfig, devices } from '@playwright/test';

const reuseExistingBackendServer = process.env.PLAYWRIGHT_REUSE_EXISTING_BACKEND === 'true';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30_000,
  expect: {
    timeout: 10_000
  },
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome']
      }
    }
  ],
  webServer: [
    {
      command: "cd .. && set -a && . ./backend/.env.postgres && set +a && SPRING_PROFILES_ACTIVE=local-postgres ./backend/mvnw -f backend/pom.xml org.springframework.boot:spring-boot-maven-plugin:3.3.3:run",
      url: 'http://127.0.0.1:8080/actuator/health',
      reuseExistingServer: reuseExistingBackendServer,
      timeout: 120_000,
      stdout: 'pipe',
      stderr: 'pipe'
    },
    {
      command: 'npm run dev -- --host 127.0.0.1 --port 5173',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: false,
      timeout: 60_000,
      stdout: 'pipe',
      stderr: 'pipe'
    }
  ]
});
