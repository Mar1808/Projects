import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.css'
})
export class AdminLoginComponent {

  apiKey: string = '';
  constructor(private router: Router) { 
    
  }
  

  login() {
    // Logique de connexion avec la clé API
    sessionStorage.setItem('apiKey', this.apiKey);
    console.log('Tentative de connexion avec la clé API :', this.apiKey);
    this.router.navigate(['/admin']);
  }
}
