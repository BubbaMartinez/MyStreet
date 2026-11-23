import { Routes } from '@angular/router';
import { ProductList } from './pages/product-list/product-list';
import { ProductDetail } from './product-detail/product-detail';


export const routes: Routes = [
  { path: '', component: ProductList },
  { path: 'products/:id', component: ProductDetail },
];