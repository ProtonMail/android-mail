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

package ch.protonmail.android.uicomponents.chips

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusedChipsList(
    chipItems: List<ChipItem>,
    animateChipsCreation: Boolean = false,
    textMaxWidth: Dp,
    onDeleteItem: (Int) -> Unit
) {
    chipItems.forEachIndexed { index, chipItem ->
        val scale by remember { mutableStateOf(Animatable(0F)) }
        val alpha by remember { mutableStateOf(Animatable(0F)) }
        InputChip(
            modifier = Modifier
                .testTag("${ChipsTestTags.InputChip}$index")
                .semantics { isValidField = chipItem !is ChipItem.Invalid }
                .padding(horizontal = 4.dp)
                .thenIf(animateChipsCreation) {
                    scale(scale.value)
                    alpha(alpha.value)
                },
            selected = false,
            onClick = { onDeleteItem(index) },
            label = {
                Text(
                    modifier = Modifier
                        .testTag(ChipsTestTags.InputChipText)
                        .widthIn(max = textMaxWidth - 64.dp),
                    text = chipItem.value,
                    color = when (chipItem) {
                        is ChipItem.Invalid -> Color.Red
                        else -> Color.Unspecified
                    }
                )
            },
            shape = chipShape,
            trailingIcon = {
                Icon(
                    Icons.Default.Clear,
                    modifier = Modifier
                        .testTag(ChipsTestTags.InputChipIcon)
                        .size(16.dp),
                    contentDescription = ""
                )
            }
        )
        LaunchedEffect(key1 = index) {
            if (animateChipsCreation) {
                scale.animateTo(1F)
                alpha.animateTo(1F)
            }
        }
    }
}

@Composable
@Suppress("MagicNumber")
internal fun UnFocusedChipsList(
    itemChip: ChipItem,
    counterChip: ChipItem? = null,
    onChipClick: () -> Unit = {}
) {
    Row {
        SuggestionChip(
            modifier = Modifier
                .testTag(ChipsTestTags.BaseSuggestionChip)
                .semantics { isValidField = itemChip !is ChipItem.Invalid }
                .weight(1f, fill = false)
                .padding(horizontal = 4.dp),
            onClick = onChipClick,
            label = {
                Text(
                    modifier = Modifier.testTag(ChipsTestTags.InputChipText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = itemChip.value,
                    color = when (itemChip) {
                        is ChipItem.Invalid -> Color.Red
                        else -> Color.Unspecified
                    }
                )
            },
            shape = chipShape
        )
        if (counterChip != null) {
            SuggestionChip(
                modifier = Modifier
                    .testTag(ChipsTestTags.AdditionalSuggestionChip)
                    .semantics { isValidField = itemChip !is ChipItem.Invalid }
                    .padding(horizontal = 4.dp),
                onClick = onChipClick,
                label = {
                    Text(
                        modifier = Modifier.testTag(ChipsTestTags.InputChipText),
                        maxLines = 1,
                        text = counterChip.value,
                        color = Color.Unspecified
                    )
                },
                shape = chipShape
            )
        }
    }
}

private val chipShape = RoundedCornerShape(16.dp)
