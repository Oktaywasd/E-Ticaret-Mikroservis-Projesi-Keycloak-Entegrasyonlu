import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface CartItem {
  productId: string;
  name: string;
  price: number;
  imageUrl?: string;
  quantity: number;
  variant?: string;
  stock: number;
}

interface CartState {
  items: CartItem[];
  addItem: (item: CartItem) => void;
  removeItem: (productId: string, variant?: string) => void;
  updateQuantity: (productId: string, quantity: number, variant?: string) => void;
  clearCart: () => void;
  totalPrice: () => number;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],

      addItem: (newItem) => {
        set((state) => {
          const existing = state.items.find(
            (i) => i.productId === newItem.productId && i.variant === newItem.variant
          );
          if (existing) {
            return {
              items: state.items.map((i) => {
                if (i.productId === newItem.productId && i.variant === newItem.variant) {
                  const newQuantity = Math.min(i.quantity + newItem.quantity, newItem.stock);
                  return { ...i, quantity: newQuantity, stock: newItem.stock };
                }
                return i;
              }),
            };
          }
          const qty = Math.min(newItem.quantity, newItem.stock);
          return { items: [...state.items, { ...newItem, quantity: qty }] };
        });
      },

      removeItem: (productId, variant) => {
        set((state) => ({
          items: state.items.filter(
            (i) => !(i.productId === productId && i.variant === variant)
          ),
        }));
      },

      updateQuantity: (productId, quantity, variant) => {
        if (quantity <= 0) {
          get().removeItem(productId, variant);
          return;
        }
        set((state) => ({
          items: state.items.map((i) => {
            if (i.productId === productId && i.variant === variant) {
              const clampedQuantity = Math.min(quantity, i.stock ?? quantity);
              return { ...i, quantity: clampedQuantity };
            }
            return i;
          })
        }));
      },

      clearCart: () => set({ items: [] }),

      totalPrice: () =>
        get().items.reduce((acc, i) => acc + i.price * i.quantity, 0),
    }),
    {
      name: 'eshop-cart',
    }
  )
);
