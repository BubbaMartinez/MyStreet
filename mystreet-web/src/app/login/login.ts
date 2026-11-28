import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submitting = signal(false);
  error = signal<string | null>(null);

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
                  const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
                  const safe = returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//');
                  this.router.navigateByUrl(safe ? returnUrl : '/');
                },
      error: (err: HttpErrorResponse) => {
        this.error.set(
          err.status === 401
            ? 'Correo o contraseña incorrectos'
            : 'No se pudo iniciar sesión. Inténtalo de nuevo.'
        );
        this.submitting.set(false);
      },
    });
  }
}