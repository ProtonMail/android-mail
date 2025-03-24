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

package ch.protonmail.android.mailnotifications.dagger

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import ch.protonmail.android.mailnotifications.data.local.NotificationPermissionLocalDataSource
import ch.protonmail.android.mailnotifications.data.local.NotificationPermissionLocalDataSourceImpl
import ch.protonmail.android.mailnotifications.data.local.NotificationTokenLocalDataSource
import ch.protonmail.android.mailnotifications.data.local.NotificationTokenLocalDataSourceImpl
import ch.protonmail.android.mailnotifications.data.local.NotificationTokenPreferences
import ch.protonmail.android.mailnotifications.data.local.fcm.FcmTokenPreferencesImpl
import ch.protonmail.android.mailnotifications.data.remote.NotificationTokenRemoteDataSource
import ch.protonmail.android.mailnotifications.data.remote.NotificationTokenRemoteDataSourceImpl
import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionRepository
import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionRepositoryImpl
import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionTelemetryRepository
import ch.protonmail.android.mailnotifications.data.repository.NotificationPermissionTelemetryRepositoryImpl
import ch.protonmail.android.mailnotifications.data.repository.NotificationTokenRepository
import ch.protonmail.android.mailnotifications.data.repository.NotificationTokenRepositoryImpl
import ch.protonmail.android.mailnotifications.domain.handler.AccountStateAwareNotificationHandler
import ch.protonmail.android.mailnotifications.domain.handler.NotificationHandler
import ch.protonmail.android.mailnotifications.domain.handler.SessionAwareNotificationHandler
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxyImpl
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailNotificationsModule {

    @Provides
    @Reusable
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Reusable
    fun provideNotificationManagerCompat(@ApplicationContext context: Context): NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EntryPointModule {

        fun handlers(): Set<NotificationHandler>
    }

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindsModule {

        @Binds
        @Singleton
        fun bindFcmTokenPreferences(implementation: FcmTokenPreferencesImpl): NotificationTokenPreferences

        @Binds
        @Reusable
        fun bindNotificationTokenRemoteDataSource(
            dataSource: NotificationTokenRemoteDataSourceImpl
        ): NotificationTokenRemoteDataSource

        @Binds
        @Reusable
        fun bindNotificationTokenLocalDataSource(
            dataSource: NotificationTokenLocalDataSourceImpl
        ): NotificationTokenLocalDataSource

        @Binds
        @Reusable
        fun bindNotificationTokenRepository(repository: NotificationTokenRepositoryImpl): NotificationTokenRepository

        @Binds
        @Reusable
        fun bindNotificationManagerCompatProxy(
            notificationManagerProxyImpl: NotificationManagerCompatProxyImpl
        ): NotificationManagerCompatProxy

        @Binds
        @Singleton
        @IntoSet
        fun bindAccountStateAwareNotificationHandler(
            handlerImpl: AccountStateAwareNotificationHandler
        ): NotificationHandler

        @Binds
        @Singleton
        @IntoSet
        fun bindSessionAwareNotificationHandler(handlerImpl: SessionAwareNotificationHandler): NotificationHandler

        @Binds
        @Singleton
        fun bindNotificationPermissionLocalData(
            dataSource: NotificationPermissionLocalDataSourceImpl
        ): NotificationPermissionLocalDataSource

        @Binds
        @Singleton
        fun bindNotificationPermissionRepository(
            repository: NotificationPermissionRepositoryImpl
        ): NotificationPermissionRepository

        @Binds
        @Singleton
        fun bindNotificationPermissionTelemetryRepo(
            repository: NotificationPermissionTelemetryRepositoryImpl
        ): NotificationPermissionTelemetryRepository
    }
}
