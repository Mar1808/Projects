import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Component, NgModule } from '@angular/core';
import { RouterOutlet, RouterModule } from '@angular/router';




@Component({  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterModule, HttpClientModule],
  template: `
    Welcome to Parking France!
   <a [routerLink]="['/admin']">Admin</a> |
   <a [routerLink]="['/user']">User</a>
    <hr />
    <router-outlet></router-outlet>
  `,
  styles: []
})
export class AppComponent {
  title = 'front';
}
