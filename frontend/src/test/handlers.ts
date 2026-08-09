import { http, HttpResponse } from 'msw';
import type { HouseholdResponse, MaintenanceEventResponse, VehicleResponse } from '../types';

type HouseholdStore = {
  households: HouseholdResponse[];
  vehiclesByHousehold: Record<string, VehicleResponse[]>;
  maintenanceByVehicle: Record<string, MaintenanceEventResponse[]>;
  vehicleValidationMode: 'normal' | 'rejectMake';
};

const store: HouseholdStore = {
  households: [],
  vehiclesByHousehold: {},
  maintenanceByVehicle: {},
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

function ensureMaintenanceBucket(vehicleId: string) {
  if (!store.maintenanceByVehicle[vehicleId]) {
    store.maintenanceByVehicle[vehicleId] = [];
  }

  return store.maintenanceByVehicle[vehicleId];
}

function sortMaintenanceEvents(events: MaintenanceEventResponse[]) {
  return [...events].sort((left, right) => {
    if (left.serviceDate !== right.serviceDate) {
      return right.serviceDate.localeCompare(left.serviceDate);
    }

    return right.createdAt.localeCompare(left.createdAt);
  });
}

function findVehicle(householdId: string, vehicleId: string) {
  const bucket = ensureVehicleBucket(householdId);
  return bucket.find((vehicle) => vehicle.id === vehicleId);
}

export function resetHomeOpsStore() {
  store.households = [];
  store.vehiclesByHousehold = {};
  store.maintenanceByVehicle = {};
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
  ensureMaintenanceBucket(vehicle.id);
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
    ensureMaintenanceBucket(vehicle.id);
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
    delete store.maintenanceByVehicle[vehicleId];
    return new HttpResponse(null, { status: 204 });
  }),

  http.get('/api/households/:householdId/vehicles/:vehicleId/maintenance-events', ({ params }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);

    if (!findVehicle(householdId, vehicleId)) {
      return apiError(404, 'NOT_FOUND', `Vehicle with id '${vehicleId}' was not found`);
    }

    return HttpResponse.json(sortMaintenanceEvents(ensureMaintenanceBucket(vehicleId)));
  }),

  http.post('/api/households/:householdId/vehicles/:vehicleId/maintenance-events', async ({ params, request }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);

    if (!findVehicle(householdId, vehicleId)) {
      return apiError(404, 'NOT_FOUND', `Vehicle with id '${vehicleId}' was not found`);
    }

    const body = (await request.json()) as {
      serviceDate?: string;
      description?: string;
      mileage?: number;
      cost?: number;
      notes?: string;
    };

    if (!body.serviceDate) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'serviceDate', message: 'serviceDate is required' }
      ]);
    }

    if (!body.description || !body.description.trim()) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'description', message: 'description is required' }
      ]);
    }

    if (typeof body.mileage === 'number' && body.mileage < 0) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'mileage', message: 'mileage must be zero or greater' }
      ]);
    }

    if (typeof body.cost === 'number' && body.cost < 0) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'cost', message: 'cost must be zero or greater' }
      ]);
    }

    const event: MaintenanceEventResponse = {
      id: nextId(),
      householdId,
      vehicleId,
      serviceDate: body.serviceDate,
      description: body.description.trim(),
      mileage: body.mileage ?? null,
      cost: body.cost ?? null,
      notes: body.notes?.trim() ? body.notes.trim() : null,
      createdAt: nowIso(),
      updatedAt: nowIso()
    };

    ensureMaintenanceBucket(vehicleId).push(event);
    return HttpResponse.json(event, { status: 201 });
  }),

  http.get('/api/households/:householdId/vehicles/:vehicleId/maintenance-events/:eventId', ({ params }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);
    const eventId = String(params.eventId);

    if (!findVehicle(householdId, vehicleId)) {
      return apiError(404, 'NOT_FOUND', `Vehicle with id '${vehicleId}' was not found`);
    }

    const event = ensureMaintenanceBucket(vehicleId).find((entry) => entry.id === eventId);
    if (!event) {
      return apiError(404, 'NOT_FOUND', `Maintenance event with id '${eventId}' was not found`);
    }

    return HttpResponse.json(event);
  }),

  http.put('/api/households/:householdId/vehicles/:vehicleId/maintenance-events/:eventId', async ({ params, request }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);
    const eventId = String(params.eventId);

    if (!findVehicle(householdId, vehicleId)) {
      return apiError(404, 'NOT_FOUND', `Vehicle with id '${vehicleId}' was not found`);
    }

    const body = (await request.json()) as {
      serviceDate?: string;
      description?: string;
      mileage?: number;
      cost?: number;
      notes?: string;
    };

    if (!body.serviceDate) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'serviceDate', message: 'serviceDate is required' }
      ]);
    }

    if (!body.description || !body.description.trim()) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'description', message: 'description is required' }
      ]);
    }

    if (typeof body.mileage === 'number' && body.mileage < 0) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'mileage', message: 'mileage must be zero or greater' }
      ]);
    }

    if (typeof body.cost === 'number' && body.cost < 0) {
      return apiError(400, 'VALIDATION_ERROR', 'Request validation failed', [
        { field: 'cost', message: 'cost must be zero or greater' }
      ]);
    }

    const bucket = ensureMaintenanceBucket(vehicleId);
    const index = bucket.findIndex((event) => event.id === eventId);
    if (index < 0) {
      return apiError(404, 'NOT_FOUND', `Maintenance event with id '${eventId}' was not found`);
    }

    const updatedEvent: MaintenanceEventResponse = {
      ...bucket[index],
      serviceDate: body.serviceDate,
      description: body.description.trim(),
      mileage: body.mileage ?? null,
      cost: body.cost ?? null,
      notes: body.notes?.trim() ? body.notes.trim() : null,
      updatedAt: '2026-08-09T12:30:00Z'
    };

    bucket[index] = updatedEvent;
    return HttpResponse.json(updatedEvent);
  }),

  http.delete('/api/households/:householdId/vehicles/:vehicleId/maintenance-events/:eventId', ({ params }) => {
    const householdId = String(params.householdId);
    const vehicleId = String(params.vehicleId);
    const eventId = String(params.eventId);

    if (!findVehicle(householdId, vehicleId)) {
      return apiError(404, 'NOT_FOUND', `Vehicle with id '${vehicleId}' was not found`);
    }

    const bucket = ensureMaintenanceBucket(vehicleId);
    const existingEvent = bucket.find((event) => event.id === eventId);
    if (!existingEvent) {
      return apiError(404, 'NOT_FOUND', `Maintenance event with id '${eventId}' was not found`);
    }

    store.maintenanceByVehicle[vehicleId] = bucket.filter((event) => event.id !== eventId);
    return new HttpResponse(null, { status: 204 });
  })
];