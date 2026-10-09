# SHOPIKU

**Belanja Mudah, Hidup Lebih Baik**

Aplikasi marketplace Android native berbasis Kotlin yang menampilkan alur belanja digital end-to-end: katalog produk, keranjang, checkout, hingga simulasi pembayaran Virtual Account. Antarmuka mengikuti pola marketplace modern dengan Material Design dan backend Supabase (PostgREST).

---

## Tentang Proyek

SHOPIKU adalah aplikasi e-commerce mobile untuk demonstrasi dan pembelajaran arsitektur Android modern. Pengguna dapat menjelajahi produk, mengelola keranjang, membuat pesanan, dan menjalankan skenario pembayaran (berhasil, menunggu, atau kedaluwarsa).

Data produk, keranjang, dan pesanan diambil dari Supabase melalui REST API. Status sesi login disimpan secara lokal dengan SharedPreferences.

> **Catatan:** Fitur pembayaran bersifat simulasi untuk pengujian UI dan alur aplikasi, bukan transaksi nyata melalui payment gateway.

---

## Fitur Utama

### Autentikasi
- Splash screen dengan pengecekan sesi pengguna
- Login dan register akun
- Persistensi status login (ingat sesi)

### Beranda & Katalog
- Banner promo dan wallet strip (SHOPIKUPay / SHOPIKUKoin)
- Flash sale dengan indikator stok
- Kategori produk (elektronik, fashion, kecantikan, dll.)
- Filter rekomendasi (Semua, Terlaris, Diskon, Lokal)
- Pencarian produk
- Pull-to-refresh

### Produk
- Daftar produk dengan RecyclerView
- Detail produk (varian warna/ukuran, jumlah, info penjual)
- Tambah ke keranjang & beli sekarang
- Ulasan pembeli dengan rating

### Keranjang & Checkout
- Kelola item keranjang (jumlah, hapus, pilih semua)
- Konfirmasi pesanan dan alamat pengiriman
- Pilihan kurir (JNE, J&T, SiCepat, AnterAja)
- Ringkasan pembayaran (subtotal, ongkir, diskon)

### Pembayaran (Simulasi)
| Skenario | Hasil |
|----------|--------|
| Berhasil | Menampilkan halaman sukses |
| Menunggu | Status pending + Virtual Account |
| Kedaluwarsa | Batas waktu habis + opsi coba lagi |

### Pesanan & Profil
- Pelacakan pesanan (timeline status)
- Profil pengguna, wishlist, riwayat transaksi
- Bottom navigation: Beranda, Kategori, Keranjang, Pesanan, Profil

---

## Teknologi

| Kategori | Stack |
|----------|--------|
| Bahasa | Kotlin 2.0 |
| UI | XML Layout, Material Design 3 |
| Binding | View Binding, Data Binding |
| Arsitektur | MVVM |
| Async | Kotlin Coroutines, StateFlow, LiveData |
| Networking | Retrofit 2, OkHttp, Gson |
| Backend | Supabase (PostgREST REST API) |
| Supabase SDK | supabase-kt (Postgrest), Ktor Client |
| Image | Glide, Coil |
| List | RecyclerView |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |
| Compile SDK | 37 |
| JDK | 11 |

---

## Arsitektur

Aplikasi mengikuti pola **MVVM** dengan pemisahan lapisan yang jelas:

```
┌─────────────────────────────────────────┐
│  UI Layer (Activity / Adapter)          │
│  Observasi UiState & navigasi           │
├─────────────────────────────────────────┤
│  ViewModel                              │
│  StateFlow / SharedFlow / LiveData      │
├─────────────────────────────────────────┤
│  Repository                             │
│  Orkestrasi data & mapping response     │
├─────────────────────────────────────────┤
│  Data Layer                             │
│  Retrofit API · Model · Session Store   │
│  Supabase PostgREST                     │
└─────────────────────────────────────────┘
```

### Alur belanja

```
Splash → Login/Register → Home
  → Product Detail → Cart → Checkout → Payment
      → Success | Pending | Expired
  → Tracking / Profile
```

### State management

State UI dikelola dengan sealed class `UiState`:

- `Loading` — proses sedang berjalan
- `Success<T>` — data berhasil dimuat
- `Error` — gagal dengan pesan
- `Empty` — tidak ada data

---

## Struktur Direktori

```text
app/src/main/java/com/example/shopiku/
├── data/
│   ├── common/          # UiState
│   ├── model/           # Product, Cart, Checkout, Payment, Review, dll.
│   ├── remote/          # RetrofitClient, API services, SupabaseClient
│   └── repository/      # Product, Cart, Checkout, Payment, Variant
├── ui/
│   ├── adapter/         # Product, Cart, Checkout, FlashSale, Review
│   ├── view/            # Activity (Splash, Home, Cart, Payment, dll.)
│   └── viewmodel/       # Product, Cart, Checkout, Payment, Search
└── res/
    ├── layout/          # XML layouts
    ├── drawable/        # Ikon & background
    ├── menu/            # Bottom navigation
    └── values/          # strings, colors, themes
```

### Layar utama

| Activity | Fungsi |
|----------|--------|
| `SplashActivity` | Launch screen & cek sesi |
| `LoginActivity` / `RegisterActivity` | Autentikasi |
| `HomeActivity` | Beranda marketplace |
| `CategoriesActivity` | Daftar kategori |
| `ProductsActivity` | Daftar produk |
| `ProductDetailActivity` | Detail & varian produk |
| `ReviewsActivity` | Ulasan produk |
| `CartActivity` | Keranjang belanja |
| `CheckoutActivity` | Konfirmasi pesanan |
| `PaymentActivity` | Simulasi pembayaran VA |
| `PaymentSuccessActivity` | Pembayaran berhasil |
| `PaymentPendingActivity` | Menunggu pembayaran |
| `PaymentExpiredActivity` | Pembayaran kedaluwarsa |
| `TrackingActivity` | Lacak pesanan |
| `ProfileActivity` | Akun pengguna |

---

## Persyaratan

- [Android Studio](https://developer.android.com/studio) (versi yang mendukung AGP 9.x)
- Android SDK sesuai `compileSdk` / `targetSdk` proyek
- JDK 11
- Emulator Android atau perangkat fisik (API 24+)
- Koneksi internet (untuk API Supabase)

---

## Cara Menjalankan

1. Clone repositori:
   ```bash
   git clone <url-repositori>
   cd Shopiku
   ```
2. Buka folder proyek di **Android Studio**.
3. Tunggu **Gradle Sync** selesai.
4. Pastikan SDK dan JDK sesuai konfigurasi proyek.
5. Jalankan aplikasi pada emulator atau perangkat (Run ▶).

---

## Konfigurasi API

Backend menggunakan **Supabase PostgREST**. Base URL dan API key dikonfigurasi di:

- `data/remote/RetrofitClient.kt`
- `data/remote/SupabaseClient.kt`

Endpoint utama yang dipakai aplikasi:

| Resource | Endpoint | Keterangan |
|----------|----------|------------|
| Produk | `GET /products` | Katalog & detail |
| Ulasan | `GET /reviews` | Review per produk |
| Keranjang | `GET/POST/PUT/DELETE /cart` | CRUD keranjang |
| Pesanan | `GET/PATCH /orders` | Checkout & status pembayaran |
| Varian | Product variant API | Warna / ukuran |

**Tips emulator:** `localhost` mengarah ke emulator, bukan mesin host. Gunakan URL Supabase publik atau alamat host yang sesuai.

Jangan commit kredensial rahasia (service role key, password) ke repositori publik. Gunakan publishable/anon key sesuai kebijakan Supabase, dan atur Row Level Security di dashboard.

---

## Pengujian Manual

Sebelum demo atau rilis, pastikan:

- [ ] Aplikasi membuka tanpa crash dari splash
- [ ] Login / register dan persistensi sesi berfungsi
- [ ] Home menampilkan produk, flash sale, dan kategori
- [ ] Detail produk, keranjang, dan checkout berjalan
- [ ] Simulasi pembayaran: sukses, pending, dan expired
- [ ] Tracking dan profil dapat diakses dari bottom nav
- [ ] Empty state dan error jaringan ditangani dengan baik
- [ ] Layout layak di layar kecil dan besar

Untuk debug crash, gunakan **Logcat** dan cari `FATAL EXCEPTION`.

---

## Status Pengembangan

| Area | Status |
|------|--------|
| UI marketplace (XML) | Aktif |
| MVVM + Repository | Aktif |
| Integrasi Supabase (produk, cart, order) | Aktif |
| Simulasi pembayaran VA | Aktif |
| Payment gateway produksi | Belum |

Versi aplikasi: **1.0.0** (`versionCode` 1).

---

## Download APK

Ingin mencoba aplikasi SHOPIKU tanpa membuka Android Studio? Unduh APK melalui tautan berikut.

**[⬇️ Download SHOPIKU APK (app-debug.apk)](./app-debug.apk)**

### Cara Instalasi

1. Unduh file `app-debug.apk` melalui tautan di atas.
2. Buka file APK pada perangkat Android.
3. Jika diminta, izinkan instalasi aplikasi dari sumber tersebut melalui pengaturan Android.
4. Lanjutkan instalasi dan buka SHOPIKU.

**Catatan:**

* APK ini merupakan versi debug untuk pengujian.
* Pastikan file `app-debug.apk` tersedia di direktori utama repositori, sejajar dengan `README.md`.
* Jika file APK berada di folder lain, sesuaikan tautan berdasarkan lokasi file tersebut.
* Untuk distribusi publik, sebaiknya gunakan APK release yang telah diuji dan ditandatangani dengan benar.
