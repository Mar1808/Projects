import { AdminParkingService } from '../../../services';
import { Parking, ParkingSpaceType } from '../../../models';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-parking-update',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './parking-update.component.html',
  styleUrl: './parking-update.component.css'
})
export class ParkingUpdateComponent {

  parkingId: number | null = null; 
  apiKey: string = '';
  parking : Parking = {
      address: { street: '', postalCode: '', city: '', country: '' },
      name: '',
      information: {
        places: [],
        equipments: [],
        price: 0
      }
  };
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

  adminParkingService : AdminParkingService = inject(AdminParkingService);
  // Autres propriétés du composant 
  constructor(private route: ActivatedRoute) { 
    const routeParams = this.route.snapshot.paramMap;
    this.parkingId = Number(routeParams.get('id'));
  }

  ngOnInit() {
    if (this.parkingId) {
      this.loadParkingDetails(this.parkingId);
    }
  }
  loadParkingDetails(id: number) {
    // Utilisez le service AdminParkingService pour charger les détails du parking
    this.adminParkingService.getParking(this.apiKey, id).subscribe(parking => {
      console.log('Détails du parking:', parking);
      // Mettez à jour les propriétés du composant avec les détails du parking
      this.parking = parking;
    });
  } 

    updateSpaceType(index: number, type: ParkingSpaceType) {
      if (this.parking.information.places[index]) {
        this.parking.information.places[index].type = type;
      }
    }

}
