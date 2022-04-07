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

package ch.protonmail.android.mailconversation.dagger

import ch.protonmail.android.mailconversation.data.local.ConversationDatabase
import ch.protonmail.android.mailconversation.data.local.ConversationLocalDataSourceImpl
import ch.protonmail.android.mailconversation.data.remote.ConversationRemoteDataSourceImpl
import ch.protonmail.android.mailconversation.data.repository.ConversationRepositoryImpl
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.network.data.ApiProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailConversationModule {

    @Provides
    @Singleton
    fun provideConversationRepositoryImpl(
        remoteDataSource: ConversationRemoteDataSource,
        localDataSource: ConversationLocalDataSource,
    ): ConversationRepository = ConversationRepositoryImpl(remoteDataSource, localDataSource)

    @Provides
    @Singleton
    fun provideConversationRemoteDataSource(
        apiProvider: ApiProvider,
    ): ConversationRemoteDataSource = ConversationRemoteDataSourceImpl(apiProvider)

    @Provides
    @Singleton
    fun provideConversationLocalDataSource(
        db: ConversationDatabase,
    ): ConversationLocalDataSource = ConversationLocalDataSourceImpl(db)
}
