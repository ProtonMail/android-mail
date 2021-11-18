/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import android.content.Context
import android.os.Build
import android.os.Build.VERSION
import ch.protonmail.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.data.NetworkManager
import me.proton.core.network.data.NetworkPrefs
import me.proton.core.network.data.ProtonCookieStore
import me.proton.core.network.data.client.ClientIdProviderImpl
import me.proton.core.network.data.client.ExtraHeaderProviderImpl
import me.proton.core.network.data.di.Constants
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkPrefs
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.network.domain.humanverification.HumanVerificationListener
import me.proton.core.network.domain.humanverification.HumanVerificationProvider
import me.proton.core.network.domain.server.ServerTimeListener
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import okhttp3.Cache
import java.io.File
import java.util.Locale
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
    fun provideApiClient() = object : ApiClient {
        override val appVersionHeader: String
            get() = "Android_${BuildConfig.VERSION_NAME}"
        override val enableDebugLogging: Boolean
            get() = true
        override val shouldUseDoh: Boolean
            get() = false
        override val userAgent: String
            get() = buildUserAgent()

        override fun forceUpdate(errorMessage: String) = Unit

        private fun buildUserAgent() = "ProtonMail/${BuildConfig.VERSION_NAME} (Android " +
                "${VERSION.RELEASE}; ${Build.MODEL}; ${Build.BRAND}; ${Build.DEVICE}; " +
                "${Locale.getDefault().language})"
    }

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
        protonCookieStore: ProtonCookieStore,
        sessionProvider: SessionProvider,
        sessionListener: SessionListener,
        humanVerificationProvider: HumanVerificationProvider,
        humanVerificationListener: HumanVerificationListener,
        extraHeaderProvider: ExtraHeaderProvider
    ): ApiManagerFactory = ApiManagerFactory(
        BASE_URL,
        apiClient,
        clientIdProvider,
        serverTimeListener,
        networkManager,
        networkPrefs,
        sessionProvider,
        sessionListener,
        humanVerificationProvider,
        humanVerificationListener,
        protonCookieStore,
        CoroutineScope(Job() + Dispatchers.Default),
        certificatePins,
        alternativeApiPins,
        {
            Cache(
                directory = File(context.cacheDir, "http_cache"),
                maxSize = TEN_MEGABYTES
            )
        },
        extraHeaderProvider,
    )

    @Provides
    @Singleton
    fun provideApiProvider(
        apiManagerFactory: ApiManagerFactory,
        sessionProvider: SessionProvider
    ): ApiProvider = ApiProvider(apiManagerFactory, sessionProvider)
}
