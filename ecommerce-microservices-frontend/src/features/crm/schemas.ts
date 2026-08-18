import { z } from 'zod';

// ─── Profile Form Schema ───────────────────────────────────────────────────────
export const profileSchema = z.object({
  firstName: z
    .string()
    .min(2, 'Ad en az 2 karakter olmalı')
    .max(50, 'Maks 50 karakter'),
  lastName: z
    .string()
    .min(2, 'Soyad en az 2 karakter olmalı')
    .max(50, 'Maks 50 karakter'),
  phoneNumber: z
    .string()
    .regex(/^(\+90|0)?[5][0-9]{9}$/, 'Geçerli bir Türkiye telefon numarası giriniz')
    .optional()
    .or(z.literal('')),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;

// ─── Address Form Schema ───────────────────────────────────────────────────────
export const addressSchema = z.object({
  title: z.string().min(1, 'Adres başlığı zorunlu').max(50, 'Maks 50 karakter'),
  street: z.string().min(5, 'Açık adres zorunlu').max(200, 'Maks 200 karakter'),
  city: z.string().min(2, 'Şehir zorunlu').max(100, 'Maks 100 karakter'),
  state: z.string().min(2, 'İlçe/Eyalet zorunlu').max(100, 'Maks 100 karakter'),
  zipCode: z
    .string()
    .regex(/^\d{5}$/, 'Posta kodu 5 haneli olmalı')
    .optional()
    .or(z.literal('')),
  country: z.string().min(2, 'Ülke zorunlu').max(100, 'Maks 100 karakter'),
});

export type AddressFormValues = z.infer<typeof addressSchema>;
