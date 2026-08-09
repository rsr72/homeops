import type { ApiErrorResponse } from '../types';

export class HomeOpsApiError extends Error {
  status: number;
  code: string;
  validationErrors: ApiErrorResponse['validationErrors'];

  constructor(status: number, code: string, message: string, validationErrors: ApiErrorResponse['validationErrors'] = []) {
    super(message);
    this.name = 'HomeOpsApiError';
    this.status = status;
    this.code = code;
    this.validationErrors = validationErrors;
  }
}

const BASE_URL = '';

async function parseJsonResponse(response: Response): Promise<unknown> {
  const text = await response.text();

  if (!text) {
    return null;
  }

  return JSON.parse(text) as unknown;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const body = await parseJsonResponse(response);

  if (!response.ok) {
    if (body && typeof body === 'object') {
      const errorBody = body as ApiErrorResponse;
      throw new HomeOpsApiError(response.status, errorBody.error, errorBody.message, errorBody.validationErrors ?? []);
    }

    throw new HomeOpsApiError(response.status, 'UNKNOWN_ERROR', response.statusText || 'Request failed');
  }

  return body as T;
}