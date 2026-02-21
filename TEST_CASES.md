# Test Cases & Bug Hunting Scenarios

> **Versi:** sesuai commit terbaru (`main` branch)
> **Terakhir diperbarui:** Februari 2026

---

## 1. Image Management

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| IMG-01 | Upload satu gambar | 1. Buka Admin Dashboard 2. Scroll ke "Slide Gambar" 3. Tap "Tambah Gambar (0/5)" 4. Pilih gambar dari galeri | Gambar muncul di baris thumbnail, tombol menampilkan hitungan terbaru (e.g., "Tambah Gambar (1/5)"), gambar terupload ke Supabase Storage dan record dibuat di `mosque_images` | High |
| IMG-02 | Upload hingga batas maksimum (5) | 1. Upload 5 gambar satu per satu | Setelah gambar ke-5, tombol "Tambah Gambar" disembunyikan dan diganti teks "Jumlah maksimum gambar tercapai (5/5)" | High |
| IMG-03 | Coba upload melebihi batas | 1. Ada 5 gambar 2. Panggil `addMosqueImage` secara programatik | Tidak ada gambar baru ditambahkan, log `W/MosqueDashboardViewModel: Max images reached` | Medium |
| IMG-04 | Hapus satu gambar | 1. Tap tombol X pada thumbnail gambar | Gambar dihapus dari slider, file Supabase Storage dihapus, record `mosque_images` dihapus via RPC `delete_image_and_reorder`, gambar tersisa diurutkan ulang | High |
| IMG-05 | Hapus semua gambar | 1. Hapus gambar satu per satu hingga kosong | Baris thumbnail kosong, tombol "Tambah Gambar (0/5)" muncul kembali | Medium |
| IMG-06 | Auto-save setelah upload | 1. Upload gambar | Pengaturan otomatis disimpan (`saveAllSettingsInternal`) setelah upload selesai | High |
| IMG-07 | Auto-save setelah hapus | 1. Hapus gambar | Pengaturan otomatis disimpan setelah penghapusan selesai | High |
| IMG-08 | Loading state saat upload | 1. Tap "Tambah Gambar (X/5)" dan pilih gambar | Tombol menampilkan spinner + teks "Mengupload...", tombol hapus dinonaktifkan selama upload | Medium |
| IMG-09 | Loading state saat hapus | 1. Tap X pada gambar | Tombol hapus dan tombol upload dinonaktifkan selama penghapusan | Medium |
| IMG-10 | Upload gambar berukuran besar | 1. Pilih gambar > 5MB | Gambar berhasil diupload (mungkin lebih lama), tidak ada timeout atau crash | Medium |
| IMG-11 | Auto-advance slider | 1. Upload 3+ gambar 2. Navigasi ke dashboard utama | Gambar berganti otomatis setiap 5 detik, index berputar dari awal setelah gambar terakhir | Medium |
| IMG-12 | Slider sync saat hapus | 1. Ada 3 gambar, slider di gambar ke-3 2. Hapus gambar ke-3 | `currentImageIndex` direset ke 0, tidak ada `IndexOutOfBoundsException` | High |

---

## 2. Settings Save

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| SET-01 | Simpan semua pengaturan | 1. Ubah nama masjid, lokasi, kutipan, teks berjalan 2. Tap "Simpan Perubahan" | Semua field disimpan ke Room DB lokal dan dipush ke Supabase PostgreSQL, snackbar "Pengaturan disimpan" muncul | High |
| SET-02 | Loading state saat simpan | 1. Tap "Simpan Perubahan" | Tombol menampilkan spinner + "Menyimpan...", tombol dinonaktifkan | Medium |
| SET-03 | Batas karakter nama masjid | 1. Ketik di field "Nama Masjid" | Tidak bisa melebihi 35 karakter, counter berwarna merah saat di batas, `isError = true` | Medium |
| SET-04 | Batas karakter lokasi masjid | 1. Ketik di field "Lokasi Masjid" | Tidak bisa melebihi 25 karakter, counter berwarna merah saat di batas | Medium |
| SET-05 | Batas karakter kutipan | 1. Ketik di field "Teks Kutipan" | Tidak bisa melebihi 100 karakter, counter berwarna merah saat di batas | Medium |
| SET-06 | Batas karakter teks berjalan | 1. Ketik di field "Teks Berjalan" | Tidak bisa melebihi 200 karakter, counter berwarna merah saat di batas | Medium |
| SET-07 | Upload logo | 1. Tap "Ubah Logo" 2. Pilih gambar | Logo terupload ke Supabase Storage (storage-only, tanpa record `mosque_images`), logo lama dihapus, pengaturan auto-save | High |
| SET-08 | Loading state saat upload logo | 1. Tap "Ubah Logo" dan pilih gambar | Tombol menampilkan spinner + "Mengupload...", tombol dinonaktifkan | Medium |
| SET-09 | Pengaturan tetap ada setelah restart | 1. Simpan pengaturan 2. Force-close dan buka ulang app | Semua pengaturan dimuat dari Room DB lokal | High |
| SET-10 | Sync pengaturan dari remote | 1. Ubah pengaturan di satu perangkat 2. Tunggu sync di perangkat lain | Perangkat lain menerima pengaturan terbaru via polling setiap 10 detik | High |

---

## 3. Authentication

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| AUTH-01 | Login berhasil | 1. Buka app (belum login) 2. Masukkan username dan password yang valid 3. Tap "Masuk" | Navigasi ke MosqueDashboard, sesi disimpan di SharedPreferences | High |
| AUTH-02 | Login gagal — kredensial salah | 1. Masukkan username atau password yang salah 2. Tap "Masuk" | Pesan error "Nama pengguna atau kata sandi salah" ditampilkan, tetap di LoginScreen | High |
| AUTH-03 | Login loading state | 1. Tap "Masuk" | Tombol dinonaktifkan selama proses login, indikator loading ditampilkan | Medium |
| AUTH-04 | Registrasi berhasil | 1. Buka RegisterScreen 2. Masukkan username baru dan password 3. Tap "Daftar" | Akun dibuat, token sesi disimpan, navigasi ke MosqueDashboard | High |
| AUTH-05 | Registrasi gagal — username sudah dipakai | 1. Daftar dengan username yang sudah ada | Pesan error "Nama pengguna sudah digunakan" ditampilkan | High |
| AUTH-06 | Logout | 1. Buka Admin Dashboard 2. Tap "Keluar" | Sesi dihapus di server (RPC), SharedPreferences dibersihkan, navigasi ke LoginScreen | High |
| AUTH-07 | Validasi sesi saat startup | 1. Buka app dengan token sesi tersimpan | Token divalidasi ke server; jika valid lanjut ke dashboard, jika tidak valid navigasi ke login | High |
| AUTH-08 | Username ditampilkan di top bar | 1. Buka Admin Dashboard | Username yang sedang login tampil di bawah judul "Dashboard Admin" di top app bar | Low |

---

## 4. Connected Devices & Session Management

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| DEV-01 | Daftar perangkat terhubung dimuat | 1. Buka Admin Dashboard | Bagian "Perangkat Terhubung" memuat daftar sesi aktif dari server, perangkat saat ini ditandai "(Perangkat ini)" | High |
| DEV-02 | Perangkat saat ini tidak bisa dikeluarkan | 1. Lihat daftar perangkat terhubung | Tidak ada tombol "Keluarkan" untuk perangkat yang sedang dipakai (`session.isCurrent = true`) | High |
| DEV-03 | Force logout perangkat lain | 1. Tap "Keluarkan" di samping perangkat lain | Sesi perangkat tersebut dihapus di server, daftar diperbarui | High |
| DEV-04 | Perbarui daftar perangkat | 1. Tap "Perbarui Daftar" | Daftar perangkat dimuat ulang dari server | Medium |
| DEV-05 | Waktu terakhir aktif | 1. Lihat daftar perangkat | Setiap perangkat menampilkan "Terakhir aktif: YYYY-MM-DD HH:MM" | Low |
| DEV-06 | Sync lock saat save | 1. Trigger save saat siklus sync background sedang berjalan | `syncMutex` memastikan operasi dijalankan berurutan, tidak ada data race | High |

---

## 5. Account Security — Change Password

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| PWD-01 | Buka halaman ubah kata sandi | 1. Scroll ke bawah di Admin Dashboard 2. Tap "Ubah Kata Sandi" | Top bar berganti judul "Ubah Kata Sandi", tampil 3 field input: Kata Sandi Saat Ini, Kata Sandi Baru, Konfirmasi | Medium |
| PWD-02 | Tombol simpan dinonaktifkan jika tidak valid | 1. Kosongkan field atau isi password baru < 6 karakter | Tombol "Simpan Kata Sandi Baru" dinonaktifkan selama kondisi tidak terpenuhi | Medium |
| PWD-03 | Validasi password tidak cocok | 1. Isi Kata Sandi Baru dan Konfirmasi dengan nilai berbeda | Field konfirmasi menampilkan `isError = true` dan teks "Kata sandi tidak cocok" (merah) | Medium |
| PWD-04 | Ubah kata sandi berhasil | 1. Isi semua field dengan benar 2. Tap "Simpan Kata Sandi Baru" | Snackbar "Kata sandi berhasil diubah" muncul, field dikosongkan, kembali ke Admin Dashboard | High |
| PWD-05 | Ubah kata sandi gagal — password lama salah | 1. Isi field Kata Sandi Saat Ini dengan yang salah 2. Tap simpan | Snackbar "Kata sandi lama salah" muncul | High |
| PWD-06 | Loading state saat ubah password | 1. Tap "Simpan Kata Sandi Baru" | Tombol menampilkan spinner + "Menyimpan...", tombol dinonaktifkan | Medium |
| PWD-07 | Toggle visibilitas password | 1. Tap ikon mata di salah satu field | Teks password menjadi terlihat / tersembunyi bergantian | Low |
| PWD-08 | Kembali dari halaman ubah password | 1. Tap tombol back (←) di top bar | Kembali ke tampilan Admin Dashboard utama, field password direset | Low |

---

## 6. Appearance — Dark/Light Theme

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| THM-01 | Aktifkan Mode Gelap | 1. Buka Admin Dashboard 2. Scroll ke "Tema Tampilan" 3. Nyalakan toggle "Mode Gelap" | Seluruh UI berpindah ke tema gelap secara langsung | High |
| THM-02 | Aktifkan Mode Terang | 1. Matikan toggle "Mode Gelap" | Seluruh UI kembali ke tema terang | High |
| THM-03 | Preferensi tema tersimpan | 1. Aktifkan Mode Gelap 2. Force-close dan buka ulang app | App terbuka dengan tema yang terakhir dipilih | High |
| THM-04 | Tema konsisten di semua layar | 1. Aktifkan Mode Gelap 2. Navigasi ke Login, Register, Dashboard, Admin | Semua layar mengikuti tema yang dipilih | Medium |

---

## 7. Prayer Times

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| PRA-01 | Fetch waktu sholat saat startup | 1. Buka app | Waktu sholat di-fetch dari API menggunakan alamat dan timezone tersimpan, ditampilkan di dashboard | High |
| PRA-02 | Ganti alamat waktu sholat | 1. Buka Admin 2. Ubah "Alamat Waktu Sholat" 3. Tap "Simpan Perubahan" | Waktu sholat di-fetch ulang dengan alamat baru | High |
| PRA-03 | Ganti timezone | 1. Buka Admin 2. Pilih timezone berbeda dari dropdown 3. Tap "Simpan Perubahan" | Waktu sholat di-fetch ulang dengan timezone baru | High |
| PRA-04 | Alamat tidak valid | 1. Masukkan alamat sembarang 2. Simpan | Pesan error muncul di `uiState.errorMessage`, waktu sholat mungkin menampilkan default "XX:XX" | Medium |
| PRA-05 | Gagal fetch karena tidak ada jaringan | 1. Matikan jaringan 2. Buka app atau refresh | Pesan error ditampilkan, waktu sholat terakhir (jika ada) tetap ditampilkan | Medium |
| PRA-06 | Validasi timezone | 1. Coba set timezone yang tidak ada secara programatik | `updatePrayerTimezone` hanya menerima nilai dalam daftar `availableTimezones` | Low |
| PRA-07 | Semua 4 opsi timezone | 1. Pilih masing-masing: Asia/Jakarta, Asia/Pontianak, Asia/Makassar, Asia/Jayapura | Setiap pilihan memperbarui waktu sholat dengan benar | Medium |
| PRA-08 | Loading state saat fetch | 1. Trigger fetch waktu sholat | `uiState.isLoading = true` selama fetch, UI menampilkan indikator loading | Medium |

---

## 8. UI Components

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| UI-01 | Tampilan image slider | 1. Tambahkan 3+ gambar 2. Lihat dashboard utama | Gambar tampil dalam slider dengan auto-advance setiap 5 detik | High |
| UI-02 | Marquee text scrolling | 1. Set teks berjalan 2. Lihat dashboard utama | Teks scroll horizontal di layar | Medium |
| UI-03 | Tampilan header | 1. Set nama masjid, lokasi, logo | Header menampilkan nama masjid, lokasi, dan logo | High |
| UI-04 | Loading gambar Supabase | 1. Gambar tersimpan di Supabase Storage | Komponen `SupabaseImage` memuat gambar dengan header auth yang benar | High |
| UI-05 | Admin dashboard bisa di-scroll | 1. Buka Admin (semua seksi terlihat) | Semua seksi bisa discroll, tidak ada konten terpotong | Medium |
| UI-06 | Offline banner | 1. Matikan jaringan 2. Buka Admin Dashboard | Banner merah muncul di bawah top app bar dengan teks "Tidak ada koneksi — perubahan akan disimpan saat online" | High |
| UI-07 | Offline banner hilang saat kembali online | 1. Matikan lalu nyalakan jaringan kembali | Banner merah menghilang ketika koneksi kembali | Medium |
| UI-08 | Snackbar pesan sukses simpan | 1. Tap "Simpan Perubahan" | Snackbar "Pengaturan disimpan" muncul di bagian bawah layar | Medium |
| UI-09 | Styling kartu seksi Admin | 1. Lihat seksi manapun di Admin | Kartu punya border tipis, background sesuai tema, padding yang memadai | Low |
| UI-10 | Top app bar Admin | 1. Buka Admin Dashboard | Menampilkan judul "Dashboard Admin" + username di bawahnya, tombol back di kiri | Low |

---

## 9. Bug Hunting Scenarios

### Race Conditions

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| BUG-01 | Upload cepat + hapus | 1. Upload gambar 2. Segera tap hapus sebelum upload selesai | Upload selesai dulu baru hapus, atau operasi diblok oleh flag `isUploadingImage`/`isDeletingImage`. Tidak ada record Supabase yang tergantung (orphaned) | High |
| BUG-02 | Save saat siklus sync | 1. Trigger save saat sync background berjalan | `syncMutex` memastikan serialisasi, tidak ada korupsi data | High |
| BUG-03 | Double-tap tombol simpan | 1. Ketuk "Simpan Perubahan" dua kali cepat | Tombol dinonaktifkan setelah tap pertama (`isSaving = true`), hanya satu operasi simpan berjalan | Medium |
| BUG-04 | Slider index saat hapus cepat | 1. Ada 5 gambar 2. Hapus gambar 3, 4, 5 cepat-cepatan | `currentImageIndex` tetap dalam batas setelah setiap penghapusan, tidak crash | High |

### Network Failures

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| BUG-05 | Upload gambar tanpa jaringan | 1. Matikan WiFi/data 2. Coba upload gambar | Upload gagal secara graceful, error dicatat di log, loading state dibersihkan, tidak crash | High |
| BUG-06 | Simpan pengaturan tanpa jaringan | 1. Matikan jaringan 2. Tap "Simpan" | Simpan ke Room lokal berhasil, push ke remote gagal secara silent (dicatat di log), data tidak hilang | High |
| BUG-07 | Hapus gambar tanpa jaringan | 1. Matikan jaringan 2. Tap X pada gambar | Penghapusan mungkin gagal, gambar tetap ada di list, error dicatat di log | Medium |
| BUG-08 | Sync polling dengan jaringan tidak stabil | 1. Toggle WiFi on/off saat sync berjalan | Sync menangani error secara graceful, retry di interval polling berikutnya | Medium |

### Edge Cases

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| BUG-09 | Nama perangkat kosong di label sesi | 1. Lihat daftar perangkat terhubung jika deviceLabel tidak diset | Menampilkan fallback yang aman, tidak crash | Medium |
| BUG-10 | App mati saat upload | 1. Mulai upload gambar 2. Force-kill app | Saat startup berikutnya, `cleanupStaleUploads` menghapus record "uploading" yang tergantung | High |
| BUG-11 | Upload logo lalu segera ganti logo | 1. Upload logo 2. Sebelum upload pertama selesai, upload logo lagi | Flag `isUploadingLogo` menonaktifkan tombol, mencegah upload ganda | Medium |
| BUG-12 | Pengaturan dimuat sebelum sync selesai | 1. Install baru, perangkat baru 2. Buka app | Room DB lokal punya nilai default, sync akhirnya menarik data remote | Medium |
| BUG-13 | Gambar dengan URI null di database | 1. Simulasikan upload tidak lengkap di Room (`imageUri = null`) | `loadSavedSettings` memfilter gambar dengan URI null/kosong | Medium |
| BUG-14 | Tidak ada gambar di slider | 1. Hapus semua gambar | Slider tidak menampilkan apa-apa, `currentImageIndex` tetap 0, loop auto-advance tidak error | Low |
| BUG-15 | Izin galeri ditolak | 1. Tolak izin storage saat diminta | Snackbar "Izin penyimpanan diperlukan" tampil, tidak crash | Medium |
| BUG-16 | Force logout dari perangkat yang sudah logout | 1. Perangkat A logout, Perangkat B coba force logout Perangkat A | Operasi gagal secara graceful (sesi tidak ada), daftar diperbarui | Medium |
| BUG-17 | Password baru kurang dari 6 karakter | 1. Di halaman ubah kata sandi, isi field Kata Sandi Baru < 6 karakter | Tombol simpan tetap dinonaktifkan (`newPassword.length >= 6` tidak terpenuhi) | Medium |

### Data Integrity

| ID | Deskripsi | Langkah | Hasil yang Diharapkan | Prioritas |
|----|-----------|---------|----------------------|-----------|
| BUG-18 | Room dan Supabase tidak sinkron | 1. Upload gambar 2. Hapus manual dari dashboard Supabase 3. Cek app | URL gambar di Room mengarah ke file Storage yang sudah dihapus, `SupabaseImage` tidak menampilkan apa-apa | Medium |
| BUG-19 | Cleanup upload tergantung | 1. Buat record dengan `upload_status = "uploading"` lebih dari 30 menit lalu 2. Restart app | `cleanupStaleUploads` menghapus record orphaned | Medium |
| BUG-20 | Urutan gambar setelah hapus | 1. Upload gambar A, B, C (urutan 0, 1, 2) 2. Hapus B | C diurutkan ulang ke posisi 1 via RPC `delete_image_and_reorder` | High |
| BUG-21 | Upsert settings conflict | 1. Dua perangkat menyimpan pengaturan dengan `device_name` sama secara bersamaan | Upsert dengan `onConflict = "device_name"` menyelesaikan konflik, tulisan terakhir menang | Medium |
