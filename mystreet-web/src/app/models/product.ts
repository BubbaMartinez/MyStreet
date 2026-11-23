export interface Product {
  id: string;
  name: string;
  brand: string;
  description: string;
  price: number;
  imageUrl: string;
  sizes: string[];
  stockQty: number;
}