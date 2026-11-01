import { AdminParkingService } from '../../../services';
import { Parking, ParkingInfo, Address, Space, ParkingSpaceType } from '../../../models';
import { FormsModule } from '@angular/forms';
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from "@angular/router";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './admin.component.html',
  styles: `
  .admin-container { max-width: 1000px; margin: auto; padding: 2rem; }
  .api-key-section { margin-bottom: 1rem; display:flex; gap:.5rem; align-items:center; }
  .error { color: #b00020; margin: .75rem 0; }
  .parkings-list ul { list-style: none; padding: 0; margin:0; }
  .parkings-list li { padding: 0.5rem .75rem; border-bottom: 1px solid #ddd; display:flex; justify-content:space-between; align-items:center; }
  form.create-form { display:grid; gap:.75rem; grid-template-columns:repeat(auto-fill,minmax(200px,1fr)); background:#f9f9f9; padding:1rem; border-radius:8px; margin-top:1rem; }
  form.create-form fieldset { border:1px solid #ddd; padding:.75rem; border-radius:6px; grid-column:1/-1; }
  form.create-form button[type=submit] { grid-column:1/-1; justify-self:start; }
  .spaces-config { display:flex; flex-wrap:wrap; gap:.5rem; }
  .space-item { background:#eef; padding:.5rem .75rem; border-radius:6px; display:flex; flex-direction:column; gap:.25rem; }
  label { font-weight:600; font-size:.85rem; }
  input, select { padding:.4rem .5rem; border:1px solid #ccc; border-radius:4px; width:100%; }
  h3 { margin-top:2rem; }
  .actions { display:flex; gap:.5rem; }
  `
})
export class AdminComponent {
  apiKey: string = '';
  parkings: Parking[] = [];
  error?: string;
  loadingList = false;
  creating = false;

  // Form model for new parking
  newParking: Parking = this.buildEmptyParking();
  equipmentsInput: string = '';
  spacesCount: number = 0;
  tempClosedAll: boolean = false;

   spaceTypes: ParkingSpaceType[] = ['car','e_car','disabled','truck','bike','motorcycle','bus','van'];

  equipmentEnum: string[] = [
    'Toilets',
    'Elevator',
    'Video_surveillance',
    'EV_charging_station',
    'Bike_station',
    'Motorcycle_station',
    'Disabled'
  ];
  equipmentChecked: boolean[] = new Array(this.equipmentEnum.length).fill(false);

  adminService : AdminParkingService = inject(AdminParkingService);
   constructor(private router: Router) {
    this.apiKey = sessionStorage.getItem('apiKey') || '';
    if (this.apiKey === '') {
      // Rediriger vers la page de connexion si la clé API est manquante
      console.log('Clé API manquante, redirection vers la page de connexion.');
      this.router.navigate(['/login']);
    }
  }

  private buildEmptyParking(): Parking {
    return {
      //id: 0, // l'API exigera peut-être d'ignorer cet id côté serveur
      name: '',
      address: this.buildEmptyAddress(),
      information: this.buildEmptyInfo()
    };
  }

  private buildEmptyAddress(): Address {
    return { street: '', city: '', postalCode: '', country: '' };
  }

  private buildEmptyInfo(): ParkingInfo {
    return { places: [], equipments: [], price: 0 };
  }

  refreshSpaces() {
    // Recrée le tableau des places selon spacesCount
    const places: Space[] = [];
    for (let i = 0; i < this.spacesCount; i++) {

      places.push({ type: 'car', tempClosedSpace: this.tempClosedAll,
        topology: { zone: 'A'+(i + 1), niveau: Math.floor(Math.random() * 3) + 1, position: { latitude: Math.random() * 90, longitude: Math.random() * 180 } }
      });
    }
    this.newParking.information.places = places;
  }

  updateSpaceType(index: number, type: ParkingSpaceType) {
    if (this.newParking.information.places[index]) {
      this.newParking.information.places[index].type = type;
    }
  }

  loadParkings() {
    if (!this.apiKey) { this.error = 'Clé API requise.'; return; }
    this.loadingList = true;
    this.adminService.getAllParkings(this.apiKey).subscribe({
      next: (data) => {
        this.parkings = data;
        this.error = undefined;
        this.loadingList = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Erreur lors du chargement des parkings.';
        this.loadingList = false;
      }
    });
  }

  submitCreateParking() {
    if (!this.apiKey) { this.error = 'Clé API requise pour créer.'; return; }
    if (!this.newParking.name || !this.newParking.address.street || !this.newParking.address.city) {
      this.error = 'Champs obligatoires manquants.';
      return;
    }
    console.log(this.newParking);
    this.creating = true;
    this.adminService.createParking(this.apiKey, this.newParking).subscribe({
      next: () => {
        this.creating = false;
        this.error = undefined;
        this.resetForm();
        this.loadParkings();
      },
      error: (err) => {
        this.creating = false;
        this.error = err?.error?.message || 'Erreur lors de la création du parking.';
      }
    });
  }

  resetForm() {
    this.newParking = this.buildEmptyParking();
    this.equipmentsInput = '';
    this.spacesCount = 0;
    this.tempClosedAll = false;
  }

  toggleEquipment(eq: string, checked: boolean) {
    const arr = this.newParking.information.equipments || [];
    if (checked) {
      if (!arr.includes(eq as any)) arr.push(eq as any);
    } else {
      const idx = arr.indexOf(eq as any);
      if (idx > -1) arr.splice(idx, 1);
    }
    this.newParking.information.equipments = arr;
  }
  checked(index: number) {
    this.equipmentChecked[index] = !this.equipmentChecked[index];
    this.toggleEquipment(this.equipmentEnum[index], this.equipmentChecked[index]);
  }
}
