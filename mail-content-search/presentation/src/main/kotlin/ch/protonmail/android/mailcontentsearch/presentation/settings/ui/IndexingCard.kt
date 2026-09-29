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

import java.util.Locale
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonButton
import ch.protonmail.android.design.compose.component.protonSecondaryButtonColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.ContentSearchSettingsState

/**
 * How far this account's index has got, and the one control over it.
 *
 * Shown only while content search is on for the account: there is no progress to report otherwise.
 */
@Composable
internal fun IndexingCard(
    state: ContentSearchSettingsState.Data,
    actions: IndexingCardActions,
    modifier: Modifier = Modifier
) {
    val status = state.toIndexingCardStatus()

    // Animated so each progress report slides the boundary along instead of snapping it.
    val progress by animateFloatAsState(
        targetValue = (state.displayPercentage() / FULL_PERCENTAGE).toFloat(),
        animationSpec = tween(durationMillis = PROGRESS_ANIMATION_MILLIS, easing = LinearEasing),
        label = "indexingProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(modifier = Modifier.padding(ProtonDimens.Spacing.Large)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            id = R.string.mail_settings_content_search_indexing_percentage,
                            state.displayPercentage().formatPercentage()
                        ),
                        style = ProtonTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = status.progressColor(),
                        trackColor = ProtonTheme.colors.shade20,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

                    IndexingStatusRow(status = status)
                }

                Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Huge))

                ProtonButton(
                    onClick = { actions.forStatus(status)() },
                    enabled = status.isActionEnabled,
                    loading = state.isRetryingIndexing,
                    elevation = null,
                    shape = CircleShape,
                    border = if (status.isActionEnabled || state.isRetryingIndexing) {
                        BorderStroke(width = 1.dp, color = ProtonTheme.colors.borderNorm)
                    } else {
                        null
                    },
                    contentPadding = PaddingValues(
                        horizontal = ProtonDimens.Spacing.Large,
                        vertical = ProtonDimens.Spacing.Standard
                    ),
                    colors = ButtonDefaults.protonSecondaryButtonColors(
                        loading = state.isRetryingIndexing,
                        backgroundColor = ProtonTheme.colors.interactionWeakNorm
                    )
                ) {
                    if (!state.isRetryingIndexing) {
                        Text(
                            text = stringResource(id = status.actionLabel),
                            style = ProtonTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            if (status != IndexingCardStatus.UpToDate) {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

                Text(
                    text = stringResource(id = R.string.mail_settings_content_search_indexing_description),
                    style = ProtonTheme.typography.bodyMedium,
                    color = ProtonTheme.colors.textWeak
                )
            }
        }
    }
}

internal data class IndexingCardActions(
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onRetry: () -> Unit
)

private fun IndexingCardActions.forStatus(status: IndexingCardStatus): () -> Unit = when (status) {
    IndexingCardStatus.Failed -> onRetry
    IndexingCardStatus.Paused -> onResume
    IndexingCardStatus.WaitingForWifi,
    IndexingCardStatus.InProgress,
    IndexingCardStatus.UpToDate -> onPause
}

@Composable
private fun IndexingStatusRow(status: IndexingCardStatus, modifier: Modifier = Modifier) {
    val color = if (status == IndexingCardStatus.Failed) {
        ProtonTheme.colors.notificationError
    } else {
        ProtonTheme.colors.textWeak
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        status.icon?.let { icon ->
            Icon(
                modifier = Modifier.size(ProtonDimens.IconSize.ExtraSmall),
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Small))
        }
        Text(
            text = stringResource(id = status.label),
            style = ProtonTheme.typography.labelMedium,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The card's status, and the action offered for it.
 *
 * [UpToDate] is the one status with nothing to offer: an account with no backfill left cannot be
 * paused. Its button is shown but disabled, so the card keeps the same shape throughout.
 */
private enum class IndexingCardStatus(
    @param:StringRes val label: Int,
    @param:StringRes val actionLabel: Int,
    @param:DrawableRes val icon: Int?,
    val isActionEnabled: Boolean
) {
    Failed(
        label = R.string.mail_settings_content_search_failed_status,
        actionLabel = R.string.mail_settings_content_search_failed_retry,
        icon = R.drawable.ic_sync_problem,
        isActionEnabled = true
    ),
    Paused(
        label = R.string.mail_settings_content_search_indexing_paused,
        actionLabel = R.string.mail_settings_content_search_indexing_resume,
        icon = null,
        isActionEnabled = true
    ),
    WaitingForWifi(
        label = R.string.mail_settings_content_search_waiting_for_wifi_status,
        actionLabel = R.string.mail_settings_content_search_indexing_pause,
        icon = R.drawable.ic_sync,
        isActionEnabled = true
    ),
    InProgress(
        label = R.string.mail_settings_content_search_indexing_in_progress,
        actionLabel = R.string.mail_settings_content_search_indexing_pause,
        icon = R.drawable.ic_sync,
        isActionEnabled = true
    ),
    UpToDate(
        label = R.string.mail_settings_content_search_indexing_up_to_date,
        actionLabel = R.string.mail_settings_content_search_indexing_pause,
        icon = null,
        isActionEnabled = false
    )
}

@Composable
private fun IndexingCardStatus.progressColor(): Color = if (this == IndexingCardStatus.Failed) {
    ProtonTheme.colors.notificationError
} else {
    ProtonTheme.colors.textAccent
}

private fun ContentSearchSettingsState.Data.toIndexingCardStatus(): IndexingCardStatus = when {
    isIndexingFailed -> IndexingCardStatus.Failed
    isIndexingPaused -> IndexingCardStatus.Paused
    isWaitingForUnmeteredConnection -> IndexingCardStatus.WaitingForWifi
    isIndexingCompleted -> IndexingCardStatus.UpToDate
    else -> IndexingCardStatus.InProgress
}

private fun ContentSearchSettingsState.Data.displayPercentage(): Double = when {
    syncPercentage != null -> syncPercentage
    isIndexingCompleted -> FULL_PERCENTAGE
    else -> 0.0
}

private fun Double.formatPercentage(): String = if (this == this.toLong().toDouble()) {
    this.toLong().toString()
} else {
    String.format(Locale.getDefault(), "%.2f", this)
}

private const val FULL_PERCENTAGE = 100.0

private const val PROGRESS_ANIMATION_MILLIS = 500

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "In progress")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "In progress dark")
@Composable
private fun IndexingCardInProgressPreview() {
    ProtonInvertedTheme {
        IndexingCard(
            state = PreviewState.copy(syncPercentage = 30.0, isIndexingActive = true),
            actions = PreviewIndexingActions
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Up to date")
@Composable
private fun IndexingCardUpToDatePreview() {
    ProtonInvertedTheme {
        IndexingCard(
            state = PreviewState.copy(isIndexingCompleted = true),
            actions = PreviewIndexingActions
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true, name = "Failed")
@Composable
private fun IndexingCardFailedPreview() {
    ProtonInvertedTheme {
        IndexingCard(
            state = PreviewState.copy(syncPercentage = 30.0, isIndexingFailed = true),
            actions = PreviewIndexingActions
        )
    }
}

private val PreviewState = ContentSearchSettingsState.Data(
    isContentSearchEnabled = true,
    isAllowMobileDataEnabled = true,
    syncPercentage = null
)

private val PreviewIndexingActions = IndexingCardActions(onPause = {}, onResume = {}, onRetry = {})
