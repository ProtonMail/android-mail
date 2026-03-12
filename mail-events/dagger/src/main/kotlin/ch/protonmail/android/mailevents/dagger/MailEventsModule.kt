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

package ch.protonmail.android.mailevents.dagger

import android.content.Context
import ch.protonmail.android.mailevents.data.device.DeviceInfoProviderImpl
import ch.protonmail.android.mailevents.data.local.EventsDataStoreProvider
import ch.protonmail.android.mailevents.data.local.MailEventsDataSource
import ch.protonmail.android.mailevents.data.local.MailEventsDataSourceImpl
import ch.protonmail.android.mailevents.data.referrer.InstallReferrerDataSource
import ch.protonmail.android.mailevents.data.referrer.PlayInstallReferrerDataSourceImpl
import ch.protonmail.android.mailevents.data.remote.EventsDataSource
import ch.protonmail.android.mailevents.data.remote.RustEventsDataSource
import ch.protonmail.android.mailevents.data.repository.AppInstallRepositoryImpl
import ch.protonmail.android.mailevents.data.repository.EventsRepositoryImpl
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.AppEventBroadcasterImpl
import ch.protonmail.android.mailevents.domain.repository.AppInstallRepository
import ch.protonmail.android.mailevents.domain.repository.DeviceInfoProvider
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import me.proton.android.core.events.domain.AccountEventBroadcasterImpl
import javax.inject.Singleton

@Module(includes = [MailEventsModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object MailEventsModule {

    @Provides
    @Singleton
    fun provideEventsDataStoreProvider(@ApplicationContext context: Context): EventsDataStoreProvider =
        EventsDataStoreProvider(context)

    @Provides
    @Singleton
    fun provideInstallReferrerDataSource(@ApplicationContext context: Context): InstallReferrerDataSource =
        PlayInstallReferrerDataSourceImpl(context)

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindsModule {

        @Binds
        @Reusable
        fun bindsEventsDataSource(impl: MailEventsDataSourceImpl): MailEventsDataSource

        @Binds
        @Reusable
        fun bindsEventsRemoteDataSource(impl: RustEventsDataSource): EventsDataSource

        @Binds
        @Reusable
        fun bindsEventsRepository(impl: EventsRepositoryImpl): EventsRepository

        @Binds
        @Reusable
        fun bindsDeviceInfoProvider(impl: DeviceInfoProviderImpl): DeviceInfoProvider


        @Binds
        @Reusable
        fun bindsAppInstallRepository(impl: AppInstallRepositoryImpl): AppInstallRepository
    }
}

@InstallIn(SingletonComponent::class)
@Module
interface MailEventsBroadcasterModule {

    @Binds
    @Singleton
    fun bindAppEventBroadcaster(impl: AppEventBroadcasterImpl): AppEventBroadcaster

    @Binds
    @Singleton
    fun bindAccountEventBroadcaster(impl: AccountEventBroadcasterImpl): AccountEventBroadcaster
}
