package com.otus.securehomework.di

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.otus.securehomework.data.repository.AuthRepository
import com.otus.securehomework.data.repository.UserRepository
import com.otus.securehomework.data.source.local.UserPreferences
import com.otus.securehomework.data.source.network.AuthApi
import com.otus.securehomework.data.source.network.UserApi
import com.otus.securehomework.crypto.aes.UserAesKey
import com.otus.securehomework.crypto.aes.AesKeysPreferences
import com.otus.securehomework.crypto.aes.IAesKey
import com.otus.securehomework.crypto.biometric.BioPreferences
import com.otus.securehomework.crypto.biometric.BioAesKey
import com.otus.securehomework.crypto.biometric.BioModule
import com.otus.securehomework.crypto.biometric.PREFERENCES_BIOMETRIC
import com.otus.securehomework.crypto.CryptoManager
import com.otus.securehomework.crypto.rsa.IRSAParameterSpec
import com.otus.securehomework.crypto.rsa.RSAGenEncryptSpecImpl
import com.otus.securehomework.crypto.rsa.RSAGeneratorSpecImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRemoteDataSource(preferences: UserPreferences): RemoteDataSource {
        return RemoteDataSource(preferences)
    }

    @Provides
    fun provideAuthApi(remoteDataSource: RemoteDataSource): AuthApi {
        return remoteDataSource.buildApi(AuthApi::class.java)
    }

    @Provides
    fun provideUserApi(remoteDataSource: RemoteDataSource): UserApi {
        return remoteDataSource.buildApi(UserApi::class.java)
    }

    @Singleton
    @Provides
    fun provideUserPreferences(@ApplicationContext context: Context, manager: CryptoManager): UserPreferences {
        return UserPreferences(context, manager)
    }

    @Provides
    fun provideAuthRepository(authApi: AuthApi, preferences: UserPreferences): AuthRepository {
        return AuthRepository(authApi, preferences)
    }

    @Provides
    fun provideUserRepository(userApi: UserApi): UserRepository {
        return UserRepository(userApi)
    }

    @Singleton
    @Provides
    fun provideUserDatastore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_BIOMETRIC) }
        )
    }

    @Provides
    fun provideAesKey(@ApplicationContext context: Context, rsaParameterSpec : IRSAParameterSpec) : IAesKey {
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) UserAesKey()
                else AesKeysPreferences(context, rsaParameterSpec)
    }

    @Singleton
    @Provides
    fun provideRSAParameterSpec(@ApplicationContext context: Context) : IRSAParameterSpec {
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) RSAGenEncryptSpecImpl()
                else RSAGeneratorSpecImpl(context)
    }

    @Singleton
    @Provides
    fun provideBioPreferences(preferences: DataStore<Preferences>): BioPreferences {
        return BioPreferences(preferences)
    }

    @Singleton
    @Provides
    fun provideBioModule(preferences: BioPreferences, @Named(PREFERENCES_BIOMETRIC) manager: CryptoManager) : BioModule {
        return BioModule(preferences, manager)
    }

    @Provides
    fun provideBioAesKey() : BioAesKey {
        return BioAesKey()
    }

    @Singleton
    @Provides
    fun provideCipherManager(aesKey: IAesKey) : CryptoManager {
        return CryptoManager(aesKey)
    }

    @Singleton
    @Provides
    @Named(PREFERENCES_BIOMETRIC)
    fun provideBioCipherManager(aesKey: BioAesKey) : CryptoManager {
        return CryptoManager(aesKey)
    }
}
