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

import android.content.Context
import ch.protonmail.android.mailcommon.data.dagger.MailCommonDataModule
import ch.protonmail.android.mailcommon.data.repository.AppLocaleRepositoryImpl
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.coroutines.MainDispatcher
import ch.protonmail.android.mailcommon.domain.repository.AppLocaleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(includes = [MailCommonDataModule::class])
@InstallIn(SingletonComponent::class)
object MailCommonModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IODispatcher
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    fun provideAppLocaleRepository(@ApplicationContext context: Context): AppLocaleRepository =
        AppLocaleRepositoryImpl(context)

    @Provides
    @Singleton
    @AppScope
    fun provideAppScope(@DefaultDispatcher dispatcher: CoroutineDispatcher) = CoroutineScope(dispatcher)
}
