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

package ch.protonmail.android.maillabel.dagger

import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.maillabel.data.MailLabelRustCoroutineScope
import ch.protonmail.android.maillabel.data.local.LabelDataSource
import ch.protonmail.android.maillabel.data.local.RustGetLabelIdBySystemLabel
import ch.protonmail.android.maillabel.data.local.RustGetSystemLabelById
import ch.protonmail.android.maillabel.data.local.RustLabelDataSource
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.usecase.CreateMailbox
import ch.protonmail.android.maillabel.data.repository.InMemorySelectedMailLabelIdRepositoryImpl
import ch.protonmail.android.maillabel.data.repository.RustLabelRepository
import ch.protonmail.android.maillabel.data.repository.ViewModeRepositoryImpl
import ch.protonmail.android.maillabel.data.usecase.CreateRustSidebar
import ch.protonmail.android.maillabel.data.usecase.RustGetAllMailLabelId
import ch.protonmail.android.maillabel.domain.repository.LabelRepository
import ch.protonmail.android.maillabel.domain.repository.SelectedMailLabelIdRepository
import ch.protonmail.android.maillabel.domain.repository.ViewModeRepository
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailLabelModule {

    @Provides
    @Singleton
    @MailLabelRustCoroutineScope
    fun provideLabelRustCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Suppress("LongParameterList")
    @Provides
    @Singleton
    fun provideRustLabelDataSource(
        userSessionRepository: UserSessionRepository,
        createRustSidebar: CreateRustSidebar,
        createMailbox: CreateMailbox,
        rustGetAllMailLabelId: RustGetAllMailLabelId,
        rustGetSystemLabelById: RustGetSystemLabelById,
        rustGetLabelIdBySystemLabel: RustGetLabelIdBySystemLabel,
        @MailLabelRustCoroutineScope coroutineScope: CoroutineScope,
        @IODispatcher ioDispatcher: CoroutineDispatcher
    ): LabelDataSource = RustLabelDataSource(
        userSessionRepository,
        createRustSidebar,
        createMailbox,
        rustGetAllMailLabelId,
        rustGetSystemLabelById,
        rustGetLabelIdBySystemLabel,
        coroutineScope,
        ioDispatcher
    )

    @Provides
    @Singleton
    fun providesLabelRepository(rustLabelDataSource: LabelDataSource): LabelRepository =
        RustLabelRepository(rustLabelDataSource)

    @Provides
    @Singleton
    fun provideSelectedMailLabelIdRepository(
        @AppScope appScope: CoroutineScope,
        findLocalSystemLabelId: FindLocalSystemLabelId,
        observePrimaryUserId: ObservePrimaryUserId
    ): SelectedMailLabelIdRepository =
        InMemorySelectedMailLabelIdRepositoryImpl(appScope, findLocalSystemLabelId, observePrimaryUserId)

    @Provides
    @Singleton
    fun providesViewModelRepository(rustMailboxFactory: RustMailboxFactory): ViewModeRepository =
        ViewModeRepositoryImpl(rustMailboxFactory)

}
