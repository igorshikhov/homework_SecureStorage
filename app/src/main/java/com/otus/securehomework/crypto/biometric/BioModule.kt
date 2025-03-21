package com.otus.securehomework.crypto.biometric

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.otus.securehomework.R
import com.otus.securehomework.crypto.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

const val PREFERENCES_BIOMETRIC = "biometric_preferences "

class BioModule @Inject constructor(
    private val bioPreferences: BioPreferences,
    @Named(PREFERENCES_BIOMETRIC) private val cryptoManager: CryptoManager
) {
    private fun isBiometricAvailable(): Boolean = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)

    fun isAvailable(context: Context): Boolean {
        if (isBiometricAvailable()) {
            try {
                val canAuth = BiometricManager.from(context).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                return (canAuth == BiometricManager.BIOMETRIC_SUCCESS)
            }
            catch(e: Exception) {
                Toast.makeText(context, e.message ?: "error", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    private fun isBiometricStrongAvailable(context: Context): Boolean {
        try {
            val canAuth = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
            return (canAuth == BiometricManager.BIOMETRIC_SUCCESS)
        }
        catch(e: Exception) {
            Toast.makeText(context, e.message ?: "error", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun isBiometricWeakAvailable(context: Context): Boolean {
        try {
            val canAuth = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            return (canAuth == BiometricManager.BIOMETRIC_SUCCESS)
        }
        catch(e: Exception) {
            Toast.makeText(context, e.message ?: "error", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun isUserRegistered() : Flow<Boolean> {
        return bioPreferences.isBiometricEnabled()
    }

    suspend fun clearRegisteredUser() {
        return bioPreferences.clearBiometric()
    }

    fun registerUserBiometrics(context: FragmentActivity, onSuccess: () -> Unit = {}, userData: CharSequence) {
        if (isBiometricAvailable()) {
            val cipher = cryptoManager.initEncryptionCipher()
            val prompt = getBiometricPrompt(context) { authResult ->
                authResult.cryptoObject?.cipher?.let { cipher ->
                    context.lifecycleScope.launch {
                        bioPreferences.setUserData(cryptoManager.encrypt(userData, cipher))
                    }
                    onSuccess()
                }
            }
            prompt.authenticate(getPromptInfoRegister(context), BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun getPromptAuth(context: FragmentActivity, negativeButtonText: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_prompt_title))
            .setDescription(context.getString(R.string.biometric_prompt_description))
            .setConfirmationRequired(false)
            .setNegativeButtonText(negativeButtonText)
            .apply {
                if (isBiometricStrongAvailable(context))
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                else if (isBiometricWeakAvailable(context))
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            }
            .build()
    }
    
    fun authenticateUser(context: FragmentActivity, negativeButtonText: String, onSuccess: (CharSequence) -> Unit) {
        if (isBiometricAvailable()) {
            context.lifecycleScope.launch {
                val userData = bioPreferences.getUserData()
                val payloadData = cryptoManager.getPayload(userData)
                val cipher = cryptoManager.initDecryptionCipher(payloadData.iv)
                val prompt = getBiometricPrompt(context) { authResult ->
                    authResult.cryptoObject?.cipher?.let { cipherAuth ->
                        onSuccess(cryptoManager.decrypt(payloadData.ciphertext, cipherAuth))
                    }
                }
                prompt.authenticate(getPromptAuth(context, negativeButtonText), BiometricPrompt.CryptoObject(cipher))
            }
        }
    }

    private fun getPromptInfoRegister(context: FragmentActivity): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.register_biometric))
            .setSubtitle(context.getString(R.string.authenticate_using_biometric))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()
    }

    private fun getBiometricPrompt(context: FragmentActivity, onAuthSucceed: (BiometricPrompt.AuthenticationResult) -> Unit): BiometricPrompt {
        val biometricPrompt = BiometricPrompt(
            context,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.e("BIO", "Authentication Succeeded: ${result.cryptoObject}")
                    onAuthSucceed(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.e("BIO", "onAuthenticationError")
                }

                override fun onAuthenticationFailed() {
                    Log.e("BIO", "onAuthenticationFailed")
                }
            }
        )
        return biometricPrompt
    }
}