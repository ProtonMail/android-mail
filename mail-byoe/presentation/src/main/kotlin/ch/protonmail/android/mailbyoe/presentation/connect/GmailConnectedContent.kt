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

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbyoe.presentation.R
import ch.protonmail.android.mailbyoe.presentation.byoeColors
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Shown once the Gmail account is connected, whichever entry point started the connection. [email] is the
 * connected address when the web page reported it. Without [importEmails], it says older emails stay in Gmail;
 * with it, "Learn what gets imported" opens [GmailImportInfoSheet].
 */
@Composable
internal fun GmailConnectedContent(
    email: String?,
    importEmails: Boolean,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportInfo by rememberSaveable { mutableStateOf(false) }

    GmailImportInfoSheet(
        show = showImportInfo,
        email = email,
        onOpenInbox = onOpenInbox,
        onDismiss = { showImportInfo = false }
    ) {
        GmailConnectedPage(
            email = email,
            importEmails = importEmails,
            onOpenInbox = onOpenInbox,
            onLearnWhatGetsImported = { showImportInfo = true },
            modifier = modifier
        )
    }
}

@Composable
private fun GmailConnectedPage(
    email: String?,
    importEmails: Boolean,
    onOpenInbox: () -> Unit,
    onLearnWhatGetsImported: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundNorm)
            .background(byoeColors().backgroundTint)
            .safeDrawingPadding()
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = ProtonDimens.Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Huge)
        ) {
            GmailConnectedHeader(email)
            GmailConnectedInfo(importEmails, onLearnWhatGetsImported)
        }

        GmailConnectedOpenInboxButton(
            onClick = onOpenInbox,
            modifier = Modifier.padding(top = ProtonDimens.Spacing.ExtraLarge, bottom = ProtonDimens.Spacing.Compact)
        )
    }
}

@Composable
private fun GmailConnectedHeader(email: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ModeratelyLarge)
    ) {
        GmailConnectedAnimation()
        Column(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.MediumLight)
        ) {
            Text(
                text = stringResource(R.string.byoe_gmail_connected_title),
                style = ProtonTheme.typography.headlineMedium.copy(lineHeight = TitleLineHeight),
                fontWeight = FontWeight.Bold,
                color = ProtonTheme.colors.textNorm,
                textAlign = TextAlign.Center
            )
            email?.let {
                Text(
                    text = it,
                    style = ProtonTheme.typography.bodyMedium.copy(fontSize = BodyFontSize),
                    fontWeight = FontWeight.Medium,
                    color = ProtonTheme.colors.textWeak,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Plays once and stays on its last frame.
@Composable
private fun GmailConnectedAnimation() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.byoe_gmail_connected))
    LottieAnimation(
        composition = composition,
        modifier = Modifier.size(IllustrationWidth, IllustrationHeight)
    )
}

@Composable
private fun GmailConnectedInfo(importEmails: Boolean, onLearnWhatGetsImported: () -> Unit) {
    val shape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge)
    val rows = listOf(
        GmailConnectedInfoRow(
            icon = R.drawable.ic_proton_envelope_dot,
            title = R.string.byoe_gmail_connected_stay_on_top_title,
            body = AnnotatedString(stringResource(R.string.byoe_gmail_connected_stay_on_top_body))
        ),
        GmailConnectedInfoRow(
            icon = R.drawable.ic_byoe_bell_slash,
            title = R.string.byoe_gmail_connected_notifications_title,
            body = AnnotatedString(stringResource(R.string.byoe_gmail_connected_notifications_body))
        ),
        if (importEmails) {
            GmailConnectedInfoRow(
                icon = R.drawable.ic_proton_arrows_rotate,
                title = R.string.byoe_gmail_connected_import_title,
                body = importBody(onLearnWhatGetsImported)
            )
        } else {
            GmailConnectedInfoRow(
                icon = R.drawable.ic_proton_arrows_rotate,
                title = R.string.byoe_gmail_connected_older_emails_title,
                body = AnnotatedString(stringResource(R.string.byoe_gmail_connected_older_emails_body))
            )
        }
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(byoeColors().cardBackground)
            .border(width = ProtonDimens.BorderSize.Default, brush = byoeColors().cardBorder, shape = shape)
    ) {
        rows.forEachIndexed { index, row ->
            GmailConnectedInfoRowContent(row = row, showDivider = index != rows.lastIndex)
        }
    }
}

@Composable
private fun importBody(onLearnWhatGetsImported: () -> Unit): AnnotatedString {
    val body = stringResource(R.string.byoe_gmail_connected_import_body)
    val learnMore = stringResource(R.string.byoe_gmail_connected_import_learn_more)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = ProtonTheme.colors.textAccent, textDecoration = TextDecoration.None)
    )
    return buildAnnotatedString {
        append(body)
        append(" ")
        val link = LinkAnnotation.Clickable(
            tag = LEARN_WHAT_GETS_IMPORTED_TAG,
            styles = linkStyles,
            linkInteractionListener = { onLearnWhatGetsImported() }
        )
        withLink(link) { append(learnMore) }
    }
}

private data class GmailConnectedInfoRow(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    val body: AnnotatedString
)

@Composable
private fun GmailConnectedInfoRowContent(row: GmailConnectedInfoRow, showDivider: Boolean) {
    Row(
        modifier = Modifier.padding(start = ProtonDimens.Spacing.Large, top = ProtonDimens.Spacing.ExtraLarge),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
    ) {
        Box(
            modifier = Modifier
                .size(InfoIconContainerSize)
                .clip(RoundedCornerShape(InfoIconContainerRadius))
                .background(byoeColors().iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(row.icon),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm,
                modifier = Modifier.size(ProtonDimens.IconSize.Medium)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.padding(end = ProtonDimens.Spacing.Large, bottom = ProtonDimens.Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
            ) {
                Text(
                    text = stringResource(row.title),
                    style = ProtonTheme.typography.titleSmall.copy(fontSize = BodyFontSize),
                    fontWeight = FontWeight.SemiBold,
                    color = ProtonTheme.colors.textNorm
                )
                Text(
                    text = row.body,
                    style = ProtonTheme.typography.bodySmall.copy(
                        fontSize = SmallFontSize,
                        lineHeight = SmallLineHeight
                    ),
                    color = ProtonTheme.colors.textWeak
                )
            }
            if (showDivider) HorizontalDivider(color = byoeColors().cardDivider)
        }
    }
}

/** The page's main action, which the import info sheet repeats. */
@Composable
internal fun GmailConnectedOpenInboxButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ProtonSolidButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.protonButtonColors(contentColor = ProtonTheme.colors.textInverted),
        modifier = modifier
            .fillMaxWidth()
            .height(ActionButtonHeight)
    ) {
        Text(
            text = stringResource(R.string.byoe_gmail_connected_open_inbox),
            style = ProtonTheme.typography.bodyLarge.copy(lineHeight = ActionLineHeight),
            fontWeight = FontWeight.SemiBold
        )
    }
}

private const val LEARN_WHAT_GETS_IMPORTED_TAG = "learnWhatGetsImported"

private val IllustrationWidth = 200.dp
private val IllustrationHeight = 172.dp
private val InfoIconContainerSize = 32.dp
private val InfoIconContainerRadius = 10.dp
private val TitleLineHeight = 34.sp
private val BodyFontSize = 15.sp
private val SmallFontSize = 13.sp
private val SmallLineHeight = 18.sp
private val ActionLineHeight = 21.sp
private val ActionButtonHeight = 53.dp

@Preview(widthDp = 412, heightDp = 917)
@Preview(widthDp = 412, heightDp = 917, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GmailConnectedPreview() {
    ProtonTheme {
        GmailConnectedContent(email = "james.miller@gmail.com", importEmails = true, onOpenInbox = {})
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun GmailConnectedWithoutImportPreview() {
    ProtonTheme {
        GmailConnectedContent(email = "james.miller@gmail.com", importEmails = false, onOpenInbox = {})
    }
}
