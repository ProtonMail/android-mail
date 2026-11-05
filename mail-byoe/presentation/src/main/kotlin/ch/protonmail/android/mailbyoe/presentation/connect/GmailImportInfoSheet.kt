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

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetDefaults
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbyoe.presentation.R
import kotlinx.coroutines.launch

/**
 * Explains what the Gmail import brings over, shown over [content] while [show] is true. [email] names the label
 * the imported emails go under, when the web page reported the address.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GmailImportInfoSheet(
    show: Boolean,
    email: String?,
    onOpenInbox: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hide: (then: () -> Unit) -> Unit = { then ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            then()
        }
    }

    ProtonModalBottomSheetLayout(
        showBottomSheet = show,
        sheetState = sheetState,
        onDismissed = onDismiss,
        dismissOnBack = true,
        dragHandle = null,
        sheetContent = {
            GmailImportInfoContent(email = email, onOpenInbox = { hide(onOpenInbox) })
        },
        content = content
    )
}

@Composable
private fun GmailImportInfoContent(email: String?, onOpenInbox: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge)
            .padding(bottom = SheetBottomPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProtonModalBottomSheetDefaults.DragHandle()
        Spacer(modifier = Modifier.size(HandleToTitleSpacing))
        GmailImportInfoHeader()
        Spacer(modifier = Modifier.size(ProtonDimens.Spacing.Huge))
        GmailImportInfoBullets(email)
        Spacer(modifier = Modifier.size(SectionSpacing))
        GmailConnectedOpenInboxButton(onClick = onOpenInbox)
    }
}

@Composable
private fun GmailImportInfoHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Compact)
    ) {
        Text(
            text = stringResource(R.string.byoe_import_info_title),
            style = ProtonTheme.typography.titleMedium.copy(fontSize = TitleFontSize, lineHeight = TitleLineHeight),
            fontWeight = FontWeight.Bold,
            color = ProtonTheme.colors.textNorm,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.byoe_import_info_subtitle),
            style = ProtonTheme.typography.bodyMedium.copy(fontSize = BodyFontSize),
            color = ProtonTheme.colors.textWeak,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GmailImportInfoBullets(email: String?) {
    val labelBody = email?.let { stringResource(R.string.byoe_import_info_label_body, it) }
        ?: stringResource(R.string.byoe_import_info_label_body_no_address)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Large)
    ) {
        GmailImportInfoBullet(lead = R.string.byoe_import_info_label_lead, body = labelBody)
        GmailImportInfoBullet(
            lead = R.string.byoe_import_info_folders_lead,
            body = stringResource(R.string.byoe_import_info_folders_body)
        )
        GmailImportInfoBullet(
            lead = R.string.byoe_import_info_filters_lead,
            body = stringResource(R.string.byoe_import_info_filters_body)
        )
    }
}

@Composable
private fun GmailImportInfoBullet(@StringRes lead: Int, body: String) {
    val style = ProtonTheme.typography.bodyMedium.copy(fontSize = BodyFontSize)
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = ProtonTheme.colors.textNorm, fontWeight = FontWeight.Medium)) {
            append(stringResource(lead))
        }
        append(" ")
        append(body)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
        Text(text = BULLET, style = style, color = ProtonTheme.colors.textNorm)
        Text(text = text, style = style, color = ProtonTheme.colors.textWeak)
    }
}

private const val BULLET = "•"

// With the handle's own padding, leaves the design's gap between the handle and the title.
private val HandleToTitleSpacing = 26.dp
private val SectionSpacing = 48.dp
private val SheetBottomPadding = 30.dp
private val TitleFontSize = 17.sp
private val TitleLineHeight = 22.sp
private val BodyFontSize = 15.sp

@Preview(widthDp = 412)
@Composable
private fun GmailImportInfoPreview() {
    ProtonTheme {
        Box(modifier = Modifier.background(ProtonTheme.colors.backgroundInvertedNorm)) {
            GmailImportInfoContent(email = "james.miller@gmail.com", onOpenInbox = {})
        }
    }
}
