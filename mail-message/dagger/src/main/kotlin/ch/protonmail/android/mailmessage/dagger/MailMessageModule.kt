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

package ch.protonmail.android.mailmessage.dagger

import ch.protonmail.android.mailmessage.data.local.MessageDatabase
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSourceImpl
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSourceImpl
import ch.protonmail.android.mailmessage.data.repository.MessageRepositoryImpl
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.network.data.ApiProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailMessageModule {

    @Provides
    @Singleton
    fun provideMessageRepositoryImpl(
        remoteDataSource: MessageRemoteDataSource,
        localDataSource: MessageLocalDataSource
    ): MessageRepository = MessageRepositoryImpl(remoteDataSource, localDataSource)

    @Provides
    @Singleton
    fun provideMessageRemoteDataSource(
        apiProvider: ApiProvider
    ): MessageRemoteDataSource = MessageRemoteDataSourceImpl(apiProvider)

    @Provides
    @Singleton
    fun provideMessageLocalDataSource(
        db: MessageDatabase
    ): MessageLocalDataSource = MessageLocalDataSourceImpl(db)
}
