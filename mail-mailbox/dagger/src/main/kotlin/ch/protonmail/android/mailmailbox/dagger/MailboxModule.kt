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

package ch.protonmail.android.mailmailbox.dagger

import ch.protonmail.android.mailmailbox.data.repository.InMemoryMailboxRepositoryImpl
import ch.protonmail.android.mailmailbox.data.repository.UnreadCountersRepositoryImpl
import ch.protonmail.android.mailmailbox.domain.annotations.IsSkipInitialMediatorRefreshEnabled
import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountersRepository
import ch.protonmail.android.mailmailbox.domain.usecase.featureflags.SkipInitialMediatorRefresh
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module(includes = [MailboxModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object MailboxModule {

    @Provides
    @IsSkipInitialMediatorRefreshEnabled
    fun provideSkipInitialMediatorRefresh(isEnabled: SkipInitialMediatorRefresh) = isEnabled()

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindsModule {

        @Binds
        @Reusable
        fun bindsUnreadCountRepository(impl: UnreadCountersRepositoryImpl): UnreadCountersRepository

        @Binds
        @Singleton
        fun bindsInMemoryMailboxRepository(impl: InMemoryMailboxRepositoryImpl): InMemoryMailboxRepository
    }
}
