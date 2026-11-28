import { Injectable, computed, signal } from '@angular/core';
import { CartItem } from '../models/cart-item';
import { Product } from '../models/product';

const STORAGE_KEY = 'mystreet.cart';

@Injectable({ providedIn: 'root' })
export class CartService {
  private items = signal<CartItem[]>(this.restore());

  readonly cartItems = this.items.asReadonly();
  readonly count = computed(() => this.items().reduce((n, i) => n + i.quantity, 0));
  readonly total = computed(() => this.items().reduce((sum, i) => sum + i.price * i.quantity, 0));

  add(product: Product, size: string, quantity = 1): void {
    const list = [...this.items()];
    const index = list.findIndex((i) => i.productId === product.id && i.size === size);

    if (index >= 0) {
      const item = list[index];
      list[index] = { ...item, quantity: Math.min(item.quantity + quantity, item.stockQty) };
    } else {
      list.push({
        productId: product.id,
        name: product.name,
        brand: product.brand,
        imageUrl: product.imageUrl,
        price: product.price,
        size,
        quantity: Math.min(quantity, product.stockQty),
        stockQty: product.stockQty,
      });
    }
    this.save(list);
  }

  setQuantity(productId: string, size: string, quantity: number): void {
    if (quantity <= 0) {
      this.remove(productId, size);
      return;
    }
    this.save(
      this.items().map((i) =>
        i.productId === productId && i.size === size
          ? { ...i, quantity: Math.min(quantity, i.stockQty) }
          : i
      )
    );
  }

  remove(productId: string, size: string): void {
    this.save(this.items().filter((i) => !(i.productId === productId && i.size === size)));
  }

  clear(): void {
    this.save([]);
  }

  private save(list: CartItem[]): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    this.items.set(list);
  }

  private restore(): CartItem[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      const parsed = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}