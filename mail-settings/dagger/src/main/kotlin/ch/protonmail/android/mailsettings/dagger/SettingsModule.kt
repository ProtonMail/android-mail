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
import ch.protonmail.android.mailsettings.data.MailSettingsDataStoreProvider
import ch.protonmail.android.mailsettings.data.repository.AddressIdentityRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.AlternativeRoutingRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.AppLanguageRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.AutoLockRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.BackgroundSyncSettingRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.BiometricsSystemStateRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.CombinedContactsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.InMemoryToolbarPreferenceRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.LocalSpotlightEventsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.LocalStorageDataRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.MobileFooterRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.NotificationsSettingsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.PreventScreenshotsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.ThemeRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.local.AddressIdentityLocalDataSource
import ch.protonmail.android.mailsettings.data.repository.local.AddressIdentityLocalDataSourceImpl
import ch.protonmail.android.mailsettings.data.repository.local.AlternativeRoutingLocalDataSource
import ch.protonmail.android.mailsettings.data.repository.local.AlternativeRoutingLocalDataSourceImpl
import ch.protonmail.android.mailsettings.data.repository.local.AutoLockLocalDataSource
import ch.protonmail.android.mailsettings.data.repository.local.AutoLockLocalDataSourceImpl
import ch.protonmail.android.mailsettings.data.repository.local.MobileFooterLocalDataSource
import ch.protonmail.android.mailsettings.data.repository.local.MobileFooterLocalDataSourceImpl
import ch.protonmail.android.mailsettings.data.repository.remote.AddressIdentityRemoteDataSource
import ch.protonmail.android.mailsettings.data.repository.remote.AddressIdentityRemoteDataSourceImpl
import ch.protonmail.android.mailsettings.domain.annotations.AutodeleteFeatureEnabled
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import ch.protonmail.android.mailsettings.domain.repository.BackgroundSyncSettingRepository
import ch.protonmail.android.mailsettings.domain.repository.BiometricsSystemStateRepository
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import ch.protonmail.android.mailsettings.domain.repository.InMemoryToolbarPreferenceRepository
import ch.protonmail.android.mailsettings.domain.repository.LocalSpotlightEventsRepository
import ch.protonmail.android.mailsettings.domain.repository.LocalStorageDataRepository
import ch.protonmail.android.mailsettings.domain.repository.MobileFooterRepository
import ch.protonmail.android.mailsettings.domain.repository.NotificationsSettingsRepository
import ch.protonmail.android.mailsettings.domain.repository.PreventScreenshotsRepository
import ch.protonmail.android.mailsettings.domain.repository.ThemeRepository
import ch.protonmail.android.mailsettings.domain.usecase.IsAutodeleteFeatureEnabled
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

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideDataStoreProvider(@ApplicationContext context: Context): MailSettingsDataStoreProvider =
        MailSettingsDataStoreProvider(context)

    @Provides
    @Singleton
    fun provideAlternativeRoutingLocalDataSource(
        dataStoreProvider: MailSettingsDataStoreProvider
    ): AlternativeRoutingLocalDataSource = AlternativeRoutingLocalDataSourceImpl(dataStoreProvider)

    @Provides
    @Singleton
    fun provideAlternativeRoutingRepository(
        alternativeRoutingLocalDataSource: AlternativeRoutingLocalDataSource
    ): AlternativeRoutingRepository = AlternativeRoutingRepositoryImpl(alternativeRoutingLocalDataSource)

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
    fun provideThemeRepository(dataStoreProvider: MailSettingsDataStoreProvider): ThemeRepository =
        ThemeRepositoryImpl(dataStoreProvider)

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
    @AutodeleteFeatureEnabled
    fun provideAutodeleteFeatureEnabled(isEnabled: IsAutodeleteFeatureEnabled) = isEnabled(null)

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {

        @Binds
        @Reusable
        fun provideBackgroundSyncRepository(impl: BackgroundSyncSettingRepositoryImpl): BackgroundSyncSettingRepository

        @Binds
        @Reusable
        fun bindAddressIdentityRemoteDataSource(
            impl: AddressIdentityRemoteDataSourceImpl
        ): AddressIdentityRemoteDataSource

        @Binds
        @Reusable
        fun bindAddressIdentityLocalDataSource(impl: AddressIdentityLocalDataSourceImpl): AddressIdentityLocalDataSource

        @Binds
        @Reusable
        fun bindAddressIdentityRepository(impl: AddressIdentityRepositoryImpl): AddressIdentityRepository

        @Binds
        @Reusable
        fun bindMobileFooterRepository(impl: MobileFooterRepositoryImpl): MobileFooterRepository

        @Binds
        @Singleton
        fun bindMobileLocalFooterLocalDataSource(impl: MobileFooterLocalDataSourceImpl): MobileFooterLocalDataSource

        @Binds
        @Reusable
        fun bindLocalDataRepository(impl: LocalStorageDataRepositoryImpl): LocalStorageDataRepository

        @Binds
        @Reusable
        fun bindAutoLockRepository(impl: AutoLockRepositoryImpl): AutoLockRepository

        @Binds
        @Singleton
        fun bindAutoLockLocalDataSource(impl: AutoLockLocalDataSourceImpl): AutoLockLocalDataSource

        @Binds
        fun bindBiometricsSystemStateRepository(
            impl: BiometricsSystemStateRepositoryImpl
        ): BiometricsSystemStateRepository

        @Binds
        fun bindLocalSpotlightEventsRepository(impl: LocalSpotlightEventsRepositoryImpl): LocalSpotlightEventsRepository
    }
}

@Module
@InstallIn(ViewModelComponent::class)
internal interface ViewModelBindings {

    @Binds
    fun bindInMemoryToolbarPreferenceRepository(
        implementation: InMemoryToolbarPreferenceRepositoryImpl
    ): InMemoryToolbarPreferenceRepository
}
