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

package ch.protonmail.android.mailspotlight.dagger

import android.content.Context
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import ch.protonmail.android.mailspotlight.data.local.FeatureSpotlightLocalDataSource
import ch.protonmail.android.mailspotlight.data.local.FeatureSpotlightLocalDataSourceImpl
import ch.protonmail.android.mailspotlight.data.repository.FeatureSpotlightRepositoryImpl
import ch.protonmail.android.mailspotlight.domain.repository.FeatureSpotlightRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module(includes = [FeatureSpotlightModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object FeatureSpotlightModule {

    @Provides
    @Singleton
    fun provideSpotlightDataStoreProvider(@ApplicationContext context: Context): FeatureSpotlightDataStoreProvider =
        FeatureSpotlightDataStoreProvider(context)

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindsModule {

        @Binds
        @Reusable
        fun bindsFeatureSpotlightLocalDataSource(
            impl: FeatureSpotlightLocalDataSourceImpl
        ): FeatureSpotlightLocalDataSource

        @Binds
        @Reusable
        fun bindsFeatureSpotlightRepository(impl: FeatureSpotlightRepositoryImpl): FeatureSpotlightRepository
    }
}
