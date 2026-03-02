# Panduan Penggunaan Aplikasi Dashboard Masjid Baiturrahman

Aplikasi ini dirancang untuk menampilkan informasi masjid secara real-time di layar TV, tablet, atau perangkat Android lainnya — termasuk waktu sholat, jadwal kegiatan, kutipan islami, dan galeri gambar yang dapat diperbarui dari jarak jauh.

---

## Daftar Isi

1. [Tampilan Utama (Dashboard)](#1-tampilan-utama-dashboard)
2. [Mengakses Dashboard Admin](#2-mengakses-dashboard-admin)
3. [Login dan Registrasi](#3-login-dan-registrasi)
4. [Pengaturan Header Masjid](#4-pengaturan-header-masjid)
5. [Pengaturan Waktu Sholat](#5-pengaturan-waktu-sholat)
6. [Pengaturan Kutipan](#6-pengaturan-kutipan)
7. [Manajemen Slide Gambar](#7-manajemen-slide-gambar)
8. [Teks Berjalan (Marquee)](#8-teks-berjalan-marquee)
9. [Tema Tampilan](#9-tema-tampilan)
10. [Menyimpan Perubahan](#10-menyimpan-perubahan)
11. [Perangkat Terhubung](#11-perangkat-terhubung)
12. [Ubah Kata Sandi](#12-ubah-kata-sandi)
13. [Sinkronisasi Antar Perangkat](#13-sinkronisasi-antar-perangkat)
14. [Logout](#14-logout)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Tampilan Utama (Dashboard)

Saat aplikasi dibuka, layar utama menampilkan informasi berikut secara otomatis:

| Elemen | Keterangan |
|--------|-----------|
| **Header Masjid** | Nama masjid, lokasi, dan logo di bagian atas |
| **Jam & Tanggal** | Jam digital real-time beserta tanggal Masehi dan Hijriyah |
| **Slide Gambar** | Gambar yang berganti otomatis setiap 5 detik |
| **Kutipan Islami** | Teks kutipan yang bisa dikustomisasi |
| **Jadwal Waktu Sholat** | Subuh, Syuruk, Dzuhur, Ashar, Maghrib, Isya |
| **Hitung Mundur Sholat** | Sisa waktu menuju waktu sholat berikutnya |
| **Teks Berjalan** | Teks berjalan (marquee) di bagian paling bawah layar |

### Tata letak berdasarkan perangkat

- **TV / Tablet (Landscape):** Slider gambar ada di sisi kiri, panel informasi (jam, kutipan) di sisi kanan, jadwal sholat di bawah.
- **Ponsel (Portrait):** Semua elemen ditampilkan berurutan dari atas ke bawah.

---

## 2. Mengakses Dashboard Admin

Untuk mengubah konten yang ditampilkan, Anda perlu masuk ke **Dashboard Admin**.

1. Dari layar utama, tap ikon **⚙️ (roda gigi)** yang ada di sudut layar.
   - Di TV/tablet: ikon ada di sudut kiri atas area slider gambar.
   - Di ponsel: ikon ada di sudut kanan atas layar.
2. Jika belum login, Anda akan diarahkan ke halaman **Login**.
3. Jika sudah login, Dashboard Admin langsung terbuka.

---

## 3. Login dan Registrasi

### Login

1. Masukkan **Nama Pengguna** dan **Kata Sandi** Anda.
2. Tap tombol **Masuk**.
3. Jika berhasil, Dashboard Admin akan terbuka secara otomatis.
4. Jika gagal, akan muncul pesan error di bagian bawah layar (mis. "Nama pengguna atau kata sandi salah").

> Anda juga bisa menekan tombol **Enter / Done** di keyboard setelah mengisi kata sandi untuk langsung login.

### Registrasi (Akun Baru)

1. Di halaman Login, tap **"Daftar"**.
2. Masukkan nama pengguna baru dan kata sandi.
3. Tap tombol **Daftar**.
4. Jika nama pengguna sudah digunakan orang lain, akan muncul pesan error.

### Kembali ke Dashboard Tanpa Login

Tap tombol **← (panah kembali)** di pojok kiri atas halaman Login untuk kembali ke tampilan utama tanpa login.

---

## 4. Pengaturan Header Masjid

Di bagian **"Pengaturan Header"** pada Admin Dashboard:

### Nama Masjid
- Ketik nama masjid di field **"Nama Masjid"**.
- Batas maksimum: **35 karakter**.
- Penghitung karakter (mis. `12/35`) ada di bawah field; berubah merah saat mendekati batas.

### Lokasi Masjid
- Ketik lokasi atau alamat singkat di field **"Lokasi Masjid"**.
- Batas maksimum: **25 karakter**.

### Logo Masjid
1. Tap tombol **"Ubah Logo"** untuk membuka galeri gambar.
2. Pilih gambar logo dari galeri.
3. Logo akan otomatis diupload dan tersimpan ke semua perangkat.
4. Preview logo saat ini tampil di sebelah kiri tombol.
5. Jika belum ada logo, akan tertulis "Belum ada".

### Menghapus Logo
- Jika logo sudah ada, tombol merah **"Hapus Logo"** muncul di bawah tombol "Ubah Logo".
- Tap tombol tersebut untuk menghapus logo dari semua perangkat.
- Tombol berubah menjadi "Menghapus..." selama proses berlangsung.
- Selama penghapusan berjalan, tombol "Ubah Logo" juga dinonaktifkan sementara.

> Logo tidak memerlukan tap "Simpan Perubahan" — upload dan hapus tersimpan otomatis.

---

## 5. Pengaturan Waktu Sholat

Di bagian **"Pengaturan Waktu Sholat"**:

### Alamat Waktu Sholat

Field alamat mendukung dua cara input:

**Cara 1 — Gunakan GPS (direkomendasikan)**
1. Tap ikon **📍 (lokasi)** di sisi kanan field alamat.
2. Jika diminta izin lokasi, pilih **Izinkan**.
3. Aplikasi akan mendeteksi lokasi perangkat secara otomatis dan mengisi field alamat.
4. Waktu sholat akan dihitung langsung dari koordinat GPS — lebih akurat dibanding pencarian teks.

> Jika GPS tidak aktif atau perangkat tidak memiliki GPS (mis. TV), akan muncul notifikasi "GPS tidak aktif atau tidak tersedia".

**Cara 2 — Cari lokasi manual**
1. Ketik nama kota atau alamat di field (minimal 3 karakter).
2. Tunggu sebentar; daftar saran lokasi akan muncul di bawah field.
3. Pilih salah satu lokasi dari daftar saran.

> **Penting:** Tombol "Simpan Perubahan" hanya aktif jika lokasi dikonfirmasi via GPS atau dipilih dari daftar saran. Mengetik bebas tanpa memilih dari daftar akan memunculkan pesan error di field dan menonaktifkan tombol simpan.

### Zona Waktu
Pilih zona waktu sesuai lokasi masjid dari dropdown:

| Pilihan | Wilayah |
|---------|---------|
| `Asia/Jakarta` | WIB — Jawa, Sumatera, Kalimantan Barat & Tengah |
| `Asia/Pontianak` | WIB — Pontianak (tidak berubah musim) |
| `Asia/Makassar` | WITA — Kalimantan Timur, Sulawesi, Bali, NTB, NTT |
| `Asia/Jayapura` | WIT — Maluku, Papua |

> Perubahan zona waktu di satu perangkat akan otomatis memperbarui waktu sholat di semua perangkat yang terhubung dalam hitungan detik (tanpa perlu restart).

---

## 6. Pengaturan Kutipan

Di bagian **"Pengaturan Kutipan"**:

- Ketik kutipan islami atau pesan yang ingin ditampilkan di layar utama.
- Batas maksimum: **150 karakter**.
- Kutipan bisa berupa ayat Al-Qur'an, hadis, atau pesan kegiatan masjid.

---

## 7. Manajemen Slide Gambar

Di bagian **"Slide Gambar (640 x 410) (Maks 5)"**:

Aplikasi mendukung hingga **5 gambar** yang ditampilkan bergantian di layar utama.

### Menambah Gambar

1. Tap tombol **"Tambah Gambar (X/5)"**.
2. Jika diminta izin akses galeri, pilih **Izinkan**.
3. Pilih gambar dari galeri perangkat.
4. Gambar akan diupload; tombol berubah menjadi "Mengupload..." selama proses.
5. Setelah selesai, thumbnail gambar muncul di baris galeri Admin.

> Ukuran gambar yang direkomendasikan: **640 × 410 piksel** (rasio landscape).

### Menghapus Gambar

1. Tap ikon **X** di sudut kanan atas thumbnail gambar.
2. Gambar akan dihapus dari Supabase Storage dan diperbarui di semua perangkat.
3. Gambar yang tersisa secara otomatis diurutkan ulang.

### Catatan Penting

- Setelah gambar ke-5 ditambahkan, tombol upload disembunyikan dan muncul teks **"Jumlah maksimum gambar tercapai (5/5)"**.
- Saat upload atau hapus sedang berjalan, semua tombol gambar dinonaktifkan sementara.
- Gambar **tersinkronisasi otomatis** ke semua perangkat yang terhubung.

---

## 8. Teks Berjalan (Marquee)

Di bagian **"Teks Berjalan"**:

- Ketik pengumuman atau informasi yang ingin ditampilkan sebagai teks berjalan di bagian bawah layar.
- Batas maksimum: **200 karakter**.
- Contoh: `Pengajian Rutin setiap Kamis malam pukul 19.30 WIB | Shalat Jumat pukul 12.00 WIB`

---

## 9. Tema Tampilan

Di bagian **"Tema Tampilan"**:

- Aktifkan toggle **"Mode Gelap"** untuk beralih ke tampilan gelap (dark mode).
- Matikan toggle untuk kembali ke tampilan terang (light mode).
- Preferensi tema tersimpan secara lokal dan diingat saat app dibuka kembali.

> Perubahan tema berlaku langsung tanpa perlu menyimpan atau restart.

---

## 10. Menyimpan Perubahan

Setelah mengubah pengaturan apa pun di bagian Header, Waktu Sholat, Kutipan, atau Teks Berjalan:

1. Scroll ke bawah di Admin Dashboard.
2. Tap tombol hijau **"Simpan Perubahan"**.
3. Tombol berubah menjadi "Menyimpan..." selama proses berlangsung.
4. Setelah selesai, muncul pesan **"Pengaturan disimpan"** di bagian bawah layar.

> **Perubahan logo dan gambar slide disimpan otomatis** — tidak perlu tap "Simpan Perubahan".

---

## 11. Perangkat Terhubung

Di bagian **"Perangkat Terhubung"** (scroll ke bawah setelah tombol Simpan):

Bagian ini menampilkan daftar semua perangkat yang sedang login ke akun yang sama.

- Perangkat yang sedang Anda gunakan ditandai **(Perangkat ini)** berwarna hijau.
- Setiap perangkat menampilkan label dan waktu terakhir aktif.

### Mengeluarkan Perangkat Lain

1. Temukan perangkat yang ingin dikeluarkan di daftar.
2. Tap tombol **"Keluarkan"** (berwarna merah) di samping perangkat tersebut.
3. Sesi perangkat tersebut akan dihapus dari server secara langsung.

### Memperbarui Daftar

- Tap tombol **"Perbarui Daftar"** untuk menyegarkan daftar perangkat secara manual.

---

## 12. Ubah Kata Sandi

1. Scroll ke bawah di Admin Dashboard.
2. Tap tombol **"Ubah Kata Sandi"**.
3. Isi tiga field yang muncul:
   - **Kata Sandi Saat Ini** — kata sandi yang sekarang aktif
   - **Kata Sandi Baru** — minimal **6 karakter**
   - **Konfirmasi Kata Sandi Baru** — harus sama persis dengan Kata Sandi Baru
4. Tap **"Simpan Kata Sandi Baru"**.
5. Jika berhasil, muncul pesan "Kata sandi berhasil diubah" dan kembali ke Admin Dashboard.
6. Jika kata sandi lama salah, muncul pesan error.

> Tombol simpan baru aktif jika semua field terisi, kata sandi baru ≥ 6 karakter, dan konfirmasi cocok.

---

## 13. Sinkronisasi Antar Perangkat

Aplikasi mendukung penggunaan di **beberapa perangkat sekaligus** (mis. TV ruang utama + TV ruang wudu).

- Pengaturan masjid (nama, lokasi, logo, teks, gambar) **otomatis tersinkronisasi** setiap ~10 detik ke semua perangkat yang terhubung.
- Perubahan yang disimpan di satu perangkat akan tampil di perangkat lain tanpa perlu restart.
- Gambar yang diupload dari perangkat mana pun akan tampil di semua perangkat.
- Jika **alamat atau zona waktu** diubah di satu perangkat, perangkat lain akan **langsung memperbarui waktu sholat** begitu sinkronisasi diterima — tanpa perlu membuka Admin atau menekan tombol apa pun.

### Kondisi Offline

Jika perangkat tidak terhubung ke internet:
- Banner merah muncul di bagian atas Admin Dashboard: **"Tidak ada koneksi — perubahan akan disimpan saat online"**
- Perubahan pengaturan tetap tersimpan secara lokal.
- Push ke server akan gagal; coba kembali saat koneksi pulih.

---

## 14. Logout

1. Scroll ke paling bawah di Admin Dashboard.
2. Tap tombol merah **"Keluar"**.
3. Sesi pada perangkat ini dihapus dari server.
4. Anda kembali ke tampilan utama (dashboard) tanpa akses admin.

---

## 15. Troubleshooting

### Waktu sholat tidak tampil / "XX:XX"
- Pastikan alamat waktu sholat sudah dikonfirmasi via GPS atau dipilih dari daftar saran — bukan diketik bebas.
- Pastikan perangkat terhubung ke internet.
- Tap "Simpan Perubahan" setelah mengubah alamat untuk memaksa refresh.

### Tombol "Simpan Perubahan" tidak aktif
- Field alamat mendeteksi bahwa teks telah diubah secara manual tanpa memilih dari daftar saran atau menggunakan GPS.
- Solusi: ketik ulang nama lokasi dan pilih salah satu opsi dari dropdown yang muncul, atau tap ikon GPS untuk mengisi otomatis.

### Gambar tidak muncul di layar utama
- Pastikan setidaknya satu gambar sudah diupload di bagian "Slide Gambar".
- Pastikan koneksi internet aktif (gambar dimuat dari Supabase Storage).
- Coba restart aplikasi.

### Logo tidak berubah setelah upload
- Pastikan proses upload selesai (tombol kembali ke "Ubah Logo", bukan "Mengupload...").
- Pastikan koneksi internet aktif saat upload.

### Perubahan tidak muncul di perangkat lain
- Tunggu sekitar 10–15 detik agar sinkronisasi berjalan.
- Pastikan kedua perangkat terhubung ke internet.

### Tidak bisa login
- Periksa ejaan nama pengguna dan kata sandi.
- Pastikan koneksi internet aktif.
- Jika lupa kata sandi, hubungi administrator sistem.

### Aplikasi menampilkan "Tidak ada koneksi"
- Periksa koneksi WiFi atau data seluler perangkat.
- Perubahan yang sudah disimpan secara lokal akan otomatis dikirim saat koneksi pulih.

---

*Aplikasi ini dikembangkan untuk kebutuhan internal Masjid Baiturrahman.*
