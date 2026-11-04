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

package ch.protonmail.android.mailbyoe.presentation.spotlight

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
import ch.protonmail.android.design.compose.component.ProtonButton
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.ProtonSwitch
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbyoe.presentation.R
import ch.protonmail.android.mailbyoe.presentation.byoeColors

/**
 * Full-screen BYOE prompt for existing users in a treatment group.
 *
 * [onConnect] receives whether the user wants their existing Gmail emails imported.
 */
@Composable
fun ByoeSpotlightScreen(
    onConnect: (importEmails: Boolean) -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onLater)

    ByoeSpotlightContent(onConnect = onConnect, onLater = onLater, modifier = modifier)
}

@Composable
private fun ByoeSpotlightContent(
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
            ByoeSpotlightIllustration()
            ByoeSpotlightHeader()
            ByoeSpotlightBenefits(
                importEmails = importEmails,
                onImportEmailsChange = { importEmails = it }
            )
        }

        ByoeSpotlightActions(onConnect = { onConnect(importEmails) }, onLater = onLater)
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
private fun ByoeSpotlightHeader() {
    Column(
        modifier = Modifier.padding(
            start = ProtonDimens.Spacing.ExtraLarge,
            end = ProtonDimens.Spacing.ExtraLarge,
            bottom = ProtonDimens.Spacing.MediumLight
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ModeratelyLarge)
    ) {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.MediumLight)
        ) {
            Text(
                text = stringResource(R.string.byoe_spotlight_title),
                style = ProtonTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ProtonTheme.colors.textNorm,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.byoe_spotlight_subtitle),
                style = ProtonTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ProtonTheme.colors.textWeak,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ByoeSpotlightBenefits(importEmails: Boolean, onImportEmailsChange: (Boolean) -> Unit) {
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
            benefits.forEach { benefit -> ByoeSpotlightBenefitRow(benefit) }
        }
        HorizontalDivider(color = byoeColors().cardDivider)
        ByoeSpotlightImportRow(checked = importEmails, onCheckedChange = onImportEmailsChange)
    }
}

@Composable
private fun ByoeSpotlightBenefitRow(@StringRes benefit: Int) {
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
            style = ProtonTheme.typography.bodyMedium,
            color = ProtonTheme.colors.textNorm
        )
    }
}

@Composable
private fun ByoeSpotlightImportRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // The whole row toggles, so TalkBack reads the label with the switch and the tap target is the row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(ProtonDimens.Spacing.Large),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)
        ) {
            Text(
                text = stringResource(R.string.byoe_spotlight_import_title),
                style = ProtonTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ProtonTheme.colors.textNorm
            )
            Text(
                text = stringResource(R.string.byoe_spotlight_import_body),
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textWeak
            )
        }
        ProtonSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

@Composable
private fun ByoeSpotlightActions(onConnect: () -> Unit, onLater: () -> Unit) {
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
        ByoeSpotlightPermissionsNote()
    }
}

@Composable
private fun ByoeSpotlightPermissionsNote() {
    val text = buildAnnotatedString {
        appendInlineContent(INFO_ICON_ID)
        append(" ")
        append(stringResource(R.string.byoe_spotlight_permissions_note))
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
        style = ProtonTheme.typography.bodySmall,
        color = ProtonTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ActionLabel(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = ProtonTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold
    )
}

private val benefits = listOf(
    R.string.byoe_spotlight_benefit_read,
    R.string.byoe_spotlight_benefit_no_ads,
    R.string.byoe_spotlight_benefit_no_tracking
)

// The artwork's own size, which includes room for its shadows.
private val IllustrationWidth = 252.dp
private val IllustrationHeight = 144.dp
private val BenefitIconContainerSize = 32.dp
// Leaves the design's gap between the last benefit and the import row.
private val BenefitListBottomPadding = 34.dp
private val ActionButtonHeight = 52.dp
private const val INFO_ICON_ID = "info"

@Preview(widthDp = 412, heightDp = 917)
@Preview(widthDp = 412, heightDp = 917, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ByoeSpotlightPreview() {
    ProtonTheme {
        ByoeSpotlightScreen(onConnect = {}, onLater = {})
    }
}
