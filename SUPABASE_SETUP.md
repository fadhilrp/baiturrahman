# Supabase Setup Instructions

This document provides step-by-step instructions for setting up the Supabase backend for the Baiturrahman application.

## Prerequisites

- Supabase account at https://supabase.com
- Existing project or create a new one
- Project URL and anon key (found in Project Settings > API)

## 1. Create PostgreSQL Tables

### 1.1 Create `mosque_images` Table

Go to Supabase Dashboard > SQL Editor and run:

```sql
-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create mosque_images table
CREATE TABLE mosque_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    image_uri TEXT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    upload_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    file_size BIGINT DEFAULT 0,
    mime_type TEXT DEFAULT 'image/jpeg',
    upload_status TEXT NOT NULL DEFAULT 'uploading',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create index for faster queries
CREATE INDEX idx_mosque_images_display_order ON mosque_images(display_order);
CREATE INDEX idx_mosque_images_upload_status ON mosque_images(upload_status);

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_mosque_images_updated_at
    BEFORE UPDATE ON mosque_images
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 1.2 Create `mosque_settings` Table

```sql
-- Create mosque_settings table
CREATE TABLE mosque_settings (
    id INTEGER PRIMARY KEY DEFAULT 1,
    mosque_name TEXT NOT NULL DEFAULT 'Masjid Baiturrahman',
    mosque_location TEXT NOT NULL DEFAULT 'Pondok Pinang',
    logo_image TEXT,
    prayer_address TEXT NOT NULL DEFAULT 'Lebak Bulus, Jakarta, ID',
    prayer_timezone TEXT NOT NULL DEFAULT 'Asia/Jakarta',
    quote_text TEXT NOT NULL DEFAULT 'Lorem ipsum dolor sit amet',
    marquee_text TEXT NOT NULL DEFAULT 'Rolling Text',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT single_row_check CHECK (id = 1)
);

-- Insert default settings
INSERT INTO mosque_settings (id) VALUES (1)
ON CONFLICT (id) DO NOTHING;

-- Create trigger to auto-update updated_at
CREATE TRIGGER update_mosque_settings_updated_at
    BEFORE UPDATE ON mosque_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

## 2. Configure Row Level Security (RLS)

### 2.1 Enable RLS on Tables

```sql
-- Enable RLS
ALTER TABLE mosque_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosque_settings ENABLE ROW LEVEL SECURITY;
```

### 2.2 Create RLS Policies

```sql
-- mosque_images policies
-- Allow anyone to read
CREATE POLICY "Allow public read access to mosque_images"
ON mosque_images FOR SELECT
TO public
USING (true);

-- Allow anon users to insert
CREATE POLICY "Allow anon insert to mosque_images"
ON mosque_images FOR INSERT
TO anon
WITH CHECK (true);

-- Allow anon users to update
CREATE POLICY "Allow anon update to mosque_images"
ON mosque_images FOR UPDATE
TO anon
USING (true)
WITH CHECK (true);

-- Allow anon users to delete
CREATE POLICY "Allow anon delete from mosque_images"
ON mosque_images FOR DELETE
TO anon
USING (true);

-- mosque_settings policies
-- Allow anyone to read
CREATE POLICY "Allow public read access to mosque_settings"
ON mosque_settings FOR SELECT
TO public
USING (true);

-- Allow anon users to update
CREATE POLICY "Allow anon update to mosque_settings"
ON mosque_settings FOR UPDATE
TO anon
USING (true)
WITH CHECK (true);
```

## 3. Configure Storage Bucket

### 3.1 Create Storage Bucket (if not exists)

1. Go to Supabase Dashboard > Storage
2. Create new bucket named: `mosque-images`
3. Make it **public** (toggle Public bucket: ON)

### 3.2 Configure Bucket Policies

```sql
-- Allow public read access
CREATE POLICY "Public Access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'mosque-images');

-- Allow anon uploads
CREATE POLICY "Anon Upload"
ON storage.objects FOR INSERT
TO anon
WITH CHECK (bucket_id = 'mosque-images');

-- Allow anon updates
CREATE POLICY "Anon Update"
ON storage.objects FOR UPDATE
TO anon
USING (bucket_id = 'mosque-images');

-- Allow anon deletes
CREATE POLICY "Anon Delete"
ON storage.objects FOR DELETE
TO anon
USING (bucket_id = 'mosque-images');
```

## 4. Verify Configuration

### 4.1 Test Tables

Run in SQL Editor:

```sql
-- Test mosque_settings
SELECT * FROM mosque_settings;

-- Test mosque_images (should be empty initially)
SELECT * FROM mosque_images;
```

### 4.2 Test Storage

1. Go to Storage > mosque-images
2. Try uploading a test image
3. Verify you can access the public URL

## 5. Update Android App Configuration

### 5.1 Get Credentials

1. Go to Project Settings > API
2. Copy **Project URL** (e.g., `https://xxxxx.supabase.co`)
3. Copy **anon public** key

### 5.2 Update SupabaseClient.kt

Update the credentials in `/app/src/main/java/com/example/baiturrahman/data/remote/SupabaseClient.kt`:

```kotlin
private const val SUPABASE_URL = "YOUR_PROJECT_URL_HERE"
private const val SUPABASE_ANON_KEY = "YOUR_ANON_KEY_HERE"
```

## 6. Useful SQL Queries

### View all images with status

```sql
SELECT id, image_uri, display_order, upload_status, upload_date
FROM mosque_images
ORDER BY display_order;
```

### Count images by status

```sql
SELECT upload_status, COUNT(*) as count
FROM mosque_images
GROUP BY upload_status;
```

### View current settings

```sql
SELECT * FROM mosque_settings;
```

### Clear all images (for testing)

```sql
DELETE FROM mosque_images;
```

### Reset settings to default

```sql
UPDATE mosque_settings
SET
    mosque_name = 'Masjid Baiturrahman',
    mosque_location = 'Pondok Pinang',
    logo_image = NULL,
    prayer_address = 'Lebak Bulus, Jakarta, ID',
    prayer_timezone = 'Asia/Jakarta',
    quote_text = 'Lorem ipsum dolor sit amet',
    marquee_text = 'Rolling Text'
WHERE id = 1;
```

## Important Notes

### Authentication Model

This app uses the **`anon` (anonymous) role** for all database and storage operations:
- **No user authentication required** - suitable for trusted devices in a controlled mosque environment
- All devices use the same anonymous key for read/write operations
- RLS policies are configured to allow `anon` role for INSERT, UPDATE, and DELETE operations
- This simplifies deployment since no per-device credentials are needed

**Security Considerations:**
- Devices should be physically secured within the mosque premises
- The anon key can be restricted to specific IP ranges via Supabase API settings if needed
- Supabase provides built-in rate limiting to prevent abuse
- For higher security requirements, authentication can be added later using the Auth module

## Troubleshooting

### RLS Policies Not Working

- Verify RLS is enabled on tables
- Check policy names don't conflict
- Ensure policies use correct roles (`public`, `anon`)
- Note: The app uses the `anon` role for all operations (no authentication required)

### Storage Upload Fails

- Verify bucket is public
- Check storage policies are created
- Ensure bucket name matches exactly: `mosque-images`

### Can't Query Tables from App

- Verify anon key is correct
- Check RLS policies allow SELECT for public
- Ensure PostgREST is enabled in project settings

## Next Steps

After completing this setup:

1. Update `SupabaseClient.kt` with your credentials
2. Run the Android app
3. Test image upload from admin dashboard
4. Verify images appear in PostgreSQL table
5. Test sync between devices
