package com.metacommander.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted preferences for small local settings only.
 * Note: EncryptedSharedPreferences is deprecated in newer AndroidX Security releases;
 * evaluate Android Keystore/DataStore migration before a long-lived production release.
 */
class SecurePrefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "meta_ads_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun putPinHash(hash: String) { prefs.edit().putString("pin_hash", hash).apply() }
    fun getPinHash(): String? = prefs.getString("pin_hash", null)
}
