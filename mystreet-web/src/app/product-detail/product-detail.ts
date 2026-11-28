import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product } from '../models/product';
import { ProductService } from '../services/product';
import { CartService } from '../services/cart';

@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);

  product = signal<Product | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  private cart = inject(CartService);

  selectedSize = signal('');
  quantity = signal(1);
  added = signal(false);

    onSizeChange(event: Event) {
    this.selectedSize.set((event.target as HTMLSelectElement).value);
    this.added.set(false);
  }

  changeQuantity(delta: number, max: number) {
    this.quantity.update((q) => Math.min(Math.max(1, q + delta), max));
    this.added.set(false);
  }

  addToCart(product: Product) {
    if (!this.selectedSize()) return;
    this.cart.add(product, this.selectedSize(), this.quantity());
    this.added.set(true);
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('Producto no encontrado');
      this.loading.set(false);
      return;
    }

    this.productService.getById(id).subscribe({
      next: (data) => {
        this.product.set(data);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(
          err.status === 404 ? 'Producto no encontrado' : 'No se pudo cargar el producto'
        );
        this.loading.set(false);
      },
    });
  }
}