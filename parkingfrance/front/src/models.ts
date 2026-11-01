// Models generated from OpenAPI schemas (swagger.yaml)
// Date: 25 octobre 2025

export interface Error {
  code: string;
  message: string;
}

export interface Position {
  latitude: number;
  longitude: number;
}

export interface Address {
  street: string;
  postalCode: string;
  city: string;
  country: string;
  position?: Position;
}

export type ParkingSpaceType =
  | 'car'
  | 'e_car'
  | 'disabled'
  | 'truck'
  | 'bike'
  | 'motorcycle'
  | 'bus'
  | 'van';

export interface Topology {
  zone?: string;
  niveau?: number;
  position?: Position;
}

export interface Space {
  id?: number;
  type: ParkingSpaceType;
  tempClosedSpace: boolean;
  topology?: Topology;
}

export type Equipment =
  | 'Toilets'
  | 'Elevator'
  | 'Video_surveillance'
  | 'EV_charging_station'
  | 'Bike_station'
  | 'Motorcycle_station'
  | 'Disabled';

export interface ParkingInfo {
  price?: number;
  places: Space[];
  equipments?: Equipment[];
}

export interface Parking {
  id?: number;
  name: string;
  address: Address;
  information: ParkingInfo;
}

export interface ParkingAvailability {
  parking?: Parking;
  available: number;
  occupied: number;
  capacity: number;
}

export interface UserPosition {
  vehicle: ParkingSpaceType;
  position: Position;
  radius: number;
}

export interface PredictionRequestArounds {
  userPosition: UserPosition;
  targetDate: string;
}

export interface PredictionRequestParking {
  vehicle: ParkingSpaceType;
  targetDate: string;
}

export interface ParkingPrediction {
  id: number;
  name: string;
  address: Address;
}

export interface ParkingAvailabilityPrediction {
  parking?: ParkingPrediction;
  available: number;
  occupied: number;
  capacity: number;
}

export interface EquipmentRequest {
  equipments: Equipment[];
}
