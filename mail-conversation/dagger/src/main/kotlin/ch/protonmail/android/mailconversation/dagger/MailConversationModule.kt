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

package ch.protonmail.android.mailconversation.dagger

import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.benchmark.BenchmarkTracer
import ch.protonmail.android.mailconversation.data.local.ConversationDatabase
import ch.protonmail.android.mailconversation.data.local.ConversationLocalDataSourceImpl
import ch.protonmail.android.mailconversation.data.local.UnreadConversationsCountLocalDataSource
import ch.protonmail.android.mailconversation.data.local.UnreadConversationsCountLocalDataSourceImpl
import ch.protonmail.android.mailconversation.data.remote.ConversationRemoteDataSourceImpl
import ch.protonmail.android.mailconversation.data.remote.UnreadConversationsCountRemoteDataSource
import ch.protonmail.android.mailconversation.data.remote.UnreadConversationsCountRemoteDataSourceImpl
import ch.protonmail.android.mailconversation.data.repository.ConversationRepositoryImpl
import ch.protonmail.android.mailconversation.data.repository.UnreadConversationsCountRepositoryImpl
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.repository.UnreadConversationsCountRepository
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.usecase.ExcludeDraftMessagesAlreadyInOutbox
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.network.data.ApiProvider
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Singleton

@Module(includes = [MailConversationModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object MailConversationModule {

    @Provides
    @Singleton
    fun provideConversationRepositoryImpl(
        conversationLocalDataSource: ConversationLocalDataSource,
        conversationRemoteDataSource: ConversationRemoteDataSource,
        coroutineScopeProvider: CoroutineScopeProvider,
        messageLocalDataSource: MessageLocalDataSource,
        excludeDraftMessagesAlreadyInOutbox: ExcludeDraftMessagesAlreadyInOutbox
    ): ConversationRepository = ConversationRepositoryImpl(
        conversationRemoteDataSource = conversationRemoteDataSource,
        conversationLocalDataSource = conversationLocalDataSource,
        coroutineScopeProvider = coroutineScopeProvider,
        messageLocalDataSource = messageLocalDataSource,
        excludeDraftMessagesAlreadyInOutbox = excludeDraftMessagesAlreadyInOutbox
    )

    @Provides
    @Singleton
    fun provideConversationRemoteDataSource(
        apiProvider: ApiProvider,
        enqueuer: Enqueuer,
        benchmarkTracer: BenchmarkTracer
    ): ConversationRemoteDataSource = ConversationRemoteDataSourceImpl(
        apiProvider = apiProvider,
        enqueuer = enqueuer,
        benchmarkTracer = benchmarkTracer
    )

    @Provides
    @Singleton
    fun provideConversationLocalDataSource(db: ConversationDatabase): ConversationLocalDataSource =
        ConversationLocalDataSourceImpl(db)

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindsModule {

        @Binds
        @Reusable
        fun bindsUnreadConvoCountRepository(
            impl: UnreadConversationsCountRepositoryImpl
        ): UnreadConversationsCountRepository

        @Binds
        @Reusable
        fun bindsUnreadConvoCountRemoteDataSource(
            impl: UnreadConversationsCountRemoteDataSourceImpl
        ): UnreadConversationsCountRemoteDataSource

        @Binds
        @Reusable
        fun bindsUnreadConvoCountLocalDataSource(
            impl: UnreadConversationsCountLocalDataSourceImpl
        ): UnreadConversationsCountLocalDataSource
    }
}
