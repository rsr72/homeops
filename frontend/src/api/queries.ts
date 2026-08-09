import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { homeOpsApi } from './homeopsApi';
import type { HouseholdRequest, MaintenanceEventRequest, VehicleRequest } from '../types';

export const householdQueryKey = ['households'] as const;

export const vehicleQueryKey = (householdId: string) => ['vehicles', householdId] as const;

export const maintenanceEventQueryKey = (householdId: string, vehicleId: string) =>
  ['maintenance-events', householdId, vehicleId] as const;

export function useHouseholdsQuery() {
  return useQuery({
    queryKey: householdQueryKey,
    queryFn: homeOpsApi.households.list
  });
}

export function useCreateHouseholdMutation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: HouseholdRequest) => homeOpsApi.households.create(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: householdQueryKey });
    }
  });
}

export function useVehiclesQuery(householdId: string | undefined) {
  return useQuery({
    queryKey: householdId ? vehicleQueryKey(householdId) : ['vehicles', 'empty'],
    queryFn: () => homeOpsApi.vehicles.list(householdId as string),
    enabled: Boolean(householdId)
  });
}

export function useCreateVehicleMutation(householdId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: VehicleRequest) => homeOpsApi.vehicles.create(householdId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: vehicleQueryKey(householdId) });
    }
  });
}

export function useUpdateVehicleMutation(householdId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ vehicleId, payload }: { vehicleId: string; payload: VehicleRequest }) =>
      homeOpsApi.vehicles.update(householdId, vehicleId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: vehicleQueryKey(householdId) });
    }
  });
}

export function useDeleteVehicleMutation(householdId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (vehicleId: string) => homeOpsApi.vehicles.delete(householdId, vehicleId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: vehicleQueryKey(householdId) });
    }
  });
}

export function useMaintenanceEventsQuery(householdId: string | undefined, vehicleId: string | undefined) {
  return useQuery({
    queryKey: householdId && vehicleId ? maintenanceEventQueryKey(householdId, vehicleId) : ['maintenance-events', 'empty'],
    queryFn: () => homeOpsApi.maintenanceEvents.list(householdId as string, vehicleId as string),
    enabled: Boolean(householdId && vehicleId)
  });
}

export function useCreateMaintenanceEventMutation(householdId: string, vehicleId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: MaintenanceEventRequest) => homeOpsApi.maintenanceEvents.create(householdId, vehicleId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: maintenanceEventQueryKey(householdId, vehicleId) });
    }
  });
}

export function useUpdateMaintenanceEventMutation(householdId: string, vehicleId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ eventId, payload }: { eventId: string; payload: MaintenanceEventRequest }) =>
      homeOpsApi.maintenanceEvents.update(householdId, vehicleId, eventId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: maintenanceEventQueryKey(householdId, vehicleId) });
    }
  });
}

export function useDeleteMaintenanceEventMutation(householdId: string, vehicleId: string) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (eventId: string) => homeOpsApi.maintenanceEvents.delete(householdId, vehicleId, eventId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: maintenanceEventQueryKey(householdId, vehicleId) });
    }
  });
}