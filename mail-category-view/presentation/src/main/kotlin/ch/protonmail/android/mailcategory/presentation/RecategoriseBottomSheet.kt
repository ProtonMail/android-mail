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

package ch.protonmail.android.mailcategory.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.component.ProtonTextButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeWeak
import ch.protonmail.android.design.compose.theme.titleLargeNorm
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecategoriseBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ProtonModalBottomSheetLayout(
        showBottomSheet = true,
        sheetState = sheetState,
        onDismissed = onDismiss,
        dismissOnBack = true,
        sheetContent = {
            RecategoriseBottomSheetContent(
                onGotIt = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            )
        },
        content = {}
    )
}

@Composable
private fun RecategoriseBottomSheetContent(onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        LandscapeContent(onGotIt = onGotIt, modifier = modifier)
    } else {
        PortraitContent(onGotIt = onGotIt, modifier = modifier)
    }
}

@Composable
private fun PortraitContent(onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge)
            .padding(bottom = ProtonDimens.Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ExtraLarge)
    ) {
        TitleText(modifier = Modifier.padding(vertical = ProtonDimens.Spacing.Standard))
        RecategoriseAnimation(modifier = Modifier.fillMaxWidth())
        DescriptionText()
        GotItButton(onClick = onGotIt, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LandscapeContent(onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(horizontal = ProtonDimens.Spacing.ExtraLarge)
            .padding(bottom = ProtonDimens.Spacing.ExtraLarge)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ExtraLarge)
    ) {
        RecategoriseAnimation(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.Start
        ) {
            // The title starts 16dp below the top edge of the animation.
            TitleText(
                modifier = Modifier.padding(top = ProtonDimens.Spacing.Large),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.MediumLight))
            DescriptionText(textAlign = TextAlign.Start)
            // Pins the button to the bottom so it sits level with the bottom of the animation.
            Spacer(modifier = Modifier.weight(1f))
            GotItButton(onClick = onGotIt, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TitleText(modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.recategorise_bottom_sheet_title),
        style = ProtonTheme.typography.titleLargeNorm,
        textAlign = textAlign
    )
}

@Composable
private fun DescriptionText(modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.recategorise_bottom_sheet_description),
        style = ProtonTheme.typography.bodyLargeWeak,
        textAlign = textAlign
    )
}

@Composable
private fun RecategoriseAnimation(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.recategorise_email))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier.aspectRatio(ANIMATION_ASPECT_RATIO)
    )
}

@Composable
private fun GotItButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ProtonTextButton(
        modifier = modifier.background(
            color = ProtonTheme.colors.brandNorm,
            shape = ProtonTheme.shapes.massive
        ),
        onClick = onClick,
        contentPadding = ButtonContentPadding
    ) {
        Text(
            text = stringResource(id = R.string.recategorise_bottom_sheet_button),
            style = ProtonTheme.typography.bodyLarge,
            color = ProtonTheme.colors.textInverted,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Matches the 364x302.3 animation frame in the Figma spec; the asset keeps this ratio on all devices.
private const val ANIMATION_ASPECT_RATIO = 364f / 302.305f

// Spec: 52dp-high button — ModeratelyLarge vertical padding around the bodyLarge line height.
private val ButtonContentPadding = PaddingValues(
    horizontal = ProtonDimens.Spacing.Standard,
    vertical = ProtonDimens.Spacing.ModeratelyLarge
)

@Preview(name = "Portrait", showBackground = true)
@Composable
private fun RecategorisePortraitPreview() {
    ProtonTheme {
        PortraitContent(onGotIt = {})
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun RecategoriseLandscapePreview() {
    ProtonTheme {
        LandscapeContent(onGotIt = {})
    }
}
