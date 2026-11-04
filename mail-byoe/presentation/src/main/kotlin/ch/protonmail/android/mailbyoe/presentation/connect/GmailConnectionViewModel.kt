/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.presentation.connect

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult
import ch.protonmail.android.mailbyoe.domain.usecase.RefreshAfterGmailConnection
import ch.protonmail.android.mailbyoe.presentation.BuildConfig
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionAction
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionEvent
import ch.protonmail.android.mailcommon.domain.model.ApiEnvironment
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.SelectMailLabelId
import ch.protonmail.android.mailsession.domain.usecase.ForkSessionWithKeyPayload
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GmailConnectionViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val forkSessionWithKeyPayload: ForkSessionWithKeyPayload,
    private val refreshAfterGmailConnection: RefreshAfterGmailConnection,
    private val findLocalSystemLabelId: FindLocalSystemLabelId,
    private val selectMailLabelId: SelectMailLabelId,
    private val apiEnvironment: ApiEnvironment,
    private val reducer: GmailConnectionReducer,
    private val observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val importEmails =
        savedStateHandle.get<String>(GmailConnectionScreen.IMPORT_EMAILS_KEY)?.toBooleanStrictOrNull() ?: true

    private val mutableState = MutableStateFlow<GmailConnectionState>(GmailConnectionState.Loading)
    val state: StateFlow<GmailConnectionState> = mutableState.asStateFlow()

    init {
        when (savedStateHandle.get<String>(PAGE_PROGRESS_KEY)) {
            // Recreated after process death while the page was open: its result still arrives through the launcher.
            PageProgress.Opened.name -> Unit
            // Recreated after the page had already returned: there is nothing left to wait for.
            PageProgress.Closed.name -> emitNewStateFrom(GmailConnectionEvent.ConnectionFinished)
            // Recreated while the error was showing: show it again, so the user can still retry.
            PageProgress.Failed.name -> emitNewStateFrom(GmailConnectionEvent.ConnectionFailed)
            else -> viewModelScope.launch { openConnectionPage() }
        }
    }

    fun submit(action: GmailConnectionAction) {
        emitNewStateFrom(action)
        when (action) {
            is GmailConnectionAction.RetryClicked -> viewModelScope.launch { openConnectionPage() }
            is GmailConnectionAction.PageClosed -> viewModelScope.launch { onPageClosed(action.result) }
            is GmailConnectionAction.OpenInboxClicked -> viewModelScope.launch { selectInbox() }
        }
    }

    private suspend fun openConnectionPage() {
        val userId = observePrimaryUserId().first()
        if (userId == null) {
            Timber.e("byoe: no primary user to connect a Gmail account for")
            emitNewStateFrom(GmailConnectionEvent.PreparationFailed)
            return
        }

        forkSessionWithKeyPayload(userId).fold(
            ifLeft = {
                Timber.e("byoe: forking the session for the Gmail connection failed: $it")
                emitNewStateFrom(GmailConnectionEvent.PreparationFailed)
            },
            ifRight = { fork ->
                val url = gmailConnectionUrl(apiEnvironment.host, BuildConfig.WEBVIEW_APP_VERSION, fork, importEmails)
                Timber.d("byoe: opening the Gmail connection page on account.${apiEnvironment.host}")
                savedStateHandle[PAGE_PROGRESS_KEY] = PageProgress.Opened.name
                emitNewStateFrom(GmailConnectionEvent.PageReady(url))
            }
        )
    }

    private suspend fun onPageClosed(result: GmailConnectionResult) {
        Timber.d("byoe: Gmail connection page closed, result = ${result::class.simpleName}")
        if (result is GmailConnectionResult.Failed) {
            savedStateHandle[PAGE_PROGRESS_KEY] = PageProgress.Failed.name
            Timber.w("byoe: the Gmail connection failed, code = ${result.code}")
            emitNewStateFrom(GmailConnectionEvent.ConnectionFailed)
            return
        }

        savedStateHandle[PAGE_PROGRESS_KEY] = PageProgress.Closed.name
        if (result is GmailConnectionResult.UpsellRequested) {
            openUpsell()
            return
        }

        if (result is GmailConnectionResult.Connected) {
            emitNewStateFrom(GmailConnectionEvent.ConnectionSucceeded(result.email, importEmails))
            observePrimaryUserId().first()?.let { refreshAfterGmailConnection(it) }
            return
        }

        // Also refresh when cancelled: the connection may have completed before the page was closed.
        observePrimaryUserId().first()?.let { refreshAfterGmailConnection(it) }
        emitNewStateFrom(GmailConnectionEvent.ConnectionFinished)
    }

    /**
     * Waits here, not in the screen, so the wait survives the activity being recreated. The visibility reads
     * [UpsellingVisibility.Hidden] while the plans are still loading, which cannot be told apart from "no upsell", so
     * this waits for a visible upsell and treats the timeout as "no upsell", closing the flow.
     */
    private suspend fun openUpsell() {
        val visibility = withTimeoutOrNull(UPSELL_LOAD_TIMEOUT) {
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.ByoeConnectionLimit)
                .first { it != UpsellingVisibility.Hidden }
        }
        if (visibility == null) {
            Timber.w("byoe: no upsell to offer at the Gmail connection limit")
            emitNewStateFrom(GmailConnectionEvent.ConnectionFinished)
        } else {
            emitNewStateFrom(GmailConnectionEvent.UpsellReady(visibility))
        }
    }

    private suspend fun selectInbox() {
        val inbox = observePrimaryUserId().first()?.let { findLocalSystemLabelId(it, SystemLabelId.Inbox) }
        if (inbox != null) selectMailLabelId(inbox) else Timber.w("byoe: no inbox to open after the Gmail connection")
        emitNewStateFrom(GmailConnectionEvent.InboxSelected)
    }

    private fun emitNewStateFrom(operation: GmailConnectionOperation) {
        mutableState.update { reducer.newStateFrom(it, operation) }
    }

    private enum class PageProgress { Opened, Closed, Failed }

    private companion object {

        const val PAGE_PROGRESS_KEY = "gmail_connection_page_progress"
        val UPSELL_LOAD_TIMEOUT = 5.seconds
    }
}
