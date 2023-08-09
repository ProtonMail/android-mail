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

package ch.protonmail.android.uitest.di

import java.security.SecureRandom
import java.security.cert.X509Certificate
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.spyk
import me.proton.core.network.dagger.CoreBaseNetworkModule
import me.proton.core.network.data.NetworkManager
import me.proton.core.network.data.di.SharedOkHttpClient
import me.proton.core.network.domain.NetworkManager
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * A test module that overrides the [CoreBaseNetworkModule] to allow HTTPS connections between the app and
 * the local [MockWebServer] with a customized [OkHttpClient].
 *
 * The provided [NetworkManager] is the same as the one currently used in production code.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoreBaseNetworkModule::class])
class CoreBaseNetworkTestModule {

    private val testX509TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    @Provides
    @Reusable
    @TestClientSSLSocketFactory
    internal fun provideTestClientSSLSocketFactory(): SSLSocketFactory {
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(testX509TrustManager), SecureRandom())
        }.socketFactory
    }

    @Provides
    @Singleton
    @SharedOkHttpClient
    internal fun provideOkHttpClient(@TestClientSSLSocketFactory sslSocketFactory: SSLSocketFactory): OkHttpClient =
        OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, testX509TrustManager).build()

    @Provides
    @Singleton
    internal fun provideNetworkManager(@ApplicationContext context: Context): NetworkManager =
        spyk(NetworkManager(context))
}
