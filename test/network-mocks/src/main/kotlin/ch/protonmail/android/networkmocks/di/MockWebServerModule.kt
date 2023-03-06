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

package ch.protonmail.android.networkmocks.di

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLSocketFactory

/**
 * A test module that provides a [MockWebServer] instance with HTTPS support enabled by a custom [SSLSocketFactory].
 */
@Module
@InstallIn(SingletonComponent::class)
object MockWebServerModule {

    @Provides
    @Singleton
    fun provideMockWebServer(@TestServerSSLSocketFactory socketFactory: SSLSocketFactory): MockWebServer =
        MockWebServer().apply { useHttps(socketFactory, false) }

    @Provides
    @Reusable
    @TestServerSSLSocketFactory
    fun provideTestSSLSocketFactory(): SSLSocketFactory {
        val localhost = runBlocking {
            withContext(Dispatchers.IO) {
                InetAddress.getByName("localhost").canonicalHostName
            }
        }

        val localhostCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName(localhost)
            .duration(1, TimeUnit.DAYS)
            .build()

        val handshakeCertificates = HandshakeCertificates.Builder()
            .heldCertificate(localhostCertificate)
            .build()

        return handshakeCertificates.sslSocketFactory()
    }
}
