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

package ch.protonmail.android.mailcomposer.dagger

import ch.protonmail.android.composer.data.local.DraftStateDatabase
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSource
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSourceImpl
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSource
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSourceImpl
import ch.protonmail.android.composer.data.repository.DraftRepositoryImpl
import ch.protonmail.android.composer.data.repository.DraftStateRepositoryImpl
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.network.data.ApiProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailComposerModule {

    @Provides
    @Singleton
    fun provideDraftRepository(enqueuer: Enqueuer): DraftRepository = DraftRepositoryImpl(enqueuer)

    @Provides
    @Singleton
    fun provideDraftStateRepository(localDataSource: DraftStateLocalDataSource): DraftStateRepository =
        DraftStateRepositoryImpl(localDataSource)

    @Provides
    @Singleton
    fun provideDraftStateLocalDataSource(draftStateDatabase: DraftStateDatabase): DraftStateLocalDataSource =
        DraftStateLocalDataSourceImpl(draftStateDatabase)

    @Provides
    @Singleton
    fun provideDraftStateRemoteDataSource(apiProvider: ApiProvider): DraftRemoteDataSource =
        DraftRemoteDataSourceImpl(apiProvider)
}
