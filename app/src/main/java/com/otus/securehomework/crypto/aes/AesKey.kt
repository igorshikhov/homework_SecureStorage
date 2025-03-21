package com.otus.securehomework.crypto.aes

import java.security.KeyStore
import javax.crypto.SecretKey

interface IAesKey {
    fun clearKey()

    fun getSecretKey(): SecretKey
}

abstract class AesKey : IAesKey
{
    protected val keyStore: KeyStore = KeyStore.getInstance(KEY_PROVIDER).apply {
        load(null)
    }

    protected fun removeKey(keyAlias: String) {
        keyStore.deleteEntry(keyAlias)
    }

    abstract override fun getSecretKey(): SecretKey

    protected companion object {
        const val KEY_PROVIDER = "AndroidKeyStore"
        const val KEY_LENGTH = 256
        const val AES_ALGORITHM = "AES"
    }
}