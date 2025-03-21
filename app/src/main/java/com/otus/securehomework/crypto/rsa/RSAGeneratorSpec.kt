package com.otus.securehomework.crypto.rsa

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.math.BigInteger
import java.security.spec.AlgorithmParameterSpec
import java.util.Calendar
import javax.security.auth.x500.X500Principal

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class RSAGeneratorSpecImpl (private val applicationContext: Context) : IRSAParameterSpec {
    override fun getAlgorithmParameterSpec(): AlgorithmParameterSpec {
        val calendar = Calendar.getInstance()
        val start = calendar.time
        calendar.add(Calendar.YEAR, 10)
        val end = calendar.time
        return android.security.KeyPairGeneratorSpec.Builder(applicationContext)
            .setAlias(IRSAParameterSpec.RSA_KEY_ALIAS)
            .setSubject(X500Principal("CN=${IRSAParameterSpec.RSA_KEY_ALIAS}"))
            .setSerialNumber(BigInteger.TEN)
            .setStartDate(start)
            .setEndDate(end)
            .build()
    }
}
