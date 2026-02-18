# Test Cases & Bug Hunting Scenarios

## 1. Image Management

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| IMG-01 | Upload a single image | 1. Open Admin Dashboard 2. Scroll to "Slide Gambar" 3. Tap "Tambah Gambar" 4. Select an image from gallery | Image appears in the slider row, count updates (e.g., 1/5), image is uploaded to Supabase Storage and a record is created in `mosque_images` | High |
| IMG-02 | Upload to maximum limit (5) | 1. Upload 5 images one by one | After 5th image, "Tambah Gambar" button is hidden and "Jumlah maksimum gambar tercapai (5/5)" text is shown | High |
| IMG-03 | Attempt upload beyond limit | 1. Have 5 images 2. Try to call `addMosqueImage` programmatically | No new image is added, log warning "Cannot add more images, limit reached (5/5)" | Medium |
| IMG-04 | Delete an image | 1. Tap the X button on an image thumbnail | Image is removed from slider, Supabase Storage file is deleted, `mosque_images` record is deleted via `delete_image_and_reorder` RPC, remaining images are reordered | High |
| IMG-05 | Delete all images | 1. Delete images one by one until none remain | Slider row is empty, image count shows 0/5, "Tambah Gambar" button reappears | Medium |
| IMG-06 | Auto-save after upload | 1. Upload an image | Settings are automatically saved (via `saveAllSettingsInternal`) after upload completes | High |
| IMG-07 | Auto-save after delete | 1. Delete an image | Settings are automatically saved after deletion completes | High |
| IMG-08 | Upload loading state | 1. Tap "Tambah Gambar" and select an image | Button shows spinner + "Mengupload..." text, delete buttons are disabled during upload | Medium |
| IMG-09 | Delete loading state | 1. Tap X on an image | Delete buttons and upload button are disabled during deletion | Medium |
| IMG-10 | Upload large image | 1. Select an image > 5MB | Image uploads successfully (may take longer), no timeout or crash | Medium |
| IMG-11 | Image slider auto-advance | 1. Upload 3+ images 2. Navigate to main dashboard | Images auto-cycle every 5 seconds, index wraps around correctly | Medium |
| IMG-12 | Image slider sync during delete | 1. Have 3 images, slider is on image #3 2. Delete image #3 | `currentImageIndex` resets to 0, no IndexOutOfBoundsException | High |

## 2. Settings Save

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| SET-01 | Save all settings | 1. Modify mosque name, location, quote, marquee 2. Tap "Simpan Perubahan" | All fields are saved to local Room DB and pushed to Supabase PostgreSQL | High |
| SET-02 | Save loading state | 1. Tap "Simpan Perubahan" | Button shows spinner + "Menyimpan...", button is disabled | Medium |
| SET-03 | Mosque name character limit | 1. Type in "Nama Masjid" field | Cannot exceed 35 characters, counter shows red at limit, `isError = true` | Medium |
| SET-04 | Mosque location character limit | 1. Type in "Lokasi Masjid" field | Cannot exceed 25 characters, counter shows red at limit | Medium |
| SET-05 | Quote text character limit | 1. Type in "Teks Kutipan" field | Cannot exceed 100 characters, counter shows red at limit | Medium |
| SET-06 | Marquee text character limit | 1. Type in "Teks Berjalan" field | Cannot exceed 100 characters, counter shows red at limit | Medium |
| SET-07 | Logo upload | 1. Tap "Ubah Logo" 2. Select an image | Logo uploads to Supabase Storage (storage-only, no `mosque_images` record), old logo is deleted, settings auto-save | High |
| SET-08 | Logo upload loading state | 1. Tap "Ubah Logo" and select image | Button shows spinner + "Mengupload...", button is disabled | Medium |
| SET-09 | Settings persist after app restart | 1. Save settings 2. Kill and reopen app | All settings are loaded from local Room DB | High |
| SET-10 | Settings sync from remote | 1. Change settings on master device 2. Wait for sync on non-master | Non-master device receives updated settings via polling | High |

## 3. Device & Sync

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| DEV-01 | Select existing device name from dropdown | 1. Open Admin 2. In "Nama Perangkat" dropdown, select an existing device | Device name is set, dropdown closes | High |
| DEV-02 | Add new device name | 1. Click "+ Tambah Perangkat Baru" 2. Enter a new name in the text field | Text field appears, user can type a new name | High |
| DEV-03 | Cancel adding new device | 1. Click "+ Tambah Perangkat Baru" 2. Click "Batal" | Returns to dropdown mode, device name resets to previously saved name | Medium |
| DEV-04 | Device rename on save | 1. Select a different device name (or enter new) 2. Tap "Simpan" | `rename_device` RPC is called, both `mosque_settings` and `mosque_images` are updated atomically | High |
| DEV-05 | Device rename failure | 1. Enter a device name that conflicts or network is down 2. Tap "Simpan" | Snackbar shows "Gagal mengubah nama perangkat", save is aborted | High |
| DEV-06 | Device name with same name (no rename) | 1. Keep the same device name 2. Tap "Simpan" | No rename RPC is called, settings save normally | Medium |
| DEV-07 | Master device toggle | 1. Toggle "Perangkat Utama" switch ON | All editing sections (Header, Prayer, Quote, Images, Marquee) become visible | High |
| DEV-08 | Non-master device view | 1. Toggle "Perangkat Utama" switch OFF | Only sync settings and info message are shown, full "Simpan Perubahan" is replaced with "Simpan Pengaturan Sinkronisasi" | High |
| DEV-09 | Sync enabled toggle | 1. Toggle "Aktifkan Sinkronisasi" ON 2. Save | `syncService.startSync()` is called, polling begins | Medium |
| DEV-10 | Sync disabled toggle | 1. Toggle "Aktifkan Sinkronisasi" OFF 2. Save | `syncService.stopSync()` is called, polling stops | Medium |
| DEV-11 | Sync lock during save | 1. Trigger save while a sync cycle is in progress | Save waits for `syncMutex` to be released, no data race | High |
| DEV-12 | Sync restart after save | 1. Change device name and save | Sync is stopped and restarted to pick up new device name | Medium |
| DEV-13 | Device names loaded on startup | 1. Open the app | `loadDeviceNames()` is called in ViewModel init, dropdown is populated | High |
| DEV-14 | Empty device names list | 1. No devices exist in Supabase `mosque_settings` | Dropdown is empty, user can still add a new device via "+ Tambah Perangkat Baru" | Medium |

## 4. Prayer Times

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| PRA-01 | Fetch prayer times on startup | 1. Open app | Prayer times are fetched from API using saved address and timezone, displayed on dashboard | High |
| PRA-02 | Change prayer address | 1. Open Admin 2. Change "Alamat Waktu Sholat" 3. Save | Prayer times are re-fetched with new address | High |
| PRA-03 | Change timezone | 1. Open Admin 2. Select different timezone from dropdown 3. Save | Prayer times are re-fetched with new timezone | High |
| PRA-04 | Invalid address | 1. Enter a nonsensical address 2. Save | Error message is shown in `uiState.errorMessage`, default "XX:XX" times may be displayed | Medium |
| PRA-05 | Network failure during fetch | 1. Disable network 2. Open app or refresh prayer times | Error message is shown, previously cached times (if any) remain | Medium |
| PRA-06 | Timezone validation | 1. Attempt to set an invalid timezone programmatically | `updatePrayerTimezone` only accepts values in `availableTimezones` list | Low |
| PRA-07 | All 4 timezone options | 1. Select each timezone: Asia/Jakarta, Asia/Pontianak, Asia/Makassar, Asia/Jayapura | Each selection updates prayer times correctly | Medium |
| PRA-08 | Loading state during fetch | 1. Trigger prayer time fetch | `uiState.isLoading` is true while fetching, UI shows loading indicator | Medium |

## 5. UI Components

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| UI-01 | Image slider display | 1. Add 3+ images 2. View main dashboard | Images display in slider with auto-advance every 5 seconds | High |
| UI-02 | Image slider manual navigation | 1. Tap on image indicator dots (if present) | Slider navigates to selected image | Medium |
| UI-03 | Marquee text scrolling | 1. Set marquee text 2. View main dashboard | Text scrolls horizontally across the screen | Medium |
| UI-04 | Header display | 1. Set mosque name, location, logo | Header shows mosque name, location, and logo image | High |
| UI-05 | Supabase image loading | 1. Images are stored in Supabase Storage | `SupabaseImage` component loads images with proper headers/auth | High |
| UI-06 | Admin dashboard scroll | 1. Open Admin with all sections visible (master mode) | All sections are scrollable, no content is cut off | Medium |
| UI-07 | Admin section card styling | 1. View any admin section | Card has white background, 2dp elevation, proper padding | Low |
| UI-08 | Snackbar messages | 1. Trigger a rename failure | Snackbar appears at bottom with error message | Medium |
| UI-09 | Top app bar | 1. Open Admin Dashboard | Shows "Dashboard Admin" title with back arrow, emerald green theme | Low |

## 6. Bug Hunting Scenarios

### Race Conditions

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| BUG-01 | Rapid image upload + delete | 1. Upload an image 2. Immediately tap delete before upload finishes | Either upload completes then delete runs, or operation is properly blocked by `isUploadingImage`/`isDeletingImage` flags. No orphaned Supabase records | High |
| BUG-02 | Save during sync cycle | 1. Trigger save while background sync is in progress | `syncMutex` ensures operations are serialized, no data corruption | High |
| BUG-03 | Double-tap save button | 1. Rapidly tap "Simpan Perubahan" twice | Button is disabled after first tap (`isSaving = true`), only one save operation runs | Medium |
| BUG-04 | Slider index during rapid deletes | 1. Have 5 images 2. Delete images 3, 4, 5 rapidly | `currentImageIndex` stays within bounds after each delete, no crash | High |
| BUG-05 | Concurrent rename + sync | 1. Rename device while a sync poll is happening | Sync lock prevents both from running simultaneously | High |

### Network Failures

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| BUG-06 | Upload image with no network | 1. Disable WiFi/data 2. Try to upload image | Upload fails gracefully, error is logged, no crash, loading state is cleared | High |
| BUG-07 | Save settings with no network | 1. Disable network 2. Tap "Simpan" | Local Room save succeeds, remote push fails silently (logged), settings are not lost | High |
| BUG-08 | Delete image with no network | 1. Disable network 2. Tap X on image | Deletion may fail, image remains in list, error is logged | Medium |
| BUG-09 | Sync polling with intermittent network | 1. Toggle WiFi on/off during sync | Sync handles errors gracefully, retries on next poll interval | Medium |
| BUG-10 | Rename device with no network | 1. Disable network 2. Change device name 3. Save | Rename RPC fails, snackbar shows error, save is aborted (name not changed locally) | High |

### Edge Cases

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| BUG-11 | Empty device name | 1. Clear device name field 2. Tap save | Rename is skipped (check `newName.isNotBlank()`), or validation prevents empty save | Medium |
| BUG-12 | Very long device name | 1. Enter a 200+ character device name | Should either be limited by UI or handled by DB constraint | Low |
| BUG-13 | Special characters in device name | 1. Enter device name with emojis, unicode, or SQL-like strings | Name is properly escaped by Supabase client, no injection | Medium |
| BUG-14 | Duplicate device name rename | 1. Rename device to a name that already exists in `mosque_settings` | RPC should handle conflict (depends on DB constraint), error surfaced to user | Medium |
| BUG-15 | App kill during upload | 1. Start uploading an image 2. Force-kill the app | On next startup, `cleanupStaleUploads` removes orphaned "uploading" records | High |
| BUG-16 | Logo upload then immediately change logo | 1. Upload logo 2. Before first upload finishes, upload another | `isUploadingLogo` flag should disable the button, preventing double upload | Medium |
| BUG-17 | Settings loaded before sync completes | 1. Fresh install, non-master device 2. Open app | Local Room has default values, sync eventually pulls remote data | Medium |
| BUG-18 | Image with null URI in database | 1. Simulate an incomplete upload in Room (imageUri = null) | `loadSavedSettings` filters out images with null/blank URIs | Medium |
| BUG-19 | Zero images in slider | 1. Delete all images | Slider shows nothing, `currentImageIndex` stays at 0, auto-advance loop is harmless | Low |
| BUG-20 | Permissions denied for image picker | 1. Deny storage permission when prompted | Snackbar shows "Izin penyimpanan diperlukan untuk memilih gambar" | Medium |

### Data Integrity

| ID | Description | Steps | Expected Result | Priority |
|----|-------------|-------|-----------------|----------|
| BUG-21 | Room and Supabase out of sync | 1. Upload image 2. Manually delete from Supabase dashboard 3. Check app | Image URL in Room points to deleted Storage file, `SupabaseImage` shows nothing (no fallback) | Medium |
| BUG-22 | Stale upload cleanup | 1. Create a record with `upload_status = "uploading"` older than 30 min 2. Restart app | `cleanupStaleUploads` deletes the orphaned record | Medium |
| BUG-23 | Image display order after delete | 1. Upload images A, B, C (order 0, 1, 2) 2. Delete B | C is reordered to position 1 via `delete_image_and_reorder` RPC | High |
| BUG-24 | Upsert settings conflict | 1. Two devices save settings with same device_name simultaneously | Upsert with `onConflict = "device_name"` resolves correctly, last write wins | Medium |
| BUG-25 | Device rename atomicity | 1. Rename device 2. Check both `mosque_settings` and `mosque_images` tables | Both tables have the new device name (atomic via `rename_device` RPC) | High |
