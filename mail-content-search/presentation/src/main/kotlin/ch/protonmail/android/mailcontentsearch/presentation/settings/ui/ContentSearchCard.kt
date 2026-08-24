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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import ch.protonmail.android.design.compose.component.ProtonSecondaryButton
import ch.protonmail.android.design.compose.component.ProtonSettingsToggleItem
import ch.protonmail.android.design.compose.component.protonSecondaryButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcontentsearch.presentation.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ContentSearchCard(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    syncPercentage: Double?,
    isIndexingActive: Boolean,
    isWaitingForUnmeteredConnection: Boolean,
    isIndexingFailed: Boolean,
    isRetryingIndexing: Boolean,
    actions: ContentSearchCardActions
) {
    val showFailure = isEnabled && isIndexingFailed

    // No grace period for the Wi-Fi wait: unlike the gap before the first progress event, it is a
    // state the user put the app in, and it lasts until they change network or the setting.
    var showPreparing by remember { mutableStateOf(false) }
    LaunchedEffect(isIndexingActive, syncPercentage, isWaitingForUnmeteredConnection) {
        if (isIndexingActive && syncPercentage == null && !isWaitingForUnmeteredConnection) {
            delay(PREPARING_DELAY_MILLIS.milliseconds)
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
                DescriptionText(onLearnMoreClick = actions.onLearnMoreClick)
            }

            MailDivider()

            ProtonSettingsToggleItem(
                modifier = Modifier.padding(
                    horizontal = ProtonDimens.Spacing.Large,
                    vertical = ProtonDimens.Spacing.Medium
                ),
                value = isEnabled,
                onToggle = actions.onToggle
            ) {
                Text(
                    text = stringResource(id = R.string.mail_settings_content_search_toggle_title),
                    style = ProtonTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )

                val statusText = when {
                    // Nothing here when the account has failed.
                    showFailure -> null

                    isWaitingForUnmeteredConnection -> stringResource(
                        id = R.string.mail_settings_content_search_waiting_for_wifi_status
                    )

                    syncPercentage != null -> stringResource(
                        id = R.string.mail_settings_content_search_syncing_status,
                        syncPercentage.roundToInt()
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

            if (showFailure) {
                MailDivider()
                IndexingFailureRow(
                    isRetrying = isRetryingIndexing,
                    onRetryClick = actions.onRetryClick
                )
            }
        }
    }
}

internal data class ContentSearchCardActions(
    val onToggle: (Boolean) -> Unit,
    val onRetryClick: () -> Unit,
    val onLearnMoreClick: () -> Unit
)

@Composable
private fun IndexingFailureRow(
    modifier: Modifier = Modifier,
    isRetrying: Boolean,
    onRetryClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ProtonDimens.Spacing.Large,
                vertical = ProtonDimens.Spacing.Medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.mail_settings_content_search_failed_status),
            style = ProtonTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = ProtonTheme.colors.notificationError
        )
        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Medium))
        ProtonSecondaryButton(
            onClick = onRetryClick,
            loading = isRetrying,
            colors = ButtonDefaults.protonSecondaryButtonColors(
                loading = isRetrying,
                backgroundColor = ProtonTheme.colors.backgroundInvertedNorm
            )
        ) {

            if (!isRetrying) {
                Text(
                    text = stringResource(id = R.string.mail_settings_content_search_failed_retry),
                    style = ProtonTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                )
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
