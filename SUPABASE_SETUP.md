# Supabase Setup — Account Management System

This document covers the complete Supabase setup for the account-based authentication system.
All data access goes through `SECURITY DEFINER` RPC functions — the anon key never touches
raw password values or credential tables directly.

---

## Prerequisites

- Supabase account and project
- Project URL and anon key (Project Settings → API)

---

## 1. Enable Extensions

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

---

## 2. Create New Tables

### 2.1 `accounts` — one row per registered user

```sql
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,           -- bcrypt via crypt(password, gen_salt('bf'))
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 2.2 `device_sessions` — one row per device login

```sql
CREATE TABLE device_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    session_token TEXT NOT NULL UNIQUE DEFAULT encode(gen_random_bytes(32), 'hex'),
    device_identifier TEXT NOT NULL,       -- UUID generated once per install
    device_label TEXT NOT NULL DEFAULT '', -- e.g. "Samsung Galaxy Tab A"
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ON device_sessions(account_id);
CREATE INDEX ON device_sessions(session_token);
```

---

## 3. Migrate Existing Tables

> **Note:** This clears existing device-specific data. Back up first if needed.

### 3.1 Migrate `mosque_settings`

```sql
-- Clear existing device-specific data
TRUNCATE mosque_settings;

-- Drop old primary key and device_name constraint
ALTER TABLE mosque_settings DROP CONSTRAINT IF EXISTS mosque_settings_pkey CASCADE;
ALTER TABLE mosque_settings DROP CONSTRAINT IF EXISTS unique_device_name CASCADE;
ALTER TABLE mosque_settings DROP COLUMN IF EXISTS device_name;
ALTER TABLE mosque_settings DROP COLUMN IF EXISTS id;

-- Add account_id as primary key
ALTER TABLE mosque_settings ADD COLUMN account_id UUID REFERENCES accounts(id) ON DELETE CASCADE;
ALTER TABLE mosque_settings ADD PRIMARY KEY (account_id);
```

### 3.2 Migrate `mosque_images`

```sql
-- Clear existing device-specific data
TRUNCATE mosque_images;

-- Remove device_name, add account_id
ALTER TABLE mosque_images DROP COLUMN IF EXISTS device_name;
ALTER TABLE mosque_images ADD COLUMN account_id UUID REFERENCES accounts(id) ON DELETE CASCADE;
CREATE INDEX ON mosque_images(account_id);
```

---

## 4. Enable Row Level Security (Lock Down Direct Access)

```sql
-- Enable RLS on all tables
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosque_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosque_images ENABLE ROW LEVEL SECURITY;

-- Drop any existing permissive policies on data tables
DROP POLICY IF EXISTS "Allow public read access to mosque_settings" ON mosque_settings;
DROP POLICY IF EXISTS "Allow anon insert to mosque_settings" ON mosque_settings;
DROP POLICY IF EXISTS "Allow anon update to mosque_settings" ON mosque_settings;
DROP POLICY IF EXISTS "Allow public read access to mosque_images" ON mosque_images;
DROP POLICY IF EXISTS "Allow anon insert to mosque_images" ON mosque_images;
DROP POLICY IF EXISTS "Allow anon update to mosque_images" ON mosque_images;
DROP POLICY IF EXISTS "Allow anon delete from mosque_images" ON mosque_images;

-- Deny all direct anon access — all ops go through SECURITY DEFINER RPCs
-- (No permissive policies = deny by default when RLS is enabled)
```

---

## 5. Storage Bucket

The `mosque-images` bucket stays the same (logos and images).

### 5.1 Create bucket (if not exists)

1. Supabase Dashboard → Storage → Create bucket: `mosque-images`
2. Toggle **Public bucket: ON**

### 5.2 Storage policies (if not already set)

```sql
CREATE POLICY "Public read" ON storage.objects FOR SELECT TO public USING (bucket_id = 'mosque-images');
CREATE POLICY "Anon upload" ON storage.objects FOR INSERT TO anon WITH CHECK (bucket_id = 'mosque-images');
CREATE POLICY "Anon update" ON storage.objects FOR UPDATE TO anon USING (bucket_id = 'mosque-images');
CREATE POLICY "Anon delete" ON storage.objects FOR DELETE TO anon USING (bucket_id = 'mosque-images');
```

---

## 6. RPC Functions

All functions are `SECURITY DEFINER` — they run as the postgres role and can bypass RLS.

### 6.1 `check_username_available`

```sql
CREATE OR REPLACE FUNCTION check_username_available(p_username TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN NOT EXISTS (SELECT 1 FROM accounts WHERE username = p_username);
END; $$;
```

### 6.2 `register_account`

```sql
CREATE OR REPLACE FUNCTION register_account(
    p_username TEXT,
    p_password TEXT,
    p_device_id TEXT,
    p_device_label TEXT
) RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_token TEXT;
BEGIN
    IF EXISTS (SELECT 1 FROM accounts WHERE username = p_username) THEN
        RAISE EXCEPTION 'USERNAME_TAKEN';
    END IF;

    INSERT INTO accounts (username, password_hash)
    VALUES (p_username, crypt(p_password, gen_salt('bf')))
    RETURNING id INTO v_account_id;

    INSERT INTO device_sessions (account_id, device_identifier, device_label)
    VALUES (v_account_id, p_device_id, p_device_label)
    RETURNING session_token INTO v_token;

    -- Create default settings row for this account
    INSERT INTO mosque_settings (account_id, quote_text, marquee_text)
    VALUES (
        v_account_id,
        '"Sesungguhnya shalat itu mencegah dari perbuatan-perbuatan keji dan mungkar." (QS. Al-Ankabut: 45)',
        'Lurus dan rapatkan shaf, mohon untuk mematikan alat komunikasi demi menjaga kesempurnaan sholat.'
    )
    ON CONFLICT (account_id) DO NOTHING;

    RETURN json_build_object('session_token', v_token);
END; $$;
```

### 6.3 `login_account`

```sql
CREATE OR REPLACE FUNCTION login_account(
    p_username TEXT,
    p_password TEXT,
    p_device_id TEXT,
    p_device_label TEXT
) RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_password_hash TEXT;
    v_token TEXT;
    v_session_id UUID;
BEGIN
    SELECT id, password_hash INTO v_account_id, v_password_hash
    FROM accounts WHERE username = p_username;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_CREDENTIALS';
    END IF;

    IF crypt(p_password, v_password_hash) != v_password_hash THEN
        RAISE EXCEPTION 'INVALID_CREDENTIALS';
    END IF;

    UPDATE accounts SET last_active_at = NOW() WHERE id = v_account_id;

    -- Reuse existing session for this device if present
    SELECT id INTO v_session_id
    FROM device_sessions
    WHERE account_id = v_account_id AND device_identifier = p_device_id;

    IF v_session_id IS NOT NULL THEN
        UPDATE device_sessions
        SET last_seen_at = NOW(), device_label = p_device_label
        WHERE id = v_session_id
        RETURNING session_token INTO v_token;
    ELSE
        INSERT INTO device_sessions (account_id, device_identifier, device_label)
        VALUES (v_account_id, p_device_id, p_device_label)
        RETURNING session_token INTO v_token;
    END IF;

    RETURN json_build_object('session_token', v_token);
END; $$;
```

### 6.4 `validate_session`

```sql
CREATE OR REPLACE FUNCTION validate_session(p_session_token TEXT)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_username TEXT;
BEGIN
    SELECT ds.account_id, a.username INTO v_account_id, v_username
    FROM device_sessions ds
    JOIN accounts a ON a.id = ds.account_id
    WHERE ds.session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RETURN json_build_object('account_id', NULL);
    END IF;

    UPDATE device_sessions SET last_seen_at = NOW()
    WHERE session_token = p_session_token;

    RETURN json_build_object('account_id', v_account_id::TEXT, 'username', v_username);
END; $$;
```

### 6.5 `logout_device`

```sql
CREATE OR REPLACE FUNCTION logout_device(p_session_token TEXT)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    DELETE FROM device_sessions WHERE session_token = p_session_token;
END; $$;
```

### 6.6 `logout_other_device`

```sql
CREATE OR REPLACE FUNCTION logout_other_device(
    p_session_token TEXT,
    p_target_session_id UUID
) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    DELETE FROM device_sessions
    WHERE id = p_target_session_id AND account_id = v_account_id;
END; $$;
```

### 6.7 `change_password`

```sql
CREATE OR REPLACE FUNCTION change_password(
    p_session_token TEXT,
    p_old_password TEXT,
    p_new_password TEXT
) RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_password_hash TEXT;
BEGIN
    SELECT ds.account_id, a.password_hash
    INTO v_account_id, v_password_hash
    FROM device_sessions ds
    JOIN accounts a ON a.id = ds.account_id
    WHERE ds.session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    IF crypt(p_old_password, v_password_hash) != v_password_hash THEN
        RETURN json_build_object('success', false, 'error', 'WRONG_PASSWORD');
    END IF;

    UPDATE accounts
    SET password_hash = crypt(p_new_password, gen_salt('bf'))
    WHERE id = v_account_id;

    RETURN json_build_object('success', true);
END; $$;
```

### 6.8 `update_session_last_seen`

```sql
CREATE OR REPLACE FUNCTION update_session_last_seen(p_session_token TEXT)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    UPDATE device_sessions SET last_seen_at = NOW()
    WHERE session_token = p_session_token;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;
END; $$;
```

### 6.9 `get_active_sessions`

```sql
CREATE OR REPLACE FUNCTION get_active_sessions(p_session_token TEXT)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_result JSON;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    SELECT json_agg(row_to_json(r)) INTO v_result
    FROM (
        SELECT
            id,
            device_label,
            last_seen_at,
            (session_token = p_session_token) AS is_current
        FROM device_sessions
        WHERE account_id = v_account_id
        ORDER BY last_seen_at DESC
    ) r;

    RETURN COALESCE(v_result, '[]'::JSON);
END; $$;
```

### 6.10 `get_settings_by_token`

```sql
CREATE OR REPLACE FUNCTION get_settings_by_token(p_session_token TEXT)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_result JSON;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    SELECT row_to_json(t) INTO v_result
    FROM (SELECT * FROM mosque_settings WHERE account_id = v_account_id) t;

    RETURN v_result;
END; $$;
```

### 6.11 `upsert_settings_by_token`

```sql
CREATE OR REPLACE FUNCTION upsert_settings_by_token(
    p_session_token TEXT,
    p_mosque_name TEXT,
    p_mosque_location TEXT,
    p_logo_image TEXT,
    p_prayer_address TEXT,
    p_prayer_timezone TEXT,
    p_quote_text TEXT,
    p_marquee_text TEXT
) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    INSERT INTO mosque_settings (
        account_id, mosque_name, mosque_location, logo_image,
        prayer_address, prayer_timezone, quote_text, marquee_text
    )
    VALUES (
        v_account_id, p_mosque_name, p_mosque_location, p_logo_image,
        p_prayer_address, p_prayer_timezone, p_quote_text, p_marquee_text
    )
    ON CONFLICT (account_id) DO UPDATE SET
        mosque_name = EXCLUDED.mosque_name,
        mosque_location = EXCLUDED.mosque_location,
        logo_image = EXCLUDED.logo_image,
        prayer_address = EXCLUDED.prayer_address,
        prayer_timezone = EXCLUDED.prayer_timezone,
        quote_text = EXCLUDED.quote_text,
        marquee_text = EXCLUDED.marquee_text,
        updated_at = NOW();
END; $$;
```

### 6.12 `get_images_by_token`

```sql
CREATE OR REPLACE FUNCTION get_images_by_token(p_session_token TEXT)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_result JSON;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    SELECT json_agg(row_to_json(t)) INTO v_result
    FROM (
        SELECT * FROM mosque_images
        WHERE account_id = v_account_id AND upload_status = 'completed'
        ORDER BY display_order ASC
    ) t;

    RETURN COALESCE(v_result, '[]'::JSON);
END; $$;
```

### 6.13 `upload_image_atomic`

```sql
CREATE OR REPLACE FUNCTION upload_image_atomic(
    p_session_token TEXT,
    p_id UUID,
    p_display_order INTEGER,
    p_file_size BIGINT,
    p_mime_type TEXT,
    p_image_uri TEXT,
    p_upload_status TEXT DEFAULT 'completed'
) RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    v_result JSON;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    INSERT INTO mosque_images (
        id, account_id, display_order, file_size, mime_type, image_uri, upload_status
    )
    VALUES (
        p_id, v_account_id, p_display_order, p_file_size, p_mime_type, p_image_uri, p_upload_status
    )
    ON CONFLICT (id) DO UPDATE SET
        image_uri = EXCLUDED.image_uri,
        upload_status = EXCLUDED.upload_status,
        file_size = EXCLUDED.file_size,
        updated_at = NOW();

    SELECT row_to_json(t) INTO v_result
    FROM (SELECT * FROM mosque_images WHERE id = p_id) t;

    RETURN v_result;
END; $$;
```

### 6.14 `delete_image_and_reorder`

```sql
CREATE OR REPLACE FUNCTION delete_image_and_reorder(
    p_session_token TEXT,
    p_image_id UUID
) RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_account_id UUID;
    img RECORD;
    new_order INTEGER := 0;
    v_result JSON;
BEGIN
    SELECT account_id INTO v_account_id
    FROM device_sessions WHERE session_token = p_session_token;

    IF v_account_id IS NULL THEN
        RAISE EXCEPTION 'INVALID_SESSION';
    END IF;

    DELETE FROM mosque_images WHERE id = p_image_id AND account_id = v_account_id;

    FOR img IN
        SELECT id FROM mosque_images
        WHERE account_id = v_account_id AND upload_status = 'completed'
        ORDER BY display_order ASC
    LOOP
        UPDATE mosque_images SET display_order = new_order WHERE id = img.id;
        new_order := new_order + 1;
    END LOOP;

    SELECT json_agg(row_to_json(t)) INTO v_result
    FROM (
        SELECT * FROM mosque_images
        WHERE account_id = v_account_id AND upload_status = 'completed'
        ORDER BY display_order ASC
    ) t;

    RETURN COALESCE(v_result, '[]'::JSON);
END; $$;
```

---

## 7. Grant Execute Permissions

> Full argument lists are required to disambiguate from any old function versions with the same name.

```sql
GRANT EXECUTE ON FUNCTION check_username_available(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION register_account(TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION login_account(TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION validate_session(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION logout_device(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION logout_other_device(TEXT, UUID) TO anon;
GRANT EXECUTE ON FUNCTION change_password(TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION update_session_last_seen(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION get_active_sessions(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION get_settings_by_token(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION upsert_settings_by_token(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION get_images_by_token(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION upload_image_atomic(TEXT, UUID, INTEGER, BIGINT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION delete_image_and_reorder(TEXT, UUID) TO anon;
```

---

## 8. Update Android App Credentials

Update `SupabaseClient.kt` with your Supabase project URL and anon key.

---

## 9. Auth Flow Summary

```
App Start
  ├─ Has sessionToken in prefs?
  │       NO  → LoginScreen
  │       YES → validate_session(token)
  │                 NULL  → clear prefs → LoginScreen
  │                 valid → MosqueDashboard (sync starts)
  │
LoginScreen
  ├─ "Masuk" → login_account(u, p, deviceId, deviceLabel)
  │                 success → save token → MosqueDashboard
  │                 fail    → snackbar error
  └─ "Daftar" → RegisterScreen

RegisterScreen
  ├─ On blur: check_username_available(u) → show ✓/✗
  ├─ "Daftar" → register_account(u, p, deviceId, deviceLabel)
  │                 success → save token → MosqueDashboard
  └─ "Masuk" → LoginScreen

Sync (every 10s)
  1. update_session_last_seen(token)   ← heartbeat / force-logout detection
  2. get_settings_by_token(token)      ← pull settings
  3. get_images_by_token(token)        ← pull images

Logout
  └─ logout_device(token) → clear prefs → LoginScreen
```

---

## 10. Useful SQL Queries

```sql
-- View all accounts
SELECT id, username, created_at, last_active_at FROM accounts;

-- View active sessions
SELECT ds.id, a.username, ds.device_label, ds.last_seen_at
FROM device_sessions ds
JOIN accounts a ON a.id = ds.account_id
ORDER BY ds.last_seen_at DESC;

-- View settings per account
SELECT a.username, ms.*
FROM mosque_settings ms
JOIN accounts a ON a.id = ms.account_id;

-- View images per account
SELECT a.username, mi.id, mi.display_order, mi.upload_status
FROM mosque_images mi
JOIN accounts a ON a.id = mi.account_id
ORDER BY a.username, mi.display_order;

-- Reset (clear all sessions — forces all devices to re-login)
DELETE FROM device_sessions;

-- Remove a specific account and all its data
DELETE FROM accounts WHERE username = 'test_user';
```
