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

import ch.protonmail.android.BuildConfig
import ch.protonmail.android.di.ApplicationModule.LocalDiskOpCoroutineScope
import ch.protonmail.android.feature.alternativerouting.HasAlternativeRouting
import ch.protonmail.android.feature.forceupdate.ForceUpdateHandler
import ch.protonmail.android.useragent.BuildUserAgent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.network.data.client.ExtraHeaderProviderImpl
import me.proton.core.network.data.di.AlternativeApiPins
import me.proton.core.network.data.di.BaseProtonApiUrl
import me.proton.core.network.data.di.CertificatePins
import me.proton.core.network.data.di.Constants
import me.proton.core.network.data.di.DohProviderUrls
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.network.domain.serverconnection.DohAlternativesListener
import me.proton.core.util.kotlin.takeIfNotBlank
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("LongParameterList")
object NetworkModule {
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
            get() = BuildConfig.DEBUG

        override val userAgent: String get() = buildUserAgent()

        override suspend fun shouldUseDoh(): Boolean = hasAlternativeRouting().value.isEnabled

        override fun forceUpdate(errorMessage: String) {
            forceUpdateHandler.onForceUpdate(errorMessage)
        }
    }

    @Provides
    @Singleton
    @LocalDiskOpCoroutineScope
    fun provideLocalDiskOpCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    fun provideExtraHeaderProvider(): ExtraHeaderProvider = ExtraHeaderProviderImpl().apply {
        val proxyToken: String? = BuildConfig.PROXY_TOKEN
        proxyToken?.takeIfNotBlank()?.let { addHeaders("X-atlas-secret" to it) }
    }

    @DohProviderUrls
    @Provides
    fun provideDohProviderUrls(): Array<String> = Constants.DOH_PROVIDERS_URLS

    @CertificatePins
    @Provides
    fun provideCertificatePins(): Array<String> =
        Constants.DEFAULT_SPKI_PINS.takeIf { BuildConfig.USE_DEFAULT_PINS } ?: emptyArray()

    @AlternativeApiPins
    @Provides
    fun provideAlternativeApiPins(): List<String> =
        Constants.ALTERNATIVE_API_SPKI_PINS.takeIf { BuildConfig.USE_DEFAULT_PINS } ?: emptyList()

    @Provides
    @Singleton
    fun provideDohAlternativesListener(): DohAlternativesListener? = null
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkConfigModule {

    @Provides
    @BaseProtonApiUrl
    fun provideProtonApiUrl(envConfig: EnvironmentConfiguration): HttpUrl = envConfig.baseUrl.toHttpUrl()
}
