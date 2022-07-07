/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import android.content.Context
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.di.ApplicationModule.LocalDiskOpCoroutineScope
import ch.protonmail.android.feature.alternativerouting.HasAlternativeRouting
import ch.protonmail.android.feature.forceupdate.ForceUpdateHandler
import ch.protonmail.android.useragent.BuildUserAgent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import me.proton.core.auth.data.MissingScopeListenerImpl
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.humanverification.data.utils.NetworkRequestOverriderImpl
import me.proton.core.humanverification.domain.utils.NetworkRequestOverrider
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.data.NetworkManager
import me.proton.core.network.data.NetworkPrefs
import me.proton.core.network.data.ProtonCookieStore
import me.proton.core.network.data.client.ClientIdProviderImpl
import me.proton.core.network.data.client.ClientVersionValidatorImpl
import me.proton.core.network.data.client.ExtraHeaderProviderImpl
import me.proton.core.network.data.di.Constants
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkPrefs
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.network.domain.client.ClientVersionValidator
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.network.domain.humanverification.HumanVerificationListener
import me.proton.core.network.domain.humanverification.HumanVerificationProvider
import me.proton.core.network.domain.scopes.MissingScopeListener
import me.proton.core.network.domain.server.ServerTimeListener
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

private const val TEN_MEGABYTES = 10L * 1024L * 1024L

@Module
@InstallIn(SingletonComponent::class)
@Suppress("LongParameterList")
object NetworkModule {

    const val HOST = BuildConfig.HOST
    const val API_HOST = "api.$HOST"
    const val BASE_URL = "https://$API_HOST"

    private val certificatePins: Array<String> =
        Constants.DEFAULT_SPKI_PINS.takeIf { BuildConfig.USE_DEFAULT_PINS } ?: emptyArray()

    private val alternativeApiPins: List<String> =
        Constants.ALTERNATIVE_API_SPKI_PINS.takeIf { BuildConfig.USE_DEFAULT_PINS } ?: emptyList()

    @Provides
    @Singleton
    fun provideApiClient(
        buildUserAgent: BuildUserAgent,
        forceUpdateHandler: ForceUpdateHandler,
        hasAlternativeRouting: HasAlternativeRouting
    ) = object : ApiClient {
        override val appVersionHeader: String
            get() = "android-mail@${BuildConfig.VERSION_NAME}"
        override val enableDebugLogging: Boolean
            get() = true
        override val shouldUseDoh: Boolean
            get() = hasAlternativeRouting().value.isEnabled
        override val userAgent: String
            get() = buildUserAgent()

        override fun forceUpdate(errorMessage: String) {
            forceUpdateHandler.onForceUpdate(errorMessage)
        }
    }

    @Provides
    @Singleton
    @LocalDiskOpCoroutineScope
    fun provideLocalDiskOpCoroutineScope(): CoroutineScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    fun provideNetworkManager(
        @ApplicationContext context: Context
    ): NetworkManager = NetworkManager(context)

    @Provides
    @Singleton
    fun provideNetworkPrefs(
        @ApplicationContext context: Context
    ): NetworkPrefs = NetworkPrefs(context)

    @Provides
    fun provideNetworkRequestOverrider(): NetworkRequestOverrider = NetworkRequestOverriderImpl(OkHttpClient())

    @Provides
    @Singleton
    fun provideMissingScopeListener(): MissingScopeListener = MissingScopeListenerImpl()

    @Provides
    @Singleton
    fun provideProtonCookieStore(
        @ApplicationContext context: Context
    ): ProtonCookieStore = ProtonCookieStore(context)

    @Provides
    @Singleton
    fun provideClientIdProvider(
        protonCookieStore: ProtonCookieStore
    ): ClientIdProvider = ClientIdProviderImpl(BASE_URL, protonCookieStore)

    @Provides
    @Singleton
    fun provideServerTimeListener(
        context: CryptoContext
    ): ServerTimeListener = object : ServerTimeListener {
        override fun onServerTimeUpdated(epochSeconds: Long) {
            context.pgpCrypto.updateTime(epochSeconds)
        }
    }

    @Provides
    @Singleton
    fun provideExtraHeaderProvider(): ExtraHeaderProvider = ExtraHeaderProviderImpl().apply {
        val proxyToken: String? = BuildConfig.PROXY_TOKEN
        proxyToken?.takeIfNotBlank()?.let { addHeaders("X-atlas-secret" to it) }
    }

    @Provides
    @Singleton
    fun provideApiFactory(
        @ApplicationContext context: Context,
        apiClient: ApiClient,
        clientIdProvider: ClientIdProvider,
        serverTimeListener: ServerTimeListener,
        networkManager: NetworkManager,
        networkPrefs: NetworkPrefs,
        missingScopeListener: MissingScopeListener,
        protonCookieStore: ProtonCookieStore,
        sessionProvider: SessionProvider,
        sessionListener: SessionListener,
        humanVerificationProvider: HumanVerificationProvider,
        humanVerificationListener: HumanVerificationListener,
        extraHeaderProvider: ExtraHeaderProvider,
        clientVersionValidator: ClientVersionValidator
    ): ApiManagerFactory = ApiManagerFactory(
        baseUrl = BASE_URL,
        apiClient = apiClient,
        clientIdProvider = clientIdProvider,
        serverTimeListener = serverTimeListener,
        networkManager = networkManager,
        prefs = networkPrefs,
        sessionProvider = sessionProvider,
        sessionListener = sessionListener,
        humanVerificationProvider = humanVerificationProvider,
        humanVerificationListener = humanVerificationListener,
        missingScopeListener = missingScopeListener,
        cookieStore = protonCookieStore,
        scope = CoroutineScope(Job() + Dispatchers.Default),
        certificatePins = certificatePins,
        alternativeApiPins = alternativeApiPins,
        cache = {
            Cache(
                directory = File(context.cacheDir, "http_cache"),
                maxSize = TEN_MEGABYTES
            )
        },
        extraHeaderProvider = extraHeaderProvider,
        clientVersionValidator = clientVersionValidator,
        dohAlternativesListener = null
    )

    @Provides
    @Singleton
    fun provideApiProvider(
        apiManagerFactory: ApiManagerFactory,
        sessionProvider: SessionProvider
    ): ApiProvider = ApiProvider(apiManagerFactory, sessionProvider)

    @Provides
    fun provideClientVersionValidator(): ClientVersionValidator = ClientVersionValidatorImpl()
}
