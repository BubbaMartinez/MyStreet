import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Product } from '../models/product';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/products`;

  getAll(brand?: string, size?: string): Observable<Product[]> {
    let params = new HttpParams();
    if (brand) params = params.set('brand', brand);
    if (size) params = params.set('size', size);
    return this.http.get<Product[]>(this.baseUrl, { params });
  }

  getById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }
}