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

package ch.protonmail.android.mailnps.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetDefaults
import ch.protonmail.android.design.compose.component.ProtonOutlinedTextField
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.protonButtonColors
import ch.protonmail.android.design.compose.component.protonOutlineTextFieldColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailnps.domain.model.NpsScore
import ch.protonmail.android.mailnps.presentation.R
import ch.protonmail.android.mailnps.presentation.model.NpsState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NpsBottomSheetContent(
    state: NpsState,
    actions: NpsBottomSheet.Actions,
    modifier: Modifier = Modifier
) {
    // The sheet background is always light, so its content always uses the light palette, whatever the app theme.
    ProtonTheme(isDark = false) {
        NpsSheet(state = state, actions = actions, modifier = modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NpsSheet(
    state: NpsState,
    actions: NpsBottomSheet.Actions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NpsBottomSheet.Shape)
            .background(sheetBackgroundBrush())
            .navigationBarsPadding()
            .padding(bottom = ProtonDimens.Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
    ) {
        ProtonModalBottomSheetDefaults.DragHandle()

        // No top padding: the drag handle already brings its own vertical padding, which stands in for it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ProtonDimens.Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NpsIllustration()

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

            Text(
                text = stringResource(R.string.nps_title),
                style = ProtonTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = ProtonTheme.colors.textNorm,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ProtonDimens.Spacing.Large)
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Huge))

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                // Figma splits the scale into 0-5 and 6-10, even when a wider sheet could fit more per row.
                maxItemsInEachRow = SCORE_BUTTONS_PER_ROW,
                horizontalArrangement = Arrangement.spacedBy(
                    ProtonDimens.Spacing.Standard,
                    Alignment.CenterHorizontally
                ),
                verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
            ) {
                NpsScore.all.forEach { score ->
                    NpsScoreButton(
                        score = score,
                        selected = state.selectedScore == score,
                        onClick = { actions.onScoreSelected(score) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Caption(text = stringResource(R.string.nps_subtext_low))
                Caption(text = stringResource(R.string.nps_subtext_high))
            }

            AnimatedVisibility(visible = state.isScoreSelected) {
                NpsCommentField(
                    comment = state.comment,
                    onCommentChanged = actions.onCommentChanged,
                    modifier = Modifier.padding(top = ProtonDimens.Spacing.ExtraLarge)
                )
            }

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Massive))

            ProtonSolidButton(
                onClick = actions.onShareClicked,
                enabled = state.isShareEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ShareButtonHeight),
                colors = ButtonDefaults.protonButtonColors(
                    backgroundColor = ProtonTheme.colors.interactionBrandDefaultNorm,
                    contentColor = ProtonTheme.colors.textInverted,
                    disabledBackgroundColor = ProtonTheme.colors.interactionBrandDefaultDisabled,
                    disabledContentColor = ProtonTheme.colors.brandMinus20
                ),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.nps_share_button),
                    style = ProtonTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

            Text(
                text = stringResource(R.string.nps_privacy_note),
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textWeak,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NpsScoreButton(
    score: NpsScore,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) ProtonTheme.colors.interactionBrandDefaultNorm else UnselectedScoreBackground
    val textColor = if (selected) SelectedScoreText else ProtonTheme.colors.textNorm
    val scoreDescription = stringResource(R.string.nps_score_content_description, score.value)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ScoreButtonSize)
            .clip(RoundedCornerShape(ProtonDimens.CornerRadius.ExtraLarge))
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = scoreDescription }
            .padding(ProtonDimens.Spacing.MediumLight)
    ) {
        Text(
            text = score.value.toString(),
            style = ProtonTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun NpsCommentField(
    comment: String,
    onCommentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep the TextFieldValue locally so the cursor and selection survive recompositions; only the text is hoisted.
    var commentValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(comment))
    }

    ProtonOutlinedTextField(
        value = commentValue,
        onValueChange = {
            commentValue = it
            onCommentChanged(it.text)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(CommentFieldHeight),
        shape = RoundedCornerShape(ProtonDimens.CornerRadius.ExtraLarge),
        colors = protonOutlineTextFieldColors().copy(
            unfocusedIndicatorColor = ProtonTheme.colors.borderStrong
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.nps_comment_placeholder),
                style = ProtonTheme.typography.bodyLarge,
                color = ProtonTheme.colors.textHint
            )
        }
    )
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = ProtonTheme.typography.bodyMedium,
        color = ProtonTheme.colors.textWeak
    )
}

@Composable
private fun NpsIllustration() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(IllustrationSize)
            .clip(RoundedCornerShape(IllustrationCornerRadius))
            .drawBehind {
                val (start, end) = cssLinearGradientPoints(ILLUSTRATION_GRADIENT_ANGLE, size.width, size.height)
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            ILLUSTRATION_GRADIENT_START_STOP to IllustrationGradientStart,
                            ILLUSTRATION_GRADIENT_END_STOP to IllustrationGradientEnd
                        ),
                        start = start,
                        end = end
                    )
                )
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_heart),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(HeartIconSize)
        )
    }
}

/**
 * Start and end points of a CSS `linear-gradient(<angle>deg, ...)` over a [width] x [height] box: the angle
 * is measured clockwise from "to top" and the gradient line is long enough for the corners to hit the end stops.
 */
private fun cssLinearGradientPoints(
    angleDegrees: Float,
    width: Float,
    height: Float
): Pair<Offset, Offset> {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val directionX = sin(radians).toFloat()
    val directionY = -cos(radians).toFloat()
    val halfLength = (abs(width * directionX) + abs(height * directionY)) / 2
    val center = Offset(width / 2, height / 2)
    val offset = Offset(directionX * halfLength, directionY * halfLength)
    return center - offset to center + offset
}

private fun sheetBackgroundBrush(): Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to SheetGradientTop,
        SHEET_GRADIENT_MID_STOP to SheetGradientBottom
    )
)

object NpsBottomSheet {

    /** The sheet's shape; the host uses it for the sheet container so both match. */
    val Shape = RoundedCornerShape(
        topStart = ProtonDimens.CornerRadius.ExtraLarge,
        topEnd = ProtonDimens.CornerRadius.ExtraLarge
    )

    data class Actions(
        val onScoreSelected: (NpsScore) -> Unit,
        val onCommentChanged: (String) -> Unit,
        val onShareClicked: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onScoreSelected = {},
                onCommentChanged = {},
                onShareClicked = {}
            )
        }
    }
}

private val CommentFieldHeight = 140.dp
private val ShareButtonHeight = 52.dp
private val ScoreButtonSize = 50.dp
private const val SCORE_BUTTONS_PER_ROW = 6
private val IllustrationSize = 50.dp
private val IllustrationCornerRadius = 21.25.dp
private val HeartIconSize = 25.dp
private const val SHEET_GRADIENT_MID_STOP = 0.3342f
private val SheetGradientTop = Color(0xFFD4D9FB)
private val SheetGradientBottom = Color(0xFFF4F5F8)
private const val ILLUSTRATION_GRADIENT_ANGLE = 222.38f
private const val ILLUSTRATION_GRADIENT_START_STOP = 0.0656f
private const val ILLUSTRATION_GRADIENT_END_STOP = 0.7789f
private val IllustrationGradientStart = Color(0xFFFF98B7)
private val IllustrationGradientEnd = Color(0xFF9698FB)
private val UnselectedScoreBackground = Color(0x140F0F0F)
// Selected score text is the dark-theme Text/Norm value: the sheet has a fixed light background, so the
// selected number needs a light foreground against the brand fill regardless of the app theme.
private val SelectedScoreText = Color(0xFFEDEDEE)

@Preview(widthDp = 412)
@Composable
private fun NpsBottomSheetContentEmptyPreview() {
    ProtonTheme {
        NpsBottomSheetContent(state = NpsState.Initial, actions = NpsBottomSheet.Actions.Empty)
    }
}

@Preview(widthDp = 412)
@Composable
private fun NpsBottomSheetContentSelectedPreview() {
    ProtonTheme {
        NpsBottomSheetContent(
            state = NpsState.Initial.copy(selectedScore = NpsScore(NpsScore.MAX - 1), comment = "Great app"),
            actions = NpsBottomSheet.Actions.Empty
        )
    }
}
