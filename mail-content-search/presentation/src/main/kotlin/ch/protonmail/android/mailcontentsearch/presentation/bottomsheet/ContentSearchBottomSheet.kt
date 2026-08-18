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

package ch.protonmail.android.mailcontentsearch.presentation.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import ch.protonmail.android.design.compose.component.ProtonTextButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.ui.MobileDataCard

@Composable
fun ContentSearchBottomSheet(
    isMobileDataEnabled: Boolean,
    actions: ContentSearchBottomSheet.Actions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ProtonTheme.colors.backgroundInvertedNorm)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = ProtonDimens.Spacing.Standard)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(ProtonDimens.IconSize.SemiLarge),
            color = ProtonTheme.colors.brandNorm
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.content_search_bottomsheet_title),
            style = ProtonTheme.typography.titleLarge,
            color = ProtonTheme.colors.textNorm,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

        DescriptionText(onOpenSettings = actions.onOpenSettings)

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

        MobileDataCard(
            modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Standard),
            isEnabled = isMobileDataEnabled,
            onToggle = actions.onToggleMobileData
        )

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

        ProtonTextButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.Standard)
                .background(
                    color = ProtonTheme.colors.brandNorm,
                    shape = ProtonTheme.shapes.massive
                ),
            onClick = actions.onDismiss
        ) {
            Text(
                text = stringResource(id = R.string.content_search_bottomsheet_ok),
                style = ProtonTheme.typography.titleMedium,
                color = ProtonTheme.colors.textInverted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
    }
}

@Composable
private fun DescriptionText(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    val settingsLink = stringResource(id = R.string.content_search_bottomsheet_settings_link)
    val description = stringResource(id = R.string.content_search_bottomsheet_description, settingsLink)
    val linkColor = ProtonTheme.colors.brandNorm

    val prefix = description.substringBefore(settingsLink)
    val suffix = description.substringAfter(settingsLink)

    val annotatedString = buildAnnotatedString {
        append(prefix)
        withLink(
            LinkAnnotation.Clickable(
                tag = "content_search_bottomsheet_settings",
                styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                linkInteractionListener = { onOpenSettings() }
            )
        ) {
            append(settingsLink)
        }
        append(suffix)
    }

    Text(
        modifier = modifier.fillMaxWidth(),
        text = annotatedString,
        style = ProtonTheme.typography.bodyLarge,
        color = ProtonTheme.colors.textWeak,
        textAlign = TextAlign.Center
    )
}

object ContentSearchBottomSheet {

    data class Actions(
        val onOpenSettings: () -> Unit,
        val onDismiss: () -> Unit,
        val onToggleMobileData: (Boolean) -> Unit
    )
}

@AdaptivePreviews
@Composable
private fun PreviewContentSearchBottomSheet() {
    ProtonTheme {
        ContentSearchBottomSheet(
            isMobileDataEnabled = true,
            actions = ContentSearchBottomSheet.Actions(
                onOpenSettings = {},
                onDismiss = {},
                onToggleMobileData = {}
            )
        )
    }
}
