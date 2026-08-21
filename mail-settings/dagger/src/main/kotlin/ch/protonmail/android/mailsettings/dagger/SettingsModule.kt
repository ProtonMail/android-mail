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

package ch.protonmail.android.mailsettings.dagger

import android.content.Context
import ch.protonmail.android.mailcommon.domain.repository.AppLocaleRepository
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.repository.MailSettingsRefreshRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.data.InMemoryToolbarActionsRepositoryImpl
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.data.local.AutoAdvanceDataSource
import ch.protonmail.android.mailsettings.data.local.AutoAdvanceDataSourceImpl
import ch.protonmail.android.mailsettings.data.local.MailSettingsDataSource
import ch.protonmail.android.mailsettings.data.local.MobileSignatureDataSource
import ch.protonmail.android.mailsettings.data.local.MobileSignatureDataSourceImpl
import ch.protonmail.android.mailsettings.data.local.RustMailSettingsDataSource
import ch.protonmail.android.mailsettings.data.local.RustToolbarActionSettingsDataSource
import ch.protonmail.android.mailsettings.data.local.SwipeNextDataSource
import ch.protonmail.android.mailsettings.data.local.SwipeNextDataSourceImpl
import ch.protonmail.android.mailsettings.data.local.ToolbarActionSettingsDataSource
import ch.protonmail.android.mailsettings.data.repository.AppLanguageRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.AutoAdvanceRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.BackgroundSyncSettingRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.CombinedContactsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.LocalStorageDataRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.MobileSignatureRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.NotificationsSettingsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.PreventScreenshotsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.RustMailSettingsRepository
import ch.protonmail.android.mailsettings.data.repository.SwipeNextRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.ToolbarActionsRepositoryImpl
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.domain.repository.AutoAdvanceRepository
import ch.protonmail.android.mailsettings.domain.repository.BackgroundSyncSettingRepository
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import ch.protonmail.android.mailsettings.domain.repository.InMemoryToolbarActionsRepository
import ch.protonmail.android.mailsettings.domain.repository.LocalStorageDataRepository
import ch.protonmail.android.mailsettings.domain.repository.MailSettingsRepository
import ch.protonmail.android.mailsettings.domain.repository.MobileSignatureRepository
import ch.protonmail.android.mailsettings.domain.repository.NotificationsSettingsRepository
import ch.protonmail.android.mailsettings.domain.repository.PreventScreenshotsRepository
import ch.protonmail.android.mailsettings.domain.repository.SwipeNextRepository
import ch.protonmail.android.mailsettings.domain.repository.ToolbarActionsRepository
import ch.protonmail.android.mailsettings.domain.usecase.HandleCloseWebSettings
import ch.protonmail.android.mailsettings.presentation.settings.theme.ThemeObserverCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import ch.protonmail.android.mailsettings.data.repository.AppSettingsRepository as AppSettingsRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideDataStoreProvider(@ApplicationContext context: Context): MailSettingsDataStoreProvider =
        MailSettingsDataStoreProvider(context)

    @Provides
    @Singleton
    fun provideCombinedContactsRepository(
        dataStoreProvider: MailSettingsDataStoreProvider
    ): CombinedContactsRepository = CombinedContactsRepositoryImpl(dataStoreProvider)

    @Provides
    @Singleton
    fun provideAppLanguageRepository(appLocaleRepository: AppLocaleRepository): AppLanguageRepository =
        AppLanguageRepositoryImpl(appLocaleRepository)

    @Provides
    @Singleton
    fun provideNotificationExtendedRepository(
        dataStoreProvider: MailSettingsDataStoreProvider
    ): NotificationsSettingsRepository = NotificationsSettingsRepositoryImpl(dataStoreProvider)

    @Provides
    @Singleton
    fun providePreventScreenshotsRepository(
        dataStoreProvider: MailSettingsDataStoreProvider
    ): PreventScreenshotsRepository = PreventScreenshotsRepositoryImpl(dataStoreProvider)

    @Provides
    @Singleton
    @ThemeObserverCoroutineScope
    fun provideThemeObserverCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    fun provideMailSettingsRepository(mailSettingsDataSource: MailSettingsDataSource): MailSettingsRepository =
        RustMailSettingsRepository(mailSettingsDataSource)

    @Provides
    @Singleton
    fun provideHandleCloseWebSettings(
        observePrimaryUserId: ObservePrimaryUserId,
        eventLoopRepository: EventLoopRepository,
        mailSettingsRefreshRepository: MailSettingsRefreshRepository
    ): HandleCloseWebSettings =
        HandleCloseWebSettings(observePrimaryUserId, eventLoopRepository, mailSettingsRefreshRepository)

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {

        @Binds
        @Reusable
        fun provideBackgroundSyncRepository(impl: BackgroundSyncSettingRepositoryImpl): BackgroundSyncSettingRepository

        @Binds
        @Reusable
        fun bindToolbarActionsDataSource(impl: RustToolbarActionSettingsDataSource): ToolbarActionSettingsDataSource

        @Binds
        @Reusable
        fun bindToolbarActionsRepository(impl: ToolbarActionsRepositoryImpl): ToolbarActionsRepository

        @Binds
        @Reusable
        fun bindLocalDataRepository(impl: LocalStorageDataRepositoryImpl): LocalStorageDataRepository

        @Binds
        fun bindsMailSettingsDataSource(impl: RustMailSettingsDataSource): MailSettingsDataSource

        @Binds
        @Singleton
        fun bindAppSettingsRepository(impl: AppSettingsRepositoryImpl): AppSettingsRepository

        @Binds
        fun bindsMobileSignatureDataSource(impl: MobileSignatureDataSourceImpl): MobileSignatureDataSource

        @Binds
        @Singleton
        fun bindsMobileSignatureRepository(impl: MobileSignatureRepositoryImpl): MobileSignatureRepository

        @Binds
        fun autoAdvanceDataSource(impl: AutoAdvanceDataSourceImpl): AutoAdvanceDataSource

        @Binds
        fun bindsAutoAdvanceRepository(impl: AutoAdvanceRepositoryImpl): AutoAdvanceRepository

        @Binds
        fun bindsSwipeNextDataSource(impl: SwipeNextDataSourceImpl): SwipeNextDataSource

        @Binds
        fun bindsSwipeNextRepository(impl: SwipeNextRepositoryImpl): SwipeNextRepository
    }

    @Module
    @InstallIn(ViewModelComponent::class)
    internal interface ViewModelBindings {

        @Binds
        fun bindInMemoryToolbarPreferenceRepository(
            implementation: InMemoryToolbarActionsRepositoryImpl
        ): InMemoryToolbarActionsRepository
    }
}
