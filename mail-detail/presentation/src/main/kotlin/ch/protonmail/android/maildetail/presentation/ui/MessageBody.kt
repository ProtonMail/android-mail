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
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyUiModel
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
internal fun MessageBody(
    modifier: Modifier = Modifier,
    messageBodyUiModel: MessageBodyUiModel,
    onMessageBodyLinkClicked: (uri: Uri) -> Unit
) {
    val state = rememberWebViewStateWithHTMLData(
        data = messageBodyUiModel.messageBody,
        mimeType = messageBodyUiModel.mimeType.value
    )

    val client = remember {
        object : AccompanistWebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.let { onMessageBodyLinkClicked(it.url) }
                return true
            }
        }
    }

    WebView(
        onCreated = {
            it.settings.builtInZoomControls = true
            it.settings.displayZoomControls = false
            it.settings.javaScriptEnabled = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.settings.isAlgorithmicDarkeningAllowed = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.settings.safeBrowsingEnabled = true
            }
        },
        state = state,
        modifier = modifier.fillMaxWidth(),
        client = client
    )
    if (messageBodyUiModel.attachments.isNotEmpty()) {
        AttachmentFooter(
            modifier = Modifier.background(color = ProtonTheme.colors.backgroundNorm),
            attachments = messageBodyUiModel.attachments
        )
    }
}

@Composable
internal fun MessageBodyLoadingError(
    modifier: Modifier = Modifier,
    messageBodyState: MessageBodyState.Error.Data,
    onReload: () -> Unit
) {
    val isNetworkError = messageBodyState.isNetworkError
    val errorMessage = stringResource(
        if (isNetworkError) {
            R.string.error_offline_loading_message
        } else {
            R.string.error_loading_message
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundNorm)
            .padding(horizontal = ProtonDimens.MediumSpacing, vertical = MailDimens.ExtraLargeSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .size(MailDimens.ErrorIconBoxSize)
                .background(ProtonTheme.colors.backgroundSecondary, ProtonTheme.shapes.large)
                .padding(ProtonDimens.MediumSpacing),
            painter = painterResource(id = R.drawable.ic_proton_exclamation_circle),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconHint
        )
        Text(
            modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
            text = errorMessage,
            textAlign = TextAlign.Center,
            style = ProtonTheme.typography.defaultSmallWeak
        )
        if (!isNetworkError) {
            ProtonSolidButton(
                modifier = Modifier.padding(top = ProtonDimens.DefaultSpacing),
                onClick = { onReload() }
            ) {
                Text(text = stringResource(id = R.string.reload))
            }
        }
    }
}
