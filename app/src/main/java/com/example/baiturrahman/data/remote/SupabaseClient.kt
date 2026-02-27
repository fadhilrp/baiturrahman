package com.example.baiturrahman.data.remote

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertPathValidator
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    // 🚨 REPLACE THESE WITH YOUR ACTUAL SUPABASE CREDENTIALS 🚨
    // Get these from your Supabase Dashboard > Settings > API
    private const val SUPABASE_URL = "https://zdcoximugrvbajyqoslj.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InpkY294aW11Z3J2YmFqeXFvc2xqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQ0MDU4NTcsImV4cCI6MjA2OTk4MTg1N30.niF1n6ZwqWgJyLQEBD0Q1yEwq2jJTznChZnNphoGKV0"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        // Provide a pre-configured OkHttpClient with a custom TrustManager that tolerates
        // stale OCSP staples. Android's TrustManagerImpl enforces OCSP revocation, and the
        // Supabase server occasionally serves an expired OCSP response, causing every TLS
        // handshake to fail. The custom TrustManager still validates the full certificate chain;
        // it only skips revocation when the OCSP response itself is the problem.
        httpEngine = OkHttp.create {
            preconfigured = buildOkHttpClient()
        }
        install(Storage)
        install(Postgrest)
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val trustManager = buildOcspTolerantTrustManager()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
    }

    /**
     * Wraps the system X509TrustManager to tolerate stale OCSP staples from the server.
     * All other certificate errors (bad chain, unknown CA, expired cert, etc.) still throw.
     * On a stale-OCSP failure, falls back to PKIX chain validation without revocation so the
     * certificate chain itself is still fully verified.
     */
    private fun buildOcspTolerantTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        val system = factory.trustManagers.filterIsInstance<X509TrustManager>().first()

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                system.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                try {
                    system.checkServerTrusted(chain, authType)
                } catch (e: CertificateException) {
                    val isStaleOcsp = generateSequence(e as Throwable) { it.cause }
                        .any { it.message?.contains("validity interval is out-of-date") == true }
                    if (!isStaleOcsp) throw e
                    // Server is sending an expired OCSP staple — validate chain without revocation
                    Log.w(TAG, "Stale OCSP response from server; falling back to chain-only validation")
                    validateWithoutRevocation(chain)
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = system.acceptedIssuers

            private fun validateWithoutRevocation(chain: Array<X509Certificate>) {
                val certPath = CertificateFactory.getInstance("X.509").generateCertPath(chain.toList())
                val anchors = acceptedIssuers.map { TrustAnchor(it, null) }.toSet()
                val params = PKIXParameters(anchors).apply { isRevocationEnabled = false }
                try {
                    CertPathValidator.getInstance("PKIX").validate(certPath, params)
                } catch (e: CertPathValidatorException) {
                    throw CertificateException("Certificate chain validation failed", e)
                }
            }
        }
    }

    init {
        Log.d(TAG, "🔧 Supabase client initialized")
        Log.d(TAG, "📍 URL: $SUPABASE_URL")
        Log.d(TAG, "🔑 Key: ${SUPABASE_ANON_KEY.take(20)}...${SUPABASE_ANON_KEY.takeLast(4)}")

        // Validate credentials format
        if (SUPABASE_URL.contains("your-project-ref") || SUPABASE_URL.contains("your-actual-project-id") ||
            SUPABASE_ANON_KEY.contains("your-anon-key") || SUPABASE_ANON_KEY.contains("your-actual-anon-key")) {
            Log.e(TAG, "❌ PLACEHOLDER CREDENTIALS DETECTED! Please update with real Supabase credentials")
        } else {
            Log.d(TAG, "✅ Credentials format looks valid")
        }

        // Test that storage is accessible
        try {
            val storage = client.storage
            Log.d(TAG, "✅ Storage client accessible: $storage")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Storage client not accessible", e)
        }
    }
}
