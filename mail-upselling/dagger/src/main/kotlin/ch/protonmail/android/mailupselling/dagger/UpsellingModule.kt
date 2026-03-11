/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.dagger

import android.content.Context
import ch.protonmail.android.mailupselling.data.BlackFridayDataStoreProvider
import ch.protonmail.android.mailupselling.data.SpringPromoDataStoreProvider
import ch.protonmail.android.mailupselling.data.local.BlackFridayLocalDataSource
import ch.protonmail.android.mailupselling.data.local.BlackFridayLocalDataSourceImpl
import ch.protonmail.android.mailupselling.data.local.SpringPromoLocalDataSource
import ch.protonmail.android.mailupselling.data.local.SpringPromoLocalDataSourceImpl
import ch.protonmail.android.mailupselling.data.repository.BlackFridayRepositoryImpl
import ch.protonmail.android.mailupselling.data.repository.SpringPromoRepositoryImpl
import ch.protonmail.android.mailupselling.domain.annotation.PlayServicesAvailableValue
import ch.protonmail.android.mailupselling.domain.annotation.UpsellingCacheScope
import ch.protonmail.android.mailupselling.domain.repository.BlackFridayRepository
import ch.protonmail.android.mailupselling.domain.repository.SpringPromoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.proton.android.core.payment.google.domain.GoogleServicesAvailability
import me.proton.android.core.payment.google.domain.GoogleServicesUtils
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface UpsellingModule {

    @Binds
    @Reusable
    fun provideBlackFridayRepository(impl: BlackFridayRepositoryImpl): BlackFridayRepository

    @Binds
    @Singleton
    fun provideBlackFridayDataSource(impl: BlackFridayLocalDataSourceImpl): BlackFridayLocalDataSource

    @Binds
    @Reusable
    fun provideSpringPromoRepository(impl: SpringPromoRepositoryImpl): SpringPromoRepository

    @Binds
    @Singleton
    fun provideSpringPromoDataSource(impl: SpringPromoLocalDataSourceImpl): SpringPromoLocalDataSource

    @Module
    @InstallIn(SingletonComponent::class)
    object UpsellingModuleProvider {

        @Provides
        @Singleton
        fun provideDataStoreProvider(@ApplicationContext context: Context): BlackFridayDataStoreProvider =
            BlackFridayDataStoreProvider(context)

        @Provides
        @Singleton
        fun provideSpringPromoDataStoreProvider(@ApplicationContext context: Context): SpringPromoDataStoreProvider =
            SpringPromoDataStoreProvider(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    @UpsellingCacheScope
    fun provideScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @PlayServicesAvailableValue
    @Singleton
    fun providePlayServicesAvailable(gmsUtils: GoogleServicesUtils) =
        gmsUtils.getPlayServicesAvailability() == GoogleServicesAvailability.Success
}
