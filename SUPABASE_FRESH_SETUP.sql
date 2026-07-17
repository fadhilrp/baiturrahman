-- ============================================================================
-- Baiturrahman — Fresh Supabase Project Setup
-- ============================================================================
-- Run this once, top to bottom, on a brand-new Supabase project (SQL Editor).
-- Rebuilds the full schema from scratch: no prior tables required.
-- After running this, also do the two manual steps in section 8 below.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Extensions
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- 2. Tables
-- ----------------------------------------------------------------------------

-- 2.1 accounts — one row per registered user
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,           -- bcrypt via crypt(password, gen_salt('bf'))
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2.2 device_sessions — one row per device login
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

-- 2.3 mosque_settings — one row per account
CREATE TABLE mosque_settings (
    account_id UUID PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    mosque_name TEXT NOT NULL DEFAULT 'Masjid Baiturrahman',
    mosque_location TEXT NOT NULL DEFAULT 'Pondok Pinang',
    logo_image TEXT,
    prayer_address TEXT NOT NULL DEFAULT 'Lebak Bulus, Jakarta, ID',
    prayer_timezone TEXT NOT NULL DEFAULT 'Asia/Jakarta',
    quote_text TEXT NOT NULL DEFAULT '"Sesungguhnya shalat itu mencegah dari perbuatan-perbuatan keji dan mungkar." (QS. Al-Ankabut: 45)',
    marquee_text TEXT NOT NULL DEFAULT 'Lurus dan rapatkan shaf, mohon untuk mematikan alat komunikasi demi menjaga kesempurnaan sholat.',
    iqomah_duration_minutes INTEGER NOT NULL DEFAULT 10,
    iqomah_subuh_minutes INTEGER NOT NULL DEFAULT 10,
    iqomah_dzuhur_minutes INTEGER NOT NULL DEFAULT 10,
    iqomah_ashar_minutes INTEGER NOT NULL DEFAULT 10,
    iqomah_maghrib_minutes INTEGER NOT NULL DEFAULT 10,
    iqomah_isya_minutes INTEGER NOT NULL DEFAULT 10,
    adzan_offset_minutes INTEGER NOT NULL DEFAULT 0,
    is_dark_mode BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2.4 mosque_images — many rows per account
CREATE TABLE mosque_images (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    image_uri TEXT NOT NULL DEFAULT '',       -- Supabase Storage public URL
    display_order INTEGER NOT NULL DEFAULT 0, -- order in slider
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type TEXT NOT NULL DEFAULT 'image/jpeg',
    upload_status TEXT NOT NULL DEFAULT 'completed', -- uploading, completed, failed
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ON mosque_images(account_id);

-- ----------------------------------------------------------------------------
-- 3. Row Level Security — deny all direct anon access
-- ----------------------------------------------------------------------------
-- All reads/writes go through the SECURITY DEFINER RPC functions below.
-- No permissive policies are created, so RLS denies everything by default.
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosque_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE mosque_images ENABLE ROW LEVEL SECURITY;

-- ----------------------------------------------------------------------------
-- 4. Storage bucket policies
-- ----------------------------------------------------------------------------
-- Manual step first (Dashboard → Storage → Create bucket):
--   name: mosque-images
--   Public bucket: ON
-- Then run:
CREATE POLICY "Public read" ON storage.objects FOR SELECT TO public USING (bucket_id = 'mosque-images');
CREATE POLICY "Anon upload" ON storage.objects FOR INSERT TO anon WITH CHECK (bucket_id = 'mosque-images');
CREATE POLICY "Anon update" ON storage.objects FOR UPDATE TO anon USING (bucket_id = 'mosque-images');
CREATE POLICY "Anon delete" ON storage.objects FOR DELETE TO anon USING (bucket_id = 'mosque-images');

-- ----------------------------------------------------------------------------
-- 5. RPC functions (all SECURITY DEFINER, run as postgres role)
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION check_username_available(p_username TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN NOT EXISTS (SELECT 1 FROM accounts WHERE username = p_username);
END; $$;

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

    INSERT INTO mosque_settings (account_id, quote_text, marquee_text)
    VALUES (
        v_account_id,
        '"Sesungguhnya shalat itu mencegah dari perbuatan-perbuatan keji dan mungkar." (QS. Al-Ankabut: 45)',
        'Lurus dan rapatkan shaf, mohon untuk mematikan alat komunikasi demi menjaga kesempurnaan sholat.'
    )
    ON CONFLICT (account_id) DO NOTHING;

    RETURN json_build_object('session_token', v_token);
END; $$;

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

CREATE OR REPLACE FUNCTION logout_device(p_session_token TEXT)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    DELETE FROM device_sessions WHERE session_token = p_session_token;
END; $$;

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

CREATE OR REPLACE FUNCTION upsert_settings_by_token(
    p_session_token TEXT,
    p_mosque_name TEXT,
    p_mosque_location TEXT,
    p_logo_image TEXT,
    p_prayer_address TEXT,
    p_prayer_timezone TEXT,
    p_quote_text TEXT,
    p_marquee_text TEXT,
    p_iqomah_duration_minutes INTEGER DEFAULT 10,
    p_iqomah_subuh_minutes INTEGER DEFAULT 10,
    p_iqomah_dzuhur_minutes INTEGER DEFAULT 10,
    p_iqomah_ashar_minutes INTEGER DEFAULT 10,
    p_iqomah_maghrib_minutes INTEGER DEFAULT 10,
    p_iqomah_isya_minutes INTEGER DEFAULT 10,
    p_adzan_offset_minutes INTEGER DEFAULT 0,
    p_is_dark_mode BOOLEAN DEFAULT TRUE
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
        prayer_address, prayer_timezone, quote_text, marquee_text,
        iqomah_duration_minutes, iqomah_subuh_minutes, iqomah_dzuhur_minutes,
        iqomah_ashar_minutes, iqomah_maghrib_minutes, iqomah_isya_minutes,
        adzan_offset_minutes, is_dark_mode
    )
    VALUES (
        v_account_id, p_mosque_name, p_mosque_location, p_logo_image,
        p_prayer_address, p_prayer_timezone, p_quote_text, p_marquee_text,
        p_iqomah_duration_minutes, p_iqomah_subuh_minutes, p_iqomah_dzuhur_minutes,
        p_iqomah_ashar_minutes, p_iqomah_maghrib_minutes, p_iqomah_isya_minutes,
        p_adzan_offset_minutes, p_is_dark_mode
    )
    ON CONFLICT (account_id) DO UPDATE SET
        mosque_name = EXCLUDED.mosque_name,
        mosque_location = EXCLUDED.mosque_location,
        logo_image = EXCLUDED.logo_image,
        prayer_address = EXCLUDED.prayer_address,
        prayer_timezone = EXCLUDED.prayer_timezone,
        quote_text = EXCLUDED.quote_text,
        marquee_text = EXCLUDED.marquee_text,
        iqomah_duration_minutes = EXCLUDED.iqomah_duration_minutes,
        iqomah_subuh_minutes = EXCLUDED.iqomah_subuh_minutes,
        iqomah_dzuhur_minutes = EXCLUDED.iqomah_dzuhur_minutes,
        iqomah_ashar_minutes = EXCLUDED.iqomah_ashar_minutes,
        iqomah_maghrib_minutes = EXCLUDED.iqomah_maghrib_minutes,
        iqomah_isya_minutes = EXCLUDED.iqomah_isya_minutes,
        adzan_offset_minutes = EXCLUDED.adzan_offset_minutes,
        is_dark_mode = EXCLUDED.is_dark_mode,
        updated_at = NOW();
END; $$;

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

-- ----------------------------------------------------------------------------
-- 6. Grant execute permissions to the anon role
-- ----------------------------------------------------------------------------
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
GRANT EXECUTE ON FUNCTION upsert_settings_by_token(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, BOOLEAN) TO anon;
GRANT EXECUTE ON FUNCTION get_images_by_token(TEXT) TO anon;
GRANT EXECUTE ON FUNCTION upload_image_atomic(TEXT, UUID, INTEGER, BIGINT, TEXT, TEXT, TEXT) TO anon;
GRANT EXECUTE ON FUNCTION delete_image_and_reorder(TEXT, UUID) TO anon;

-- ============================================================================
-- 7. Manual steps after running this script
-- ============================================================================
-- a) Storage → Create bucket named "mosque-images", toggle Public bucket ON,
--    BEFORE running section 4 above (the storage policies reference it).
-- b) Project Settings → API → copy the new Project URL and anon key.
-- c) Update SUPABASE_URL / SUPABASE_ANON_KEY:
--    - This branch has uncommitted work moving them into BuildConfig
--      (see app/src/main/java/.../SupabaseClient.kt + local.properties),
--      so set them wherever that in-progress change now expects them.
--    - If that change isn't in place yet, they still live as constants in
--      SupabaseClient.kt.
-- d) All existing accounts, passwords, uploaded images and settings from the
--    old project are gone — this script only rebuilds structure. Every user
--    will need to register a new account from scratch, and images will need
--    to be re-uploaded through the app.
-- ============================================================================
