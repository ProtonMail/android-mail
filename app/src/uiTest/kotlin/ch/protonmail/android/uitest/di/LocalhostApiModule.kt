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

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Qualifier

/**
 * Convenient annotation to ease the injection of the [Boolean] flag indicating whether we want to force
 * the use of localhost when resolving the base URL for UI Tests.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalhostApi

/**
 * This is a custom test module that determines whether the API calls shall be mocked in UI Tests.
 *
 * Since we can't use [TestInstallIn] multiple times in our tests and we don't want to bloat the test suites with
 * multiple [InstallIn], [LocalhostApi] is introduced to set whether tests should run in complete network isolation.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocalhostApiModule {

    @Provides
    @LocalhostApi
    fun useLocalhostApi(): Boolean = true
}
