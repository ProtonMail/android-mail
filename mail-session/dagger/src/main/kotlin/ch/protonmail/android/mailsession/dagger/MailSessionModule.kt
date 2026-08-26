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

package ch.protonmail.android.mailsession.dagger

import android.content.Context
import ch.protonmail.android.mailsession.data.database.getDatabaseBaseDirectory
import ch.protonmail.android.mailsession.data.background.BackgroundExecutionWorkScheduler
import ch.protonmail.android.mailsession.data.background.PendingSendTrackerImpl
import ch.protonmail.android.mailsession.data.deviceinfo.AndroidDeviceInfoProvider
import ch.protonmail.android.mailsession.data.initializer.DatabaseLifecycleObserver
import ch.protonmail.android.mailsession.data.initializer.DatabaseLifecycleObserverImpl
import ch.protonmail.android.mailsession.data.keychain.AndroidKeyChain
import ch.protonmail.android.mailsession.data.keychain.KeyChainLocalDataSource
import ch.protonmail.android.mailsession.data.keychain.KeyChainLocalDataSourceImpl
import ch.protonmail.android.mailsession.data.logging.SentryIssueReporter
import ch.protonmail.android.mailsession.data.repository.InMemoryMailSessionRepository
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.RustEventLoopRepository
import ch.protonmail.android.mailsession.data.repository.UserSessionRepositoryImpl
import ch.protonmail.android.mailsession.data.user.RustUserDataSource
import ch.protonmail.android.mailsession.data.user.RustUserDataSourceImpl
import ch.protonmail.android.mailsession.data.wrapper.SyncServiceWrapper
import ch.protonmail.android.mailsession.domain.annotations.DatabasesBaseDirectory
import ch.protonmail.android.mailsession.domain.background.PendingSendTracker
import ch.protonmail.android.mailsession.domain.background.SendCompletionScheduler
import ch.protonmail.android.mailsession.domain.coroutines.EventLoopScope
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uniffi.mail_issue_reporter_service_uniffi.IssueReporter
import uniffi.mail_uniffi.DeviceInfoProvider
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.OsKeyChain
import uniffi.mail_uniffi.SyncService
import javax.inject.Singleton

@Module(includes = [MailSessionModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object MailSessionModule {

    @Provides
    @Singleton
    fun provideMailSessionInterface(repository: MailSessionRepository): MailSession =
        repository.getMailSession().getRustMailSession()

    /**
     * Binds the [SyncService] for the lifetime of the process, which holds because the Rust session
     * is created exactly once: [MailSessionRepository.setMailSession] has a single production caller
     * in `InitRustCommonLibrary.init()`, itself invoked once from the `RustMailCommonInitializer` App
     * Startup entry. [provideMailSessionInterface] above already relies on the same invariant.
     */
    @Provides
    @Singleton
    fun provideSyncServiceWrapper(
        mailSession: MailSession,
        mailSessionRepository: MailSessionRepository
    ): SyncServiceWrapper = SyncServiceWrapper(SyncService(mailSession), mailSessionRepository)

    @Provides
    @Singleton
    @EventLoopScope
    fun provideEventLoopScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Provides
    @Singleton
    @DatabasesBaseDirectory
    fun provideBaseDbDirectory(@ApplicationContext context: Context) = getDatabaseBaseDirectory(context)

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {

        @Binds
        @Singleton
        fun bindOsKeyChain(impl: AndroidKeyChain): OsKeyChain

        @Binds
        @Singleton
        fun bindDeviceInfoProvider(impl: AndroidDeviceInfoProvider): DeviceInfoProvider

        @Binds
        @Singleton
        fun bindMailSessionRepository(impl: InMemoryMailSessionRepository): MailSessionRepository

        @Binds
        @Singleton
        fun bindEventLoopRepository(impl: RustEventLoopRepository): EventLoopRepository

        @Binds
        @Singleton
        fun bindUserSessionRepository(impl: UserSessionRepositoryImpl): UserSessionRepository

        @Binds
        @Singleton
        fun bindUserDataSource(impl: RustUserDataSourceImpl): RustUserDataSource

        @Binds
        @Singleton
        fun bindDatabaseObserver(impl: DatabaseLifecycleObserverImpl): DatabaseLifecycleObserver

        @Binds
        @Singleton
        fun bindKeyChainLocalDataSource(impl: KeyChainLocalDataSourceImpl): KeyChainLocalDataSource

        @Binds
        @Singleton
        fun bindIssueReporter(impl: SentryIssueReporter): IssueReporter

        @Binds
        fun bindSendCompletionScheduler(impl: BackgroundExecutionWorkScheduler): SendCompletionScheduler

        @Binds
        @Singleton
        fun bindPendingSendTracker(impl: PendingSendTrackerImpl): PendingSendTracker
    }
}
