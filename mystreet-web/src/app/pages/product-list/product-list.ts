import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Product } from '../../models/product';
import { ProductService } from '../../services/product';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-list.html',
})

export class ProductList implements OnInit {
  private productService = inject(ProductService);

  products = signal<Product[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  
  brands = signal<string[]>([]);        // marcas disponibles para el selector
  selectedBrand = signal('');           // marca elegida ('' = todas)
  
  sizes = signal<string[]>([]);         // tallas disponibles para el selector
  selectedSize = signal('');            // talla elegida ('' = todas)
  


ngOnInit() {
  this.load();
}

private load() {
  this.loading.set(true);
  this.error.set(null);

  this.productService
    .getAll(this.selectedBrand() || undefined, this.selectedSize() || undefined)
    .subscribe({
      next: (data) => {
        this.products.set(data);
        if (this.brands().length === 0) {
          this.brands.set([...new Set(data.map((p) => p.brand))].sort());
        }
        if (this.sizes().length === 0) {
          this.sizes.set(
            [...new Set(data.flatMap((p) => p.sizes))].sort((a, b) => Number(a) - Number(b))
          );
        }
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el catálogo');
        this.loading.set(false);
      },
    });
}

onBrandChange(event: Event) {
  this.selectedBrand.set((event.target as HTMLSelectElement).value);
  this.load();
}

onSizeChange(event: Event) {
  this.selectedSize.set((event.target as HTMLSelectElement).value);
  this.load();
}

clearFilters() {
  this.selectedBrand.set('');
  this.selectedSize.set('');
  this.load();
}

}