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

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.auth.AuthTabIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonErrorMessageWithAction
import ch.protonmail.android.design.compose.component.ProtonSettingsDetailsAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbyoe.domain.model.GmailConnectionResult
import ch.protonmail.android.mailbyoe.presentation.R
import ch.protonmail.android.mailbyoe.presentation.connect.GmailConnectionOperation.GmailConnectionAction
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailcommon.presentation.Effect
import timber.log.Timber

/**
 * Opens the Gmail connection web page in an Auth Tab, which hands the `protonmail://` result back to this
 * screen without needing an intent filter. Browsers without Auth Tab support open a Custom Tab instead, whose
 * redirect [GmailConnectionRedirectActivity] catches.
 *
 * Once connected, it shows the success page; [onOpenInbox] runs after the Inbox is selected, with whether the user
 * chose to import their emails. When the user is at the connection limit and asks to upgrade, [onOpenUpsell] runs
 * instead with the upsell to offer, once it has loaded.
 */
@Composable
fun GmailConnectionScreen(
    onClose: () -> Unit,
    onOpenInbox: (importEmails: Boolean) -> Unit,
    onOpenUpsell: (UpsellingVisibility) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GmailConnectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.submit(GmailConnectionAction.PageClosed(result.withCustomTabRedirect().toGmailConnectionResult()))
    }

    when (val current = state) {
        is GmailConnectionState.OpeningPage -> ConsumableLaunchedEffect(current.openPage) { url ->
            GmailConnectionRedirects.consume()
            launchConnectionPage(launcher, url) {
                viewModel.submit(GmailConnectionAction.PageClosed(GmailConnectionResult.Failed(code = null)))
            }
        }

        is GmailConnectionState.Finished -> ConsumableLaunchedEffect(current.close) { onClose() }
        is GmailConnectionState.Connected -> ConsumableLaunchedEffect(current.openInbox) { onOpenInbox(current.importEmails) }
        is GmailConnectionState.UpsellRequested -> ConsumableLaunchedEffect(current.openUpsell) { onOpenUpsell(it) }
        else -> Unit
    }

    when (val current = state) {
        // The success page uses the normal theme, like the intro, not the inverted one around this screen.
        is GmailConnectionState.Connected -> ProtonTheme {
            GmailConnectedContent(
                email = current.email,
                importEmails = current.importEmails,
                onOpenInbox = { viewModel.submit(GmailConnectionAction.OpenInboxClicked) },
                modifier = modifier
            )
        }

        else -> GmailConnectionContent(
            state = current,
            onBackClick = onClose,
            onRetryClick = { viewModel.submit(GmailConnectionAction.RetryClicked) },
            modifier = modifier
        )
    }
}

object GmailConnectionScreen {

    const val IMPORT_EMAILS_KEY = "gmailImportEmails"
}

@Composable
private fun GmailConnectionContent(
    state: GmailConnectionState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsDetailsAppBar(
                title = stringResource(R.string.byoe_gmail_connection_title),
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (state is GmailConnectionState.Failed) {
                ProtonErrorMessageWithAction(
                    modifier = Modifier.padding(ProtonDimens.Spacing.Large),
                    errorMessage = stringResource(R.string.byoe_gmail_connection_error),
                    action = stringResource(R.string.byoe_gmail_connection_retry),
                    onAction = onRetryClick
                )
            } else {
                ProtonCenteredProgress()
            }
        }
    }
}

private fun launchConnectionPage(
    launcher: ActivityResultLauncher<Intent>,
    url: String,
    onUnavailable: () -> Unit
) {
    try {
        AuthTabIntent.Builder().build().launch(launcher, url.toUri(), GmailConnectionCallback.SCHEME)
    } catch (e: ActivityNotFoundException) {
        Timber.e(e, "byoe: no browser can open the Gmail connection page")
        onUnavailable()
    }
}

/**
 * A Custom Tab always returns as cancelled. When [GmailConnectionRedirectActivity] caught the page's redirect
 * meanwhile, read the result from it as an Auth Tab would have returned it.
 */
private fun ActivityResult.withCustomTabRedirect(): ActivityResult {
    if (resultCode != AuthTabIntent.RESULT_CANCELED) return this
    val redirect = GmailConnectionRedirects.consume() ?: return this
    Timber.d("byoe: using the Custom Tab redirect")
    return ActivityResult(AuthTabIntent.RESULT_OK, Intent(Intent.ACTION_VIEW, redirect))
}

private fun ActivityResult.toGmailConnectionResult(): GmailConnectionResult = when (resultCode) {
    AuthTabIntent.RESULT_OK -> runCatching {
        val uri = data?.data
        GmailConnectionCallback.parse(
            host = uri?.host,
            path = uri?.path,
            status = uri?.getQueryParameter(GmailConnectionCallback.STATUS_PARAM),
            code = uri?.getQueryParameter(GmailConnectionCallback.CODE_PARAM),
            email = uri?.getQueryParameter(GmailConnectionCallback.EMAIL_PARAM)
        )
    }.getOrElse { GmailConnectionResult.Failed(code = null) }

    AuthTabIntent.RESULT_CANCELED -> GmailConnectionResult.Cancelled
    else -> GmailConnectionResult.Failed(code = null)
}

@Preview
@Composable
private fun GmailConnectionLoadingPreview() {
    ProtonTheme {
        GmailConnectionContent(
            state = GmailConnectionState.OpeningPage(Effect.empty()),
            onBackClick = {},
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
private fun GmailConnectionFailedPreview() {
    ProtonTheme {
        GmailConnectionContent(
            state = GmailConnectionState.Failed,
            onBackClick = {},
            onRetryClick = {}
        )
    }
}
