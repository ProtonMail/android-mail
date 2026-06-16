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

package ch.protonmail.android.mailcontentsearch.dagger

import android.content.Context
import ch.protonmail.android.mailcontentsearch.data.indexer.RustContentSearchIndexer
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.data.repository.ContentSearchPreferencesRepositoryImpl
import ch.protonmail.android.mailcontentsearch.data.repository.ContentSearchSettingsRepositoryImpl
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchIndexer
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchPreferencesRepository
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ContentSearchModule {

    @Binds
    @Singleton
    fun bindContentSearchIndexer(impl: RustContentSearchIndexer): ContentSearchIndexer

    @Binds
    @Singleton
    fun bindContentSearchSettingsRepository(impl: ContentSearchSettingsRepositoryImpl): ContentSearchSettingsRepository

    @Binds
    @Singleton
    fun bindContentSearchPreferencesRepository(
        impl: ContentSearchPreferencesRepositoryImpl
    ): ContentSearchPreferencesRepository

    companion object {

        @Provides
        @Singleton
        fun provideContentSearchDataStoreProvider(
            @ApplicationContext context: Context
        ): ContentSearchDataStoreProvider = ContentSearchDataStoreProvider(context)
    }
}
