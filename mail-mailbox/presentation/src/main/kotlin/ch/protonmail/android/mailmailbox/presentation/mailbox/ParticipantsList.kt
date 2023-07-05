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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import ch.protonmail.android.mailcommon.presentation.compose.OfficialBadge
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsList.minLengthParticipantName
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsList.participantNameId
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsList.participantWithBadgeAndSeparatorId
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsList.participantWithBadgeId
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsList.participantWithSeparatorId
import ch.protonmail.android.mailmailbox.presentation.mailbox.ParticipantsList.threeDotsSlotId
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ParticipantsUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun ParticipantsList(
    modifier: Modifier = Modifier,
    participants: ParticipantsUiModel.Participants,
    fontWeight: FontWeight,
    fontColor: Color
) {
    SubcomposeLayout(
        modifier = modifier
    ) { constraints ->
        val participantWithBadgeAndSeparatorMinWidth = measureMinParticipantRowWidth(
            id = participantWithBadgeAndSeparatorId,
            constraints = constraints,
            participant = ParticipantUiModel(minLengthParticipantName, shouldShowOfficialBadge = true),
            displayData = ParticipantRowDisplayData(shouldShowSeparator = true, fontWeight, fontColor)
        )
        val participantWithBadgeMinWidth = measureMinParticipantRowWidth(
            id = participantWithBadgeId,
            constraints = constraints,
            participant = ParticipantUiModel(minLengthParticipantName, shouldShowOfficialBadge = true),
            displayData = ParticipantRowDisplayData(shouldShowSeparator = false, fontWeight, fontColor)
        )
        val participantWithSeparatorMinWidth = measureMinParticipantRowWidth(
            id = participantWithSeparatorId,
            constraints = constraints,
            participant = ParticipantUiModel(minLengthParticipantName, shouldShowOfficialBadge = false),
            displayData = ParticipantRowDisplayData(shouldShowSeparator = true, fontWeight, fontColor)
        )
        val participantNameMinWidth = measureMinParticipantRowWidth(
            id = participantNameId,
            constraints = constraints,
            participant = ParticipantUiModel(minLengthParticipantName, shouldShowOfficialBadge = false),
            displayData = ParticipantRowDisplayData(shouldShowSeparator = false, fontWeight, fontColor)
        )

        val measurables = participants.list.mapIndexed { index, participantUiModel ->
            subcompose("${participantUiModel.name}$index") {
                val displayData = ParticipantRowDisplayData(
                    shouldShowSeparator = shouldShowSeparator(index),
                    fontWeight = fontWeight,
                    fontColor = fontColor
                )
                ParticipantRow(
                    participant = participantUiModel,
                    displayData = displayData
                )
            }.single()
        }
        val threeDotsMeasurable = subcompose(threeDotsSlotId) {
            ThreeDots(fontWeight = fontWeight, fontColor = fontColor)
        }.single()

        fun minWidth(shouldShowOfficialBadge: Boolean, shouldShowSeparator: Boolean) =
            if (shouldShowOfficialBadge && shouldShowSeparator) {
                participantWithBadgeAndSeparatorMinWidth
            } else if (shouldShowOfficialBadge) {
                participantWithBadgeMinWidth
            } else if (shouldShowSeparator) {
                participantWithSeparatorMinWidth
            } else {
                participantNameMinWidth
            }

        var availableWidth = constraints.maxWidth

        val placeables = measurables.mapIndexedNotNull { index, measurable ->
            val participant = participants.list[index]
            val shouldShowSeparator = shouldShowSeparator(index)
            val minWidth = minWidth(participant.shouldShowOfficialBadge, shouldShowSeparator)

            if (minWidth > availableWidth) {
                val threeDotsPlaceable = threeDotsMeasurable.measure(constraints)
                if (availableWidth >= threeDotsPlaceable.width) {
                    // Make sure no more placeables are placed after the three dots
                    availableWidth = 0
                    return@mapIndexedNotNull threeDotsPlaceable
                }
                return@mapIndexedNotNull null
            }

            val placeable = measurable.measure(constraints.copy(minWidth = minWidth, maxWidth = availableWidth))
            availableWidth -= placeable.width
            return@mapIndexedNotNull placeable
        }

        val height = placeables.maxOf { it.height }

        layout(constraints.maxWidth, height) {
            var xPosition = 0

            placeables.forEach { placeable ->
                placeable.placeRelative(x = xPosition, y = 0)
                xPosition += placeable.width
            }
        }
    }
}

private fun SubcomposeMeasureScope.measureMinParticipantRowWidth(
    id: String,
    constraints: Constraints,
    participant: ParticipantUiModel,
    displayData: ParticipantRowDisplayData
) = subcompose(id) {
    ParticipantRow(
        participant = participant,
        displayData = displayData
    )
}.maxOf { it.measure(constraints).width }

private fun shouldShowSeparator(index: Int) = index != 0

@Composable
private fun ParticipantRow(
    modifier: Modifier = Modifier,
    participant: ParticipantUiModel,
    displayData: ParticipantRowDisplayData
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (displayData.shouldShowSeparator) {
            Separator(fontWeight = displayData.fontWeight, fontColor = displayData.fontColor)
        }
        ParticipantName(
            modifier = Modifier
                .testTag(MailboxItemTestTags.Participants)
                .weight(1f, fill = false),
            participantName = participant.name,
            fontWeight = displayData.fontWeight,
            fontColor = displayData.fontColor
        )
        if (participant.shouldShowOfficialBadge) {
            OfficialBadge()
        }
    }
}

@Composable
private fun ParticipantName(
    modifier: Modifier = Modifier,
    participantName: String,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        modifier = modifier,
        text = participantName,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = ProtonTheme.typography.defaultNorm.copy(fontWeight = fontWeight, color = fontColor)
    )
}

@Composable
private fun Separator(
    modifier: Modifier = Modifier,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        modifier = modifier.padding(end = ProtonDimens.ExtraSmallSpacing),
        text = ",",
        style = ProtonTheme.typography.defaultNorm.copy(fontWeight = fontWeight, color = fontColor)
    )
}

@Composable
private fun ThreeDots(
    modifier: Modifier = Modifier,
    fontWeight: FontWeight,
    fontColor: Color
) {
    Text(
        modifier = modifier,
        text = "...",
        style = ProtonTheme.typography.defaultNorm.copy(fontWeight = fontWeight, color = fontColor)
    )
}

data class ParticipantRowDisplayData(
    val shouldShowSeparator: Boolean,
    val fontWeight: FontWeight,
    val fontColor: Color
)

object ParticipantsList {

    const val participantWithBadgeAndSeparatorId = "participantWithBadgeAndSeparator"
    const val participantWithBadgeId = "participantWithBadge"
    const val participantWithSeparatorId = "participantWithSeparator"
    const val participantNameId = "participantName"
    const val threeDotsSlotId = "threeDotsSlotId"
    const val minLengthParticipantName = "P..."
}
