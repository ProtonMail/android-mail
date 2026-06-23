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

package ch.protonmail.android.mailsession.data.initializer

import java.io.File
import android.content.Context
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import ch.protonmail.android.mailbugreport.domain.annotations.RustLogsFileHandler
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.stream.RustSessionStreamCoordinator
import ch.protonmail.android.mailsession.domain.annotations.DatabasesBaseDirectory
import ch.protonmail.android.mailsession.domain.model.RustApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import uniffi.mail_issue_reporter_service_uniffi.IssueReporter
import uniffi.mail_uniffi.ApiConfig
import uniffi.mail_uniffi.AppDetails
import uniffi.mail_uniffi.CreateMailSessionResult
import uniffi.mail_uniffi.MailSessionParams
import uniffi.mail_uniffi.Origin
import uniffi.mail_uniffi.OsKeyChain
import uniffi.mail_uniffi.createMailSession
import javax.inject.Inject

class InitRustCommonLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mailSessionRepository: MailSessionRepository,
    private val initializeRustTlsModule: InitializeRustTlsModule,
    @DatabasesBaseDirectory private val databasesBaseDirectory: File,
    @RustLogsFileHandler private val rustLogsFileHandler: LogsFileHandler,
    private val rustApiConfig: RustApiConfig,
    private val keyChain: OsKeyChain,
    private val issueReporter: IssueReporter,
    private val streamCoordinator: RustSessionStreamCoordinator
) {

    fun init() {
        initializeRustTlsModule()

        val databasePath = databasesBaseDirectory.absolutePath
        val sessionParams = MailSessionParams(
            sessionDir = databasePath,
            userDir = databasePath,
            mailCacheDir = context.cacheDir.absolutePath,
            mailCacheSize = CACHE_SIZE,
            logDir = rustLogsFileHandler.getParentPath().absolutePath,
            logDebug = false,
            apiEnvConfig = ApiConfig(
                userAgent = rustApiConfig.userAgent,
                envId = rustApiConfig.envId,
                proxy = rustApiConfig.proxy,
                useCustomResolver = true
            ),
            appDetails = AppDetails(
                platform = rustApiConfig.platform,
                product = rustApiConfig.product,
                version = rustApiConfig.appVersion
            ),
            origin = Origin.APP,
            quarantineXattrAppName = null,
            eventPollDurationSeconds = null,
            enableContentSearch = false
        )
        Timber.d("rust-session: Initializing the Rust Lib with $sessionParams")

        when (
            val result = createMailSession(
                params = sessionParams,
                keyChain = keyChain,
                issueReporter = issueReporter
            )
        ) {
            is CreateMailSessionResult.Error -> {
                Timber.e("rust-session: Critical error! Failed creating Mail session. Reason: ${result.v1}")
            }

            is CreateMailSessionResult.Ok -> {
                val bundle = result.v1
                mailSessionRepository.setMailSession(bundle.session)
                // The HV / device-info / resolver callbacks are no longer FFI traits: the foreign
                // side must now drive the request streams returned in the bundle for the session's
                // lifetime. Delegating that to a separate coordinator to keep the init clean.
                streamCoordinator.start(bundle)
            }
        }
    }

    companion object {

        private const val CACHE_SIZE = 500_000_000uL
    }
}
