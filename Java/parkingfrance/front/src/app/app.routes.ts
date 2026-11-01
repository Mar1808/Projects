import { Routes } from '@angular/router';
import { AdminComponent } from './pages/admin/admin.component';
import { UserComponent } from './pages/user/user.component';
import { ParkingUpdateComponent } from './components/parking-update/parking-update.component';
import { ParkingComponent } from './components/parking/parking.component';
import { AdminLoginComponent } from './components/admin-login/admin-login.component';

// Configuration des routes de l'application.
// ''    : redirection initiale vers /admin
// admin : interface administrateur
// user  : interface utilisateur
// **    : wildcard (toutes les autres URLs) -> redirection /admin
export const routes: Routes = [
//   { path: '', redirectTo: 'admin', pathMatch: 'full' },
  { path: 'admin', component: AdminComponent },
  { path: 'login', component: AdminLoginComponent },
  { path: 'admin/parking/:id', component: ParkingComponent },
  { path: 'admin/parking/edit/:id', component: ParkingUpdateComponent },
  { path: 'user', component: UserComponent },
  { path: '**', redirectTo: 'user' }
];
