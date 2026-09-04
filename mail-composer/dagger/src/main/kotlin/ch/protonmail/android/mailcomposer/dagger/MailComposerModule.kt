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

import ch.protonmail.android.composer.data.local.ContactsPermissionLocalDataSource
import ch.protonmail.android.composer.data.local.ContactsPermissionLocalDataSourceImpl
import ch.protonmail.android.composer.data.local.RustAttachmentDataSource
import ch.protonmail.android.composer.data.local.RustAttachmentDataSourceImpl
import ch.protonmail.android.composer.data.local.RustDraftDataSource
import ch.protonmail.android.composer.data.local.RustDraftDataSourceImpl
import ch.protonmail.android.composer.data.local.RustSendingStatusDataSource
import ch.protonmail.android.composer.data.local.RustSendingStatusDataSourceImpl
import ch.protonmail.android.composer.data.repository.ActiveComposerInMemoryRepository
import ch.protonmail.android.composer.data.repository.AttachmentRepositoryImpl
import ch.protonmail.android.composer.data.repository.CameraTempImageFileRepository
import ch.protonmail.android.composer.data.repository.PastedImageRepositoryImpl
import ch.protonmail.android.composer.data.repository.ContactsPermissionRepositoryImpl
import ch.protonmail.android.composer.data.repository.DraftRepositoryImpl
import ch.protonmail.android.composer.data.repository.MessageExpirationTimeRepositoryImpl
import ch.protonmail.android.composer.data.repository.MessagePasswordRepositoryImpl
import ch.protonmail.android.composer.data.repository.RustComposerCspRepository
import ch.protonmail.android.composer.data.repository.SendingStatusRepositoryImpl
import ch.protonmail.android.composer.data.usecase.RustSanitizePastedContent
import ch.protonmail.android.mailcomposer.domain.repository.ActiveComposerRepository
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailcomposer.domain.repository.CameraTempImageRepository
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import ch.protonmail.android.mailcomposer.domain.repository.ComposerCspRepository
import ch.protonmail.android.mailcomposer.domain.repository.ContactsPermissionRepository
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessageExpirationTimeRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailcomposer.domain.repository.SendingStatusRepository
import ch.protonmail.android.mailcomposer.domain.usecase.SanitizePastedContent
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
abstract class MailComposerModule {

    @Binds
    @ViewModelScoped
    abstract fun bindsSanitizePastedContent(impl: RustSanitizePastedContent): SanitizePastedContent

    @Binds
    @ViewModelScoped
    abstract fun bindsDraftRepository(impl: DraftRepositoryImpl): DraftRepository

    @Binds
    @ViewModelScoped
    abstract fun bindsAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository

    @Binds
    @ViewModelScoped
    abstract fun bindsMessagePasswordRepository(impl: MessagePasswordRepositoryImpl): MessagePasswordRepository

    @Binds
    @ViewModelScoped
    abstract fun bindsMessageExpirationTimeRepository(
        impl: MessageExpirationTimeRepositoryImpl
    ): MessageExpirationTimeRepository

    @Binds
    @Reusable
    abstract fun bindsCameraTempImageRepository(impl: CameraTempImageFileRepository): CameraTempImageRepository

    @Binds
    @Reusable
    abstract fun bindsPastedImageRepository(impl: PastedImageRepositoryImpl): PastedImageRepository

    @Binds
    @ViewModelScoped
    abstract fun bindsRustAttachmentDataSource(impl: RustAttachmentDataSourceImpl): RustAttachmentDataSource

    @Binds
    @ViewModelScoped
    abstract fun bindsRustDraftDataSource(impl: RustDraftDataSourceImpl): RustDraftDataSource

    @Binds
    @Reusable
    abstract fun bindsCspRepository(impl: RustComposerCspRepository): ComposerCspRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SendingStatusModule {

    @Binds
    @Singleton
    abstract fun bindsSendingStatusRepository(impl: SendingStatusRepositoryImpl): SendingStatusRepository

    @Binds
    @Singleton
    abstract fun bindsRustSendingStatusDataSource(impl: RustSendingStatusDataSourceImpl): RustSendingStatusDataSource

    @Binds
    @Singleton
    abstract fun bindContactsPermissionLocalData(
        dataSource: ContactsPermissionLocalDataSourceImpl
    ): ContactsPermissionLocalDataSource

    @Binds
    @Singleton
    abstract fun bindContactsPermissionRepository(repo: ContactsPermissionRepositoryImpl): ContactsPermissionRepository

    @Binds
    @Singleton
    abstract fun bindsActiveComposerRepository(impl: ActiveComposerInMemoryRepository): ActiveComposerRepository

}
