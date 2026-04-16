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

package ch.protonmail.android.mailcommon.dagger

import ch.protonmail.android.mailcommon.data.network.NetworkManagerImpl
import ch.protonmail.android.mailcommon.data.repository.UndoRepositoryImpl
import ch.protonmail.android.mailcommon.data.system.BuildVersionProviderImpl
import ch.protonmail.android.mailcommon.data.system.ContentValuesProviderImpl
import ch.protonmail.android.mailcommon.data.system.DeviceCapabilitiesImpl
import ch.protonmail.android.mailcommon.data.system.SenderImageThemeProviderImpl
import ch.protonmail.android.mailcommon.data.usecase.IsDarkModeEnabledImpl
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.domain.repository.UndoRepository
import ch.protonmail.android.mailcommon.domain.system.BuildVersionProvider
import ch.protonmail.android.mailcommon.domain.system.ContentValuesProvider
import ch.protonmail.android.mailcommon.domain.system.DeviceCapabilities
import ch.protonmail.android.mailcommon.domain.usecase.IsDarkModeEnabled
import ch.protonmail.android.mailcommon.domain.usecase.SenderImageThemeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module(includes = [MailCommonDataModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object MailCommonDataModule {

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindsModule {

        @Binds
        fun bindDeviceCapabilities(impl: DeviceCapabilitiesImpl): DeviceCapabilities

        @Binds
        fun bindBuildVersionProvider(impl: BuildVersionProviderImpl): BuildVersionProvider

        @Binds
        fun bindContentValuesProvider(impl: ContentValuesProviderImpl): ContentValuesProvider

        @Binds
        fun bindNetworkManager(impl: NetworkManagerImpl): NetworkManager

        @Binds
        fun bindUndoRepository(impl: UndoRepositoryImpl): UndoRepository

        @Binds
        fun bindIsDarkModeEnabled(impl: IsDarkModeEnabledImpl): IsDarkModeEnabled

        @Binds
        fun bindSenderImageModeProvider(impl: SenderImageThemeProviderImpl): SenderImageThemeProvider

    }
}
