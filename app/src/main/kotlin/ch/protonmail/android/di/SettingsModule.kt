/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import android.content.Context
import ch.protonmail.android.mailsettings.data.DataStoreProvider
import ch.protonmail.android.mailsettings.data.repository.AlternativeRoutingRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.AutoLockRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.CombinedContactsRepositoryImpl
import ch.protonmail.android.mailsettings.data.repository.CustomAppLanguageRepositoryImpl
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import ch.protonmail.android.mailsettings.domain.repository.CustomAppLanguageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideDataStoreProvider(
        @ApplicationContext context: Context
    ): DataStoreProvider = DataStoreProvider(context)

    @Provides
    @Singleton
    fun provideAutoLockRepository(
        dataStoreProvider: DataStoreProvider
    ): AutoLockRepository = AutoLockRepositoryImpl(dataStoreProvider)

    @Provides
    @Singleton
    fun provideAlternativeRoutingRepository(
        dataStoreProvider: DataStoreProvider
    ): AlternativeRoutingRepository = AlternativeRoutingRepositoryImpl(dataStoreProvider)

    @Provides
    @Singleton
    fun provideCombinedContactsRepository(
        dataStoreProvider: DataStoreProvider
    ): CombinedContactsRepository = CombinedContactsRepositoryImpl(dataStoreProvider)

    @Provides
    @Singleton
    fun provideCustomLanguageRepository(): CustomAppLanguageRepository =
        CustomAppLanguageRepositoryImpl()
}
