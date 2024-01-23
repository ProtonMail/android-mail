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

import ch.protonmail.android.di.NetworkConfigModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.network.data.di.BaseProtonApiUrl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockWebServer

/**
 * A test module used to override the [BaseProtonApiUrl] in UI Tests.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkConfigModule::class])
object NetworkConfigTestModule {

    @Provides
    @BaseProtonApiUrl
    fun provideBaseProtonApiUrl(
        @LocalhostApi localhostApi: Boolean,
        mockWebServer: MockWebServer,
        envConfig: EnvironmentConfiguration
    ): HttpUrl {
        return if (localhostApi) {
            runBlocking {
                withContext(Dispatchers.IO) {
                    mockWebServer.url("/")
                }
            }
        } else {
            // This is a temporary solution until we come up with an efficient environment switch.
            envConfig.baseUrl.toHttpUrl()
        }
    }
}
