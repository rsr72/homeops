import { http, HttpResponse } from 'msw';
import type { HouseholdResponse, VehicleResponse } from '../types';

type HouseholdStore = {
  households: HouseholdResponse[];
  vehiclesByHousehold: Record<string, VehicleResponse[]>;
  vehicleValidationMode: 'normal' | 'rejectMake';
};

const store: HouseholdStore = {
  households: [],
  vehiclesByHousehold: {},
  vehicleValidationMode: 'normal'
};

let idCounter = 1;

function nextId() {
  const suffix = String(idCounter++).padStart(12, '0');
  return `00000000-0000-0000-0000-${suffix}`;
}

function nowIso() {
  return '2026-08-09T12:00:00Z';
}

function apiError(status: number, error: string, message: string, validationErrors: Array<{ field: string; message: string }> = []) {
  return HttpResponse.json(
    {
      timestamp: nowIso(),
      status,
      error,
      message,
      validationErrors
    },
    { status }
  );
}

function ensureVehicleBucket(householdId: string) {
  if (!store.vehiclesByHousehold[householdId]) {
    store.vehiclesByHousehold[householdId] = [];
  }

  return store.vehiclesByHousehold[householdId];
}

export function resetHomeOpsStore() {
  store.households = [];
  store.vehiclesByHousehold = {};
  store.vehicleValidationMode = 'normal';
  idCounter = 1;
}

export function seedHousehold(name: string, notes?: string) {
  const household: HouseholdResponse = {
    id: nextId(),
    name,
    notes: notes ?? null,
    createdAt: nowIso(),
    updatedAt: nowIso()
  };

  store.households.push(household);
  ensureVehicleBucket(household.id);
  return household;
}

export function seedVehicle(householdId: string, make: string, model: string) {
  const vehicle: VehicleResponse = {
    id: nextId(),
    householdId,
    make,
    model,
    year: 2022,
    vin: '1HGCM82633A004352',
    notes: 'Seeded vehicle',
    purchaseDate: '2024-01-15',
    purchaseCost: 24500,
    currentMileage: 12800,
    createdAt: nowIso(),
    updatedAt: nowIso()
  };

  ensureVehicleBucket(householdId).push(vehicle);
  return vehicle;
}

export function setVehicleValidationMode(mode: HouseholdStore['vehicleValidationMode']) {
  store.vehicleValidationMode = mode;
}

export const handlers = [
  http.get('/api/households', () => HttpResponse.json(store.households)),

  http.post('/api/households', async ({ request }) => {
    const body = (await request.json()) as { name?: string; notes?: string };

    if (!body.name || !body.name.trim()) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'name', message: 'name is required' }
      ]);
    }

    const household = seedHousehold(body.name.trim(), body.notes?.trim() || undefined);
    return HttpResponse.json(household, { status: 201 });
  }),

  http.get('/api/households/:householdId/vehicles', ({ params }) => {
    const householdId = String(params.householdId);
    return HttpResponse.json(ensureVehicleBucket(householdId));
  }),

  http.post('/api/households/:householdId/vehicles', async ({ params, request }) => {
    const householdId = String(params.householdId);
    const body = (await request.json()) as { make?: string; model?: string; year?: number };

    if (store.vehicleValidationMode === 'rejectMake' && body.make === 'Validation failure') {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'make', message: 'make is required' }
      ]);
    }

    if (!body.make || !body.make.trim()) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'make', message: 'make is required' }
      ]);
    }

    if (!body.model || !body.model.trim()) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'model', message: 'model is required' }
      ]);
    }

    if (!body.year) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'year', message: 'year is required' }
      ]);
    }

    const vehicle: VehicleResponse = {
      id: nextId(),
      householdId,
      make: body.make.trim(),
      model: body.model.trim(),
      year: body.year,
      vin: '1HGCM82633A004352',
      notes: 'Created in test',
      purchaseDate: '2024-01-15',
      purchaseCost: 24500,
      currentMileage: 12800,
      createdAt: nowIso(),
      updatedAt: nowIso()
    };

    ensureVehicleBucket(householdId).push(vehicle);
    return HttpResponse.json(vehicle, { status: 201 });
  }),

  http.put('/api/households/:householdId/vehicles/:vehicleId', async ({ params, request }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);
    const body = (await request.json()) as { make?: string; model?: string; year?: number };

    const bucket = ensureVehicleBucket(householdId);
    const index = bucket.findIndex((vehicle) => vehicle.id === vehicleId);

    if (index < 0) {
      return apiError(404, 'NOT_FOUND', `Vehicle with id '${vehicleId}' was not found`);
    }

    const updatedVehicle: VehicleResponse = {
      ...bucket[index],
      make: body.make?.trim() ?? bucket[index].make,
      model: body.model?.trim() ?? bucket[index].model,
      year: body.year ?? bucket[index].year,
      updatedAt: '2026-08-09T12:30:00Z'
    };

    bucket[index] = updatedVehicle;
    return HttpResponse.json(updatedVehicle);
  }),

  http.delete('/api/households/:householdId/vehicles/:vehicleId', ({ params }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);
    const bucket = ensureVehicleBucket(householdId);
    store.vehiclesByHousehold[householdId] = bucket.filter((vehicle) => vehicle.id !== vehicleId);
    return new HttpResponse(null, { status: 204 });
  })
];