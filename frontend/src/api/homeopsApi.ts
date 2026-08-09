import { request } from './http';
import type { HouseholdRequest, HouseholdResponse, VehicleRequest, VehicleResponse } from '../types';

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
  }
};