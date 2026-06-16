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

package ch.protonmail.android.mailcontentsearch.presentation.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import ch.protonmail.android.design.compose.component.ProtonSettingsToggleItem
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcontentsearch.presentation.R
import kotlinx.coroutines.delay

@Composable
internal fun ContentSearchCard(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    syncPercentage: Double?,
    isIndexingActive: Boolean,
    isBlockedByOtherUser: Boolean,
    onToggle: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit
) {
    var showPreparing by remember { mutableStateOf(false) }
    LaunchedEffect(isIndexingActive, syncPercentage) {
        if (isIndexingActive && syncPercentage == null) {
            delay(PREPARING_DELAY_MILLIS)
            showPreparing = true
        } else {
            showPreparing = false
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ProtonDimens.Spacing.Large)
            ) {
                Text(
                    text = stringResource(id = R.string.mail_settings_content_search_header_title),
                    style = ProtonTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))
                DescriptionText(onLearnMoreClick = onLearnMoreClick)
            }

            MailDivider()

            ProtonSettingsToggleItem(
                modifier = Modifier.padding(
                    horizontal = ProtonDimens.Spacing.Large,
                    vertical = ProtonDimens.Spacing.Medium
                ),
                value = if (isBlockedByOtherUser) null else isEnabled,
                onToggle = onToggle
            ) {
                Text(
                    text = stringResource(id = R.string.mail_settings_content_search_toggle_title),
                    style = ProtonTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )

                if (isBlockedByOtherUser) {
                    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
                    Text(
                        text = stringResource(
                            id = R.string.mail_settings_content_search_blocked_by_other_user
                        ),
                        style = ProtonTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    val statusText = when {
                        syncPercentage != null -> stringResource(
                            id = R.string.mail_settings_content_search_syncing_status,
                            syncPercentage
                        )

                        showPreparing -> stringResource(
                            id = R.string.mail_settings_content_search_preparing_status
                        )

                        else -> null
                    }
                    if (statusText != null) {
                        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))
                        Text(
                            text = statusText,
                            style = ProtonTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionText(modifier: Modifier = Modifier, onLearnMoreClick: () -> Unit) {
    val description = stringResource(id = R.string.mail_settings_content_search_description)
    val learnMore = stringResource(id = R.string.mail_settings_content_search_learn_more)
    val linkColor = ProtonTheme.colors.brandNorm

    val annotatedString = buildAnnotatedString {
        append(description)
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "learn_more",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.None
                    )
                ),
                linkInteractionListener = { onLearnMoreClick() }
            )
        ) {
            append(learnMore)
        }
    }

    Text(
        text = annotatedString,
        style = ProtonTheme.typography.bodyMedium,
        color = ProtonTheme.colors.textWeak,
        modifier = modifier
    )
}

private const val PREPARING_DELAY_MILLIS = 1_000L
