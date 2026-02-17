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
    device_name TEXT NOT NULL DEFAULT '',
    image_uri TEXT,
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
CREATE INDEX idx_mosque_images_device_name ON mosque_images(device_name);

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
-- Create mosque_settings table (device-specific)
CREATE TABLE mosque_settings (
    id SERIAL PRIMARY KEY,
    device_name TEXT NOT NULL DEFAULT '' UNIQUE,
    mosque_name TEXT NOT NULL DEFAULT 'Masjid Baiturrahman',
    mosque_location TEXT NOT NULL DEFAULT 'Pondok Pinang',
    logo_image TEXT,
    prayer_address TEXT NOT NULL DEFAULT 'Lebak Bulus, Jakarta, ID',
    prayer_timezone TEXT NOT NULL DEFAULT 'Asia/Jakarta',
    quote_text TEXT NOT NULL DEFAULT 'Lorem ipsum dolor sit amet',
    marquee_text TEXT NOT NULL DEFAULT 'Rolling Text',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

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

-- Allow anon users to insert (for new device settings)
CREATE POLICY "Allow anon insert to mosque_settings"
ON mosque_settings FOR INSERT
TO anon
WITH CHECK (true);

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

## 7. Device-Specific Data Migration

If you're upgrading from a version without device-specific data support, run these SQL commands to add the `device_name` column:

### 7.1 Add device_name to mosque_images

```sql
-- Add device_name column to mosque_images
ALTER TABLE mosque_images
ADD COLUMN IF NOT EXISTS device_name TEXT NOT NULL DEFAULT '';

-- Create index for device_name
CREATE INDEX IF NOT EXISTS idx_mosque_images_device_name ON mosque_images(device_name);
```

### 7.2 Add device_name to mosque_settings

```sql
-- Drop the single-row constraint
ALTER TABLE mosque_settings DROP CONSTRAINT IF EXISTS single_row_check;

-- Add device_name column
ALTER TABLE mosque_settings
ADD COLUMN IF NOT EXISTS device_name TEXT NOT NULL DEFAULT '';

-- Make device_name unique (each device has its own settings)
ALTER TABLE mosque_settings
ADD CONSTRAINT unique_device_name UNIQUE (device_name);

-- Add upsert policy for settings
CREATE POLICY "Allow anon insert to mosque_settings"
ON mosque_settings FOR INSERT
TO anon
WITH CHECK (true);
```

### 7.3 Understanding Device-Specific Data

- Each device identified by its `device_name` will have its own set of settings and images
- When you change the "Nama Perangkat" (device name), the app will sync data specific to that device
- Different devices (TV-1, TV-2, etc.) can have different images and settings
- If a device name has no data in Supabase, it will start with empty/default values

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

## 6. RPC Functions (Stored Procedures)

These PostgreSQL functions provide atomic, transaction-safe operations called via Supabase RPC.

### 6.1 `upload_image_atomic` — Single-call image record creation

Replaces the two-step `INSERT` → `UPDATE` pattern. Creates a completed image record in one call.

```sql
CREATE OR REPLACE FUNCTION upload_image_atomic(
    p_id UUID,
    p_device_name TEXT,
    p_display_order INTEGER,
    p_file_size BIGINT,
    p_mime_type TEXT,
    p_image_uri TEXT,
    p_upload_status TEXT DEFAULT 'completed'
) RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    result JSON;
BEGIN
    INSERT INTO mosque_images (id, device_name, display_order, file_size, mime_type, image_uri, upload_status)
    VALUES (p_id, p_device_name, p_display_order, p_file_size, p_mime_type, p_image_uri, p_upload_status)
    ON CONFLICT (id) DO UPDATE SET
        image_uri = EXCLUDED.image_uri,
        upload_status = EXCLUDED.upload_status,
        file_size = EXCLUDED.file_size,
        updated_at = NOW();

    SELECT row_to_json(t) INTO result
    FROM (SELECT * FROM mosque_images WHERE id = p_id) t;

    RETURN result;
END;
$$;
```

### 6.2 `delete_image_and_reorder` — Atomic delete + reorder

Deletes an image and reorders the remaining images for the device in a single transaction.

```sql
CREATE OR REPLACE FUNCTION delete_image_and_reorder(
    p_image_id UUID,
    p_device_name TEXT
) RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    img RECORD;
    new_order INTEGER := 0;
    result JSON;
BEGIN
    DELETE FROM mosque_images WHERE id = p_image_id;

    FOR img IN
        SELECT id FROM mosque_images
        WHERE device_name = p_device_name AND upload_status = 'completed'
        ORDER BY display_order ASC
    LOOP
        UPDATE mosque_images SET display_order = new_order WHERE id = img.id;
        new_order := new_order + 1;
    END LOOP;

    SELECT json_agg(row_to_json(t)) INTO result
    FROM (
        SELECT * FROM mosque_images
        WHERE device_name = p_device_name AND upload_status = 'completed'
        ORDER BY display_order ASC
    ) t;

    RETURN COALESCE(result, '[]'::JSON);
END;
$$;
```

### 6.3 `batch_upsert_images` — Batch migration

Upserts multiple images in a single transaction. Idempotent via `ON CONFLICT ... DO UPDATE`.

```sql
CREATE OR REPLACE FUNCTION batch_upsert_images(p_images JSON)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    img JSON;
    result_count INTEGER := 0;
BEGIN
    FOR img IN SELECT * FROM json_array_elements(p_images) LOOP
        INSERT INTO mosque_images (id, device_name, display_order, file_size, mime_type, image_uri, upload_status)
        VALUES (
            (img->>'id')::UUID,
            img->>'device_name',
            (img->>'display_order')::INTEGER,
            (img->>'file_size')::BIGINT,
            COALESCE(img->>'mime_type', 'image/jpeg'),
            img->>'image_uri',
            COALESCE(img->>'upload_status', 'completed')
        )
        ON CONFLICT (id) DO UPDATE SET
            image_uri = EXCLUDED.image_uri,
            upload_status = EXCLUDED.upload_status,
            updated_at = NOW();
        result_count := result_count + 1;
    END LOOP;

    RETURN json_build_object('upserted', result_count);
END;
$$;
```

### 6.4 `rename_device` — Atomic device rename across tables

Renames a device in both `mosque_settings` and `mosque_images` in a single transaction. Called when the user changes the device name in admin settings.

```sql
CREATE OR REPLACE FUNCTION rename_device(p_old_name TEXT, p_new_name TEXT)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE settings_count INT; images_count INT;
BEGIN
    UPDATE mosque_settings SET device_name = p_new_name WHERE device_name = p_old_name;
    GET DIAGNOSTICS settings_count = ROW_COUNT;
    UPDATE mosque_images SET device_name = p_new_name WHERE device_name = p_old_name;
    GET DIAGNOSTICS images_count = ROW_COUNT;
    RETURN json_build_object('settings_renamed', settings_count, 'images_renamed', images_count);
END; $$;
```

### 6.5 Grant Permissions

After creating the functions, grant execute permission to the `anon` role:

```sql
GRANT EXECUTE ON FUNCTION upload_image_atomic TO anon;
GRANT EXECUTE ON FUNCTION delete_image_and_reorder TO anon;
GRANT EXECUTE ON FUNCTION batch_upsert_images TO anon;
GRANT EXECUTE ON FUNCTION rename_device TO anon;
```

## Next Steps

After completing this setup:

1. Update `SupabaseClient.kt` with your credentials
2. Deploy the RPC functions via SQL Editor (Section 6)
3. Run the Android app
4. Test image upload from admin dashboard
5. Verify images appear in PostgreSQL table
6. Test sync between devices
