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

package ch.protonmail.android.mailbyoe.presentation.intro

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.protonmail.android.design.compose.component.ProtonButton
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.ProtonSwitch
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.presentation.R
import ch.protonmail.android.mailbyoe.presentation.byoeColors

/**
 * Full-screen introduction to the Gmail connection, shown before the connection page opens. Existing users get it
 * as a spotlight that presents the feature as new; onboarding, the sidebar and settings show it as [isSpotlight]
 * false. [treatment] picks the experiment copy.
 *
 * [onConnect] receives whether the user wants their existing Gmail emails imported. [onLater] receives how the user
 * left: the "Later" button, or the system back gesture (a dismiss gesture, so [ByoeDismissMethod.SwipeDown]).
 */
@Composable
fun GmailIntroScreen(
    isSpotlight: Boolean,
    treatment: ByoeTreatment,
    onConnect: (importEmails: Boolean) -> Unit,
    onLater: (method: ByoeDismissMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = { onLater(ByoeDismissMethod.SwipeDown) })

    GmailIntroContent(
        isSpotlight = isSpotlight,
        copy = GmailIntroCopy.of(treatment),
        onConnect = onConnect,
        onLater = { onLater(ByoeDismissMethod.Button) },
        modifier = modifier
    )
}

@Composable
private fun GmailIntroContent(
    isSpotlight: Boolean,
    copy: GmailIntroCopy,
    onConnect: (importEmails: Boolean) -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    var importEmails by rememberSaveable { mutableStateOf(true) }

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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ExtraLarge)
        ) {
            if (isSpotlight) {
                ByoeSpotlightIllustration()
                GmailIntroHeader(showBadge = true, subtitle = copy.subtitle)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ModeratelyLarge)
                ) {
                    GmailIntroIllustration()
                    GmailIntroHeader(showBadge = false, subtitle = copy.subtitle)
                }
            }
            GmailIntroBenefits(
                benefits = copy.benefits,
                importEmails = importEmails,
                onImportEmailsChange = { importEmails = it }
            )
        }

        GmailIntroActions(
            permissionsNote = copy.permissionsNote,
            onConnect = { onConnect(importEmails) },
            onLater = onLater
        )
    }
}

@Composable
private fun ByoeSpotlightIllustration() {
    Image(
        painter = painterResource(R.drawable.byoe_spotlight_celebration),
        contentDescription = null,
        modifier = Modifier
            .padding(top = ProtonDimens.Spacing.ExtraLarge)
            .size(IllustrationWidth, IllustrationHeight)
    )
}

@Composable
private fun GmailIntroIllustration() {
    Image(
        painter = painterResource(R.drawable.byoe_intro_gmail_to_proton),
        contentDescription = null,
        modifier = Modifier
            .padding(top = ProtonDimens.Spacing.ExtraLarge)
            .size(IntroIllustrationWidth, IntroIllustrationHeight)
    )
}

@Composable
private fun GmailIntroHeader(showBadge: Boolean, @StringRes subtitle: Int) {
    Column(
        modifier = Modifier.padding(
            start = ProtonDimens.Spacing.ExtraLarge,
            end = ProtonDimens.Spacing.ExtraLarge,
            // The intro's header sits closer to the benefits than the spotlight's.
            bottom = if (showBadge) ProtonDimens.Spacing.MediumLight else ProtonDimens.Spacing.Standard
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ModeratelyLarge)
    ) {
        if (showBadge) ByoeSpotlightBadge()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.MediumLight)
        ) {
            Text(
                text = stringResource(R.string.byoe_spotlight_title),
                style = ProtonTheme.typography.headlineMedium.copy(lineHeight = TitleLineHeight),
                fontWeight = FontWeight.Bold,
                color = ProtonTheme.colors.textNorm,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(subtitle),
                style = ProtonTheme.typography.bodyMedium.copy(fontSize = BodyFontSize),
                fontWeight = FontWeight.Medium,
                color = ProtonTheme.colors.textWeak,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ByoeSpotlightBadge() {
    Text(
        text = stringResource(R.string.byoe_spotlight_badge),
        style = ProtonTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = byoeColors().badgeText,
        modifier = Modifier
            .clip(CircleShape)
            .background(byoeColors().badgeBackground)
            .padding(horizontal = ProtonDimens.Spacing.Standard, vertical = ProtonDimens.Spacing.Small)
    )
}

@Composable
private fun GmailIntroBenefits(
    benefits: List<Int>,
    importEmails: Boolean,
    onImportEmailsChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(byoeColors().cardBackground)
            .border(width = ProtonDimens.Spacing.ExtraTiny, brush = byoeColors().cardBorder, shape = shape)
    ) {
        Column(
            modifier = Modifier.padding(
                start = ProtonDimens.Spacing.Large,
                end = ProtonDimens.Spacing.Large,
                top = ProtonDimens.Spacing.ExtraLarge,
                bottom = BenefitListBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
        ) {
            benefits.forEach { benefit -> GmailIntroBenefitRow(benefit) }
        }
        HorizontalDivider(color = byoeColors().cardDivider)
        GmailIntroImportRow(checked = importEmails, onCheckedChange = onImportEmailsChange)
    }
}

@Composable
private fun GmailIntroBenefitRow(@StringRes benefit: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(BenefitIconContainerSize), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_proton_checkmark),
                contentDescription = null,
                tint = ProtonTheme.colors.iconNorm,
                modifier = Modifier.size(ProtonDimens.IconSize.Default)
            )
        }
        Text(
            text = stringResource(benefit),
            style = ProtonTheme.typography.bodyMedium.copy(fontSize = BodyFontSize),
            color = ProtonTheme.colors.textNorm
        )
    }
}

@Composable
private fun GmailIntroImportRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // The whole row toggles, so TalkBack reads the label with the switch and the tap target is the row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(ProtonDimens.Spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ImportRowContentHeight)
                .padding(vertical = ProtonDimens.Spacing.Tiny),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
        ) {
            Text(
                text = stringResource(R.string.byoe_spotlight_import_title),
                style = ProtonTheme.typography.titleSmall.copy(fontSize = BodyFontSize),
                fontWeight = FontWeight.SemiBold,
                color = ProtonTheme.colors.textNorm
            )
            Text(
                text = stringResource(R.string.byoe_spotlight_import_body),
                style = ProtonTheme.typography.bodySmall.copy(fontSize = SmallFontSize, lineHeight = SmallLineHeight),
                color = ProtonTheme.colors.textWeak
            )
        }
        // The switch keeps its touch target but takes only the design's row height.
        ProtonSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(ImportRowContentHeight).clearAndSetSemantics {}
        )
    }
}

@Composable
private fun GmailIntroActions(
    @StringRes permissionsNote: Int,
    onConnect: () -> Unit,
    onLater: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = ProtonDimens.Spacing.Standard, bottom = ProtonDimens.Spacing.Compact),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.MediumLight)
    ) {
        ProtonSolidButton(
            onClick = onConnect,
            shape = CircleShape,
            colors = ButtonDefaults.protonButtonColors(contentColor = ProtonTheme.colors.textInverted),
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionButtonHeight)
        ) {
            ActionLabel(R.string.byoe_spotlight_connect)
        }
        ProtonButton(
            onClick = onLater,
            shape = CircleShape,
            border = null,
            elevation = null,
            colors = ButtonDefaults.protonButtonColors(
                backgroundColor = ProtonTheme.colors.interactionBrandWeakNorm,
                contentColor = ProtonTheme.colors.textAccent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionButtonHeight)
        ) {
            ActionLabel(R.string.byoe_spotlight_later)
        }
        GmailIntroPermissionsNote(permissionsNote)
    }
}

@Composable
private fun GmailIntroPermissionsNote(@StringRes note: Int) {
    val text = buildAnnotatedString {
        appendInlineContent(INFO_ICON_ID)
        append(" ")
        append(stringResource(note))
    }
    val inlineContent = mapOf(
        INFO_ICON_ID to InlineTextContent(
            Placeholder(width = 1.em, height = 1.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_proton_info_circle),
                contentDescription = null,
                tint = ProtonTheme.colors.textWeak
            )
        }
    )
    Text(
        text = text,
        inlineContent = inlineContent,
        style = ProtonTheme.typography.bodySmall.copy(fontSize = SmallFontSize, lineHeight = SmallLineHeight),
        color = ProtonTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ActionLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = ProtonTheme.typography.bodyLarge.copy(lineHeight = ActionLineHeight),
        fontWeight = FontWeight.SemiBold
    )
}

// The artwork's own size, which includes room for its shadows.
private val IllustrationWidth = 252.dp
private val IllustrationHeight = 144.dp
private val IntroIllustrationWidth = 259.dp
private val IntroIllustrationHeight = 142.dp
private val BenefitIconContainerSize = 32.dp
// Leaves the design's gap between the last benefit and the import row.
private val BenefitListBottomPadding = 34.dp
private val ImportRowContentHeight = 46.dp
private val TitleLineHeight = 34.sp
private val BodyFontSize = 15.sp
private val SmallFontSize = 13.sp
private val SmallLineHeight = 18.sp
private val ActionLineHeight = 21.sp
private val ActionButtonHeight = 53.dp
private const val INFO_ICON_ID = "info"

@Preview(widthDp = 412, heightDp = 917)
@Preview(widthDp = 412, heightDp = 917, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ByoeSpotlightPreview() {
    ProtonTheme {
        GmailIntroScreen(isSpotlight = true, treatment = ByoeTreatment.Treatment1, onConnect = {}, onLater = {})
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun ByoeSpotlightTreatment2Preview() {
    ProtonTheme {
        GmailIntroScreen(isSpotlight = true, treatment = ByoeTreatment.Treatment2, onConnect = {}, onLater = {})
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Preview(widthDp = 412, heightDp = 917, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GmailIntroPreview() {
    ProtonTheme {
        GmailIntroScreen(isSpotlight = false, treatment = ByoeTreatment.Treatment1, onConnect = {}, onLater = {})
    }
}

@Preview(widthDp = 412, heightDp = 917)
@Composable
private fun GmailIntroTreatment2Preview() {
    ProtonTheme {
        GmailIntroScreen(isSpotlight = false, treatment = ByoeTreatment.Treatment2, onConnect = {}, onLater = {})
    }
}
