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

package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.EntireMessageBodyAction
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.viewmodel.EntireMessageBodyViewModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.ui.MessageBodyWebView
import kotlinx.serialization.Serializable
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultStrongNorm

@Composable
fun EntireMessageBodyScreen(
    onBackClick: () -> Unit,
    onOpenMessageBodyLink: (Uri) -> Unit,
    viewModel: EntireMessageBodyViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val linkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }
    val phishingLinkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }

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
                                Icons.AutoMirrored.Filled.ArrowBack,
                                tint = ProtonTheme.colors.iconNorm,
                                contentDescription = stringResource(id = R.string.presentation_back)
                            )
                        }
                    }
                )
                Text(
                    modifier = Modifier
                        .background(ProtonTheme.colors.backgroundNorm)
                        .fillMaxWidth()
                        .padding(horizontal = ProtonDimens.DefaultSpacing)
                        .padding(bottom = ProtonDimens.DefaultSpacing),
                    text = state.subject,
                    textAlign = TextAlign.Center,
                    style = ProtonTheme.typography.defaultStrongNorm
                )
                MailDivider()
            }
        }
    ) { padding ->
        when (val messageBodyState = state.messageBodyState) {
            is MessageBodyState.Loading -> ProtonCenteredProgress()
            is MessageBodyState.Data -> MessageBodyWebView(
                modifier = Modifier.padding(padding),
                messageBodyUiModel = messageBodyState.messageBodyUiModel,
                onMessageBodyLinkClicked = { viewModel.submit(EntireMessageBodyAction.MessageBodyLinkClicked(it)) },
                loadEmbeddedImage = { messageId, contentId -> viewModel.loadEmbeddedImage(messageId, contentId) }
            )
            is MessageBodyState.Error.Decryption -> MessageBodyWebView(
                modifier = Modifier.padding(padding),
                messageBodyUiModel = messageBodyState.encryptedMessageBody,
                onMessageBodyLinkClicked = { viewModel.submit(EntireMessageBodyAction.MessageBodyLinkClicked(it)) },
                loadEmbeddedImage = { messageId, contentId -> viewModel.loadEmbeddedImage(messageId, contentId) }
            )
            is MessageBodyState.Error.Data -> MessageBodyLoadingError(
                modifier = Modifier.padding(padding),
                messageBodyState = messageBodyState,
                onReload = { viewModel.submit(EntireMessageBodyAction.ReloadMessageBody) }
            )
        }
    }
}

@Composable
private fun MessageBodyWebView(
    modifier: Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    onMessageBodyLinkClicked: (Uri) -> Unit,
    loadEmbeddedImage: (MessageId, String) -> GetEmbeddedImageResult?
) {
    MessageBodyWebView(
        modifier = modifier,
        messageBodyUiModel = messageBodyUiModel,
        bodyDisplayMode = MessageBodyExpandCollapseMode.NotApplicable,
        webViewActions = MessageBodyWebView.Actions(
            onMessageBodyLinkClicked = onMessageBodyLinkClicked,
            onMessageBodyLinkLongClicked = {}, // Deferred init to MessageBodyWebView.
            onExpandCollapseButtonCLicked = {}, // Button won't be shown
            loadEmbeddedImage = loadEmbeddedImage,
            onPrint = {}, // Print action is not available in this screen
            onReply = {},
            onReplyAll = {},
            onForward = {},
            onViewEntireMessageClicked = { _, _, _, _ -> } // Button won't be shown
        )
    )
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
