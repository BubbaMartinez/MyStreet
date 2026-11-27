import { Routes } from '@angular/router';
import { ProductList } from './pages/product-list/product-list';
import { ProductDetail } from './product-detail/product-detail';
import { Login } from './login/login';
import { Register } from './register/register';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';
import { Orders } from './orders/orders';
import { AdminPanel } from './admin-panel/admin-panel';
import { Cart } from './cart/cart';

export const routes: Routes = [
  { path: '', component: ProductList },
  { path: 'products/:id', component: ProductDetail },
  { path: 'cart', component: Cart },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'orders', component: Orders, canActivate: [authGuard] },
  { path: 'admin', component: AdminPanel, canActivate: [adminGuard] },
];