/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.maildetail.presentation.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonErrorMessage
import ch.protonmail.android.design.compose.component.appbar.ProtonTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.EntireMessageBodyAction
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.viewmodel.EntireMessageBodyViewModel
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.BodyImageUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebView
import ch.protonmail.android.mailmessage.presentation.ui.ZoomableWebView
import ch.protonmail.android.mailmessage.presentation.ui.messageBodyImageSaver
import kotlinx.serialization.Serializable
import timber.log.Timber

@Composable
fun EntireMessageBodyScreen(
    onBackClick: () -> Unit,
    onOpenMessageBodyLink: (Uri) -> Unit,
    viewModel: EntireMessageBodyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val linkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }
    val phishingLinkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }

    val bodyImageSaver = messageBodyImageSaver(
        onFileSaved = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    )

    ConsumableLaunchedEffect(effect = state.openMessageBodyLinkEffect) {
        if (state.requestPhishingLinkConfirmation) {
            phishingLinkConfirmationDialogState.value = it
        } else if (state.requestLinkConfirmation) {
            linkConfirmationDialogState.value = it
        } else {
            onOpenMessageBodyLink(it)
        }
    }

    if (linkConfirmationDialogState.value != null) {
        ExternalLinkConfirmationDialog(
            onCancelClicked = {
                linkConfirmationDialogState.value = null
            },
            onContinueClicked = { doNotShowAgain ->
                linkConfirmationDialogState.value?.let { onOpenMessageBodyLink(it) }
                linkConfirmationDialogState.value = null
                if (doNotShowAgain) {
                    viewModel.submit(EntireMessageBodyAction.DoNotAskLinkConfirmationAgain)
                }
            },
            linkUri = linkConfirmationDialogState.value
        )
    }

    if (phishingLinkConfirmationDialogState.value != null) {
        PhishingLinkConfirmationDialog(
            onCancelClicked = { phishingLinkConfirmationDialogState.value = null },
            onContinueClicked = {
                phishingLinkConfirmationDialogState.value?.let { onOpenMessageBodyLink(it) }
            },
            linkUri = phishingLinkConfirmationDialogState.value
        )
    }

    Scaffold(
        topBar = {
            Column {
                ProtonTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                tint = ProtonTheme.colors.iconNorm,
                                painter = painterResource(id = R.drawable.ic_proton_arrow_back),
                                contentDescription = stringResource(id = R.string.presentation_back)
                            )
                        }
                    }
                )
                Text(
                    modifier = Modifier
                        .background(ProtonTheme.colors.backgroundNorm)
                        .fillMaxWidth()
                        .padding(horizontal = ProtonDimens.Spacing.Standard)
                        .padding(bottom = ProtonDimens.Spacing.Standard),
                    text = state.subject,
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.titleLargeMedium
                )
                MailDivider()
            }
        }
    ) { padding ->
        when (val messageBodyState = state.messageBodyState) {
            is MessageBodyState.Loading -> ProtonCenteredProgress()
            is MessageBodyState.Data -> EntireMessageBodyWebView(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                messageBodyUiModel = messageBodyState.messageBodyUiModel,
                actions = EntireMessageBodyWebView.Actions(
                    onMessageBodyLinkClicked = {
                        viewModel.submit(EntireMessageBodyAction.MessageBodyLinkClicked(it))
                    },
                    loadImage = { messageId, url ->
                        viewModel.loadImage(messageId, url)
                    },
                    onDownloadImage = { messageId, url ->
                        bodyImageSaver(BodyImageUiModel(url, messageId))
                    }
                )
            )
            is MessageBodyState.Error.Decryption -> EntireMessageBodyWebView(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                messageBodyUiModel = messageBodyState.encryptedMessageBody,
                actions = EntireMessageBodyWebView.Actions(
                    onMessageBodyLinkClicked = {
                        viewModel.submit(EntireMessageBodyAction.MessageBodyLinkClicked(it))
                    },
                    loadImage = { messageId, url ->
                        viewModel.loadImage(messageId, url)
                    },
                    onDownloadImage = { messageId, url ->
                        bodyImageSaver(BodyImageUiModel(url, messageId))
                    }
                )
            )
            is MessageBodyState.Error.Data -> ProtonErrorMessage(
                modifier = Modifier.padding(padding),
                errorMessage = stringResource(id = R.string.detail_error_loading_single_message)
            )
        }
    }
}

@Composable
private fun EntireMessageBodyWebView(
    modifier: Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    actions: EntireMessageBodyWebView.Actions
) {
    val webViewCache = remember { mutableStateOf<ZoomableWebView?>(null) }
    MessageBodyWebView(
        modifier = modifier,
        messageBodyUiModel = messageBodyUiModel,
        webViewActions = MessageBodyWebView.Actions(
            onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
            onMessageBodyLinkLongClicked = {}, // Deferred init to MessageBodyWebView
            onMessageBodyImageLongClicked = {}, // Deferred init to MessageBodyWebView
            onExpandCollapseButtonCLicked = {}, // Button not shown, message is shown fully
            loadImage = actions.loadImage,
            onPrint = {}, // Print action is not available in this screen
            onDownloadImage = actions.onDownloadImage,
            onViewEntireMessageClicked = { _, _, _, _ -> } // Button won't be shown
        ),
        shouldAllowViewingEntireMessage = false,
        // The entry point for this screen is the "View entire message" button, which only shows
        // when the inline cap is active (FF on + prior crash), so the screen itself is only
        // reached in that state.
        fillContainerHeight = true,
        onBuildWebView = onBuildWebView(webViewCache)
    )
}

@Composable
private fun onBuildWebView(webView: MutableState<ZoomableWebView?>) = { context: Context ->
    if (webView.value == null) {
        Timber.d("message-webview: factory creating new webview")
        webView.value = ZoomableWebView(context)
    }

    Timber.d("message-webview: factory returning webview")
    webView.value ?: throw IllegalStateException("WebView wasn't initialized.")
}

object EntireMessageBodyScreen {

    const val MESSAGE_ID_KEY = "message id"
    const val INPUT_PARAMS_KEY = "input params"

    @Serializable
    data class InputParams(
        val shouldShowEmbeddedImages: Boolean,
        val shouldShowRemoteContent: Boolean,
        val viewModePreference: ViewModePreference
    )
}

object EntireMessageBodyWebView {

    data class Actions(
        val onMessageBodyLinkClicked: (Uri) -> Unit,
        val loadImage: (MessageId, String) -> MessageBodyImage?,
        val onDownloadImage: (MessageId, String) -> Unit
    )
}
