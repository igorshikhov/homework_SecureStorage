package com.otus.securehomework.crypto.rsa

import java.security.spec.AlgorithmParameterSpec

interface IRSAParameterSpec {

    fun getAlgorithmParameterSpec() : AlgorithmParameterSpec

    companion object {
        const val RSA_KEY_ALIAS = "OTUS_RSA"
    }
}