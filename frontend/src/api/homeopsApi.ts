import { request } from './http';
import type {
  HouseholdRequest,
  HouseholdResponse,
  MaintenanceEventRequest,
  MaintenanceEventResponse,
  VehicleRequest,
  VehicleResponse
} from '../types';

export const homeOpsApi = {
  households: {
    list: () => request<HouseholdResponse[]>('/api/households'),
    create: (payload: HouseholdRequest) =>
      request<HouseholdResponse>('/api/households', {
        method: 'POST',
        body: JSON.stringify(payload)
      })
  },
  vehicles: {
    list: (householdId: string) => request<VehicleResponse[]>(`/api/households/${householdId}/vehicles`),
    create: (householdId: string, payload: VehicleRequest) =>
      request<VehicleResponse>(`/api/households/${householdId}/vehicles`, {
        method: 'POST',
        body: JSON.stringify(payload)
      }),
    update: (householdId: string, vehicleId: string, payload: VehicleRequest) =>
      request<VehicleResponse>(`/api/households/${householdId}/vehicles/${vehicleId}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      }),
    delete: (householdId: string, vehicleId: string) =>
      request<void>(`/api/households/${householdId}/vehicles/${vehicleId}`, {
        method: 'DELETE'
      })
  },
  maintenanceEvents: {
    list: (householdId: string, vehicleId: string) =>
      request<MaintenanceEventResponse[]>(`/api/households/${householdId}/vehicles/${vehicleId}/maintenance-events`),
    create: (householdId: string, vehicleId: string, payload: MaintenanceEventRequest) =>
      request<MaintenanceEventResponse>(`/api/households/${householdId}/vehicles/${vehicleId}/maintenance-events`, {
        method: 'POST',
        body: JSON.stringify(payload)
      }),
    get: (householdId: string, vehicleId: string, eventId: string) =>
      request<MaintenanceEventResponse>(`/api/households/${householdId}/vehicles/${vehicleId}/maintenance-events/${eventId}`),
    update: (householdId: string, vehicleId: string, eventId: string, payload: MaintenanceEventRequest) =>
      request<MaintenanceEventResponse>(`/api/households/${householdId}/vehicles/${vehicleId}/maintenance-events/${eventId}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      }),
    delete: (householdId: string, vehicleId: string, eventId: string) =>
      request<void>(`/api/households/${householdId}/vehicles/${vehicleId}/maintenance-events/${eventId}`, {
        method: 'DELETE'
      })
  }
};