export interface ApiValidationError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  validationErrors: ApiValidationError[];
}

export interface HouseholdRequest {
  name: string;
  notes?: string;
}

export interface HouseholdResponse {
  id: string;
  name: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VehicleRequest {
  make: string;
  model: string;
  year: number;
  vin?: string;
  notes?: string;
  purchaseDate?: string;
  purchaseCost?: number;
  currentMileage?: number;
}

export interface VehicleResponse {
  id: string;
  householdId: string;
  make: string;
  model: string;
  year: number;
  vin: string | null;
  notes: string | null;
  purchaseDate: string | null;
  purchaseCost: number | null;
  currentMileage: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface MaintenanceEventRequest {
  serviceDate: string;
  description: string;
  mileage?: number;
  cost?: number;
  notes?: string;
}

export interface MaintenanceEventResponse {
  id: string;
  householdId: string;
  vehicleId: string;
  serviceDate: string;
  description: string;
  mileage: number | null;
  cost: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface HomeOpsErrorBody {
  status: number;
  error: string;
  message: string;
  validationErrors?: ApiValidationError[];
}