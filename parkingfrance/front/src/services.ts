import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Error,
  ParkingAvailability,
  Parking,
  ParkingSpaceType,
  ParkingAvailabilityPrediction,
  PredictionRequestArounds,
  PredictionRequestParking,
  UserPosition,
  ParkingInfo,
  EquipmentRequest,
  Topology,
} from './models';

const API_URL = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class ParkingService {
  constructor(private http: HttpClient) {}

  getParkingAvailability(id: number, spaceType?: ParkingSpaceType): Observable<ParkingAvailability> {
    let params = new HttpParams();
    if (spaceType) params = params.set('spaceType', spaceType);
    return this.http.get<ParkingAvailability>(`${API_URL}/parkings/v1/${id}/availability`, { params });
  }

  getParkingsByCity(city: string): Observable<Parking[]> {
    return this.http.get<Parking[]>(`${API_URL}/parkings/v1/city/${city}`);
  }

  getParkingEquipments(id: number): Observable<Parking> {
    return this.http.get<Parking>(`${API_URL}/parkings/v1/${id}/equipments`);
  }

  postParkingPrediction(id: number, body: PredictionRequestParking): Observable<ParkingAvailabilityPrediction[]> {
    return this.http.post<ParkingAvailabilityPrediction[]>(`${API_URL}/parkings/v1/${id}/prediction`, body);
  }

  postParkingsArounds(body: UserPosition): Observable<ParkingAvailability[]> {
    return this.http.post<ParkingAvailability[]>(`${API_URL}/parkings/v1/arounds`, body);
  }

  postParkingsAroundsPrediction(body: PredictionRequestArounds): Observable<ParkingAvailabilityPrediction[]> {
    return this.http.post<ParkingAvailabilityPrediction[]>(`${API_URL}/parkings/v1/arounds/prediction`, body);
  }
}

@Injectable({ providedIn: 'root' })
export class AdminParkingService {
  constructor(private http: HttpClient) {}

  private getAuthHeaders(apiKey: string): HttpHeaders {
    return new HttpHeaders({ 'X-API-KEY': apiKey });
  }

  getAllParkings(apiKey: string): Observable<Parking[]> {
    return this.http.get<Parking[]>(`${API_URL}/admin/v1/parkings`, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  createParking(apiKey: string, parking: Parking): Observable<any> {
    return this.http.post(`${API_URL}/admin/v1/parkings`, parking, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  getParking(apiKey: string, id: number): Observable<Parking> {
    return this.http.get<Parking>(`${API_URL}/admin/v1/parkings/${id}`, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateParking(apiKey: string, id: number, parking: Parking): Observable<Parking> {
    return this.http.patch<Parking>(`${API_URL}/admin/v1/parkings/${id}`, parking, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  deleteParking(apiKey: string, id: number): Observable<any> {
    return this.http.delete(`${API_URL}/admin/v1/parkings/${id}`, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateEquipments(apiKey: string, id: number, info: ParkingInfo): Observable<any> {
    return this.http.put(`${API_URL}/admin/v1/parkings/${id}/equipments`, info, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateTypes(apiKey: string, id: number, info: ParkingInfo): Observable<any> {
    return this.http.put(`${API_URL}/admin/v1/parkings/${id}/types`, info, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateArea(apiKey: string, id: number, topology: Topology): Observable<any> {
    return this.http.put(`${API_URL}/admin/v1/parkings/${id}/area`, topology, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  // v2 endpoints
  updateParkingV2(apiKey: string, id: number, parking: Parking): Observable<Parking> {
    return this.http.put<Parking>(`${API_URL}/admin/v2/parkings/${id}`, parking, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateParkingName(apiKey: string, id: number, name: string): Observable<Parking> {
    return this.http.patch<Parking>(`${API_URL}/admin/v2/parkings/${id}/name`, { name }, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateParkingAddress(apiKey: string, id: number, address: string): Observable<Parking> {
    return this.http.patch<Parking>(`${API_URL}/admin/v2/parkings/${id}/address`, { address }, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateParkingPrice(apiKey: string, id: number, price: number): Observable<Parking> {
    return this.http.patch<Parking>(`${API_URL}/admin/v2/parkings/${id}/price`, { price }, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateEquipmentsV2(apiKey: string, id: number, req: EquipmentRequest): Observable<Parking> {
    return this.http.patch<Parking>(`${API_URL}/admin/v2/parkings/${id}/equipments`, req, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateSpaceType(apiKey: string, parkingId: number, spaceId: number, type: string): Observable<any> {
    return this.http.patch(`${API_URL}/admin/v2/parkings/${parkingId}/space/${spaceId}/types`, { type }, {
      headers: this.getAuthHeaders(apiKey),
    });
  }

  updateSpaceArea(apiKey: string, parkingId: number, spaceId: number, topology: Topology): Observable<any> {
    return this.http.patch(`${API_URL}/admin/v2/parkings/${parkingId}/space/${spaceId}/area`, topology, {
      headers: this.getAuthHeaders(apiKey),
    });
  }
}
