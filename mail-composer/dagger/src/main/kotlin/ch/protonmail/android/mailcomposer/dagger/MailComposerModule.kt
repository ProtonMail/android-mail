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

import ch.protonmail.android.composer.data.annotations.ApiSendingErrorsEnabled
import ch.protonmail.android.composer.data.local.AttachmentStateLocalDataSource
import ch.protonmail.android.composer.data.local.AttachmentStateLocalDataSourceImpl
import ch.protonmail.android.composer.data.local.ContactsPermissionLocalDataSource
import ch.protonmail.android.composer.data.local.ContactsPermissionLocalDataSourceImpl
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSource
import ch.protonmail.android.composer.data.local.DraftStateLocalDataSourceImpl
import ch.protonmail.android.composer.data.local.MessageExpirationTimeLocalDataSource
import ch.protonmail.android.composer.data.local.MessageExpirationTimeLocalDataSourceImpl
import ch.protonmail.android.composer.data.local.MessagePasswordLocalDataSource
import ch.protonmail.android.composer.data.local.MessagePasswordLocalDataSourceImpl
import ch.protonmail.android.composer.data.local.RoomTransactor
import ch.protonmail.android.composer.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.composer.data.remote.AttachmentRemoteDataSourceImpl
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSource
import ch.protonmail.android.composer.data.remote.DraftRemoteDataSourceImpl
import ch.protonmail.android.composer.data.remote.MessageRemoteDataSource
import ch.protonmail.android.composer.data.remote.MessageRemoteDataSourceImpl
import ch.protonmail.android.composer.data.repository.AttachmentRepositoryImpl
import ch.protonmail.android.composer.data.repository.AttachmentStateRepositoryImpl
import ch.protonmail.android.composer.data.repository.ContactsPermissionRepositoryImpl
import ch.protonmail.android.composer.data.repository.DraftRepositoryImpl
import ch.protonmail.android.composer.data.repository.DraftStateRepositoryImpl
import ch.protonmail.android.composer.data.repository.MessageExpirationTimeRepositoryImpl
import ch.protonmail.android.composer.data.repository.MessagePasswordRepositoryImpl
import ch.protonmail.android.composer.data.repository.MessageRepositoryImpl
import ch.protonmail.android.composer.data.usecase.featureflags.IsApiSendingErrorsEnabled
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.annotation.IsComposerV2Enabled
import ch.protonmail.android.mailcomposer.domain.annotation.IsExternalAddressSendingEnabled
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailcomposer.domain.repository.ContactsPermissionRepository
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessageExpirationTimeRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessagePasswordRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailcomposer.domain.usecase.featureflags.IsComposerV2FeatureEnabled
import ch.protonmail.android.mailcomposer.domain.usecase.featureflags.IsExternalAddressEnabled
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MailComposerModule {

    @Binds
    @Reusable
    abstract fun bindsDraftRepository(impl: DraftRepositoryImpl): DraftRepository

    @Binds
    @Reusable
    abstract fun bindsDraftStateRepository(impl: DraftStateRepositoryImpl): DraftStateRepository

    @Binds
    @Reusable
    abstract fun provideDraftStateLocalDataSource(impl: DraftStateLocalDataSourceImpl): DraftStateLocalDataSource

    @Binds
    @Reusable
    abstract fun provideMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Reusable
    abstract fun bindsRoomTransactor(impl: RoomTransactor): Transactor

    @Binds
    @Reusable
    abstract fun bindsDraftStateRemoteDataSource(impl: DraftRemoteDataSourceImpl): DraftRemoteDataSource

    @Binds
    @Reusable
    abstract fun bindsMessageRemoteDataSource(impl: MessageRemoteDataSourceImpl): MessageRemoteDataSource

    @Binds
    @Reusable
    abstract fun bindsAttachmentStateLocalDataSource(
        impl: AttachmentStateLocalDataSourceImpl
    ): AttachmentStateLocalDataSource

    @Binds
    @Reusable
    abstract fun bindsAttachmentStateRepository(impl: AttachmentStateRepositoryImpl): AttachmentStateRepository

    @Binds
    @Reusable
    abstract fun bindsAttachmentRemoteDataSource(impl: AttachmentRemoteDataSourceImpl): AttachmentRemoteDataSource

    @Binds
    @Reusable
    abstract fun bindsAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository

    @Binds
    @Reusable
    abstract fun bindsMessagePasswordLocalDataSource(
        impl: MessagePasswordLocalDataSourceImpl
    ): MessagePasswordLocalDataSource

    @Binds
    @Reusable
    abstract fun bindsMessagePasswordRepository(impl: MessagePasswordRepositoryImpl): MessagePasswordRepository

    @Binds
    @Reusable
    @Suppress("FunctionMaxLength")
    abstract fun bindsMessageExpirationTimeLocalDataSource(
        impl: MessageExpirationTimeLocalDataSourceImpl
    ): MessageExpirationTimeLocalDataSource

    @Binds
    @Reusable
    abstract fun bindsMessageExpirationTimeRepository(
        impl: MessageExpirationTimeRepositoryImpl
    ): MessageExpirationTimeRepository

    @Module
    @InstallIn(SingletonComponent::class)
    object FeatureFlagModule {

        @Provides
        @Singleton
        @IsComposerV2Enabled
        fun provideComposerV2FeatureFlag(isEnabled: IsComposerV2FeatureEnabled) = isEnabled()

        @Provides
        @Singleton
        @IsExternalAddressSendingEnabled
        fun provideExternalAddressFeatureFlag(isEnabled: IsExternalAddressEnabled) = isEnabled()

        @Provides
        @Singleton
        @ApiSendingErrorsEnabled
        fun provideIsApiSendingErrorsEnabled(isEnabled: IsApiSendingErrorsEnabled) = isEnabled()
    }

    @Binds
    @Singleton
    abstract fun bindContactsPermissionLocalData(
        dataSource: ContactsPermissionLocalDataSourceImpl
    ): ContactsPermissionLocalDataSource

    @Binds
    @Singleton
    abstract fun bindContactsPermissionRepository(repo: ContactsPermissionRepositoryImpl): ContactsPermissionRepository
}
