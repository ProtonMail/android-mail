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

package ch.protonmail.android.mailupselling.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingButtonViewModel

@Composable
fun UpsellingMailButton(modifier: Modifier = Modifier, onClick: (type: UpsellingVisibility) -> Unit) {

    val viewModel = hiltViewModel<UpsellingButtonViewModel, UpsellingButtonViewModel.Factory> { factory ->
        factory.create(UpsellingEntryPoint.Feature.Navbar)
    }

    val state = viewModel.state.collectAsStateWithLifecycle()

    // For now type is not propagated as it will be needed for telemetry purposes in a later story.
    val type = state.value.visibility

    AnimatedVisibility(
        visible = state.value.isShown,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        when (val visibility = state.value.visibility) {
            is UpsellingVisibility.Hidden -> Unit
            is UpsellingVisibility.Promotional.IntroductoryPrice ->
                UpsellingPromotionalMailButton(modifier = modifier, onButtonClick = { onClick(type) })

            is UpsellingVisibility.Promotional.BlackFriday ->
                UpsellingBlackFridayMailButton(visibility, modifier = modifier, onButtonClick = { onClick(type) })

            is UpsellingVisibility.Promotional.SpringPromo ->
                UpsellingSpringPromoMailButton(modifier = modifier, onButtonClick = { onClick(type) })

            is UpsellingVisibility.Normal -> UpsellingMailButton(modifier = modifier, onButtonClick = { onClick(type) })
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpsellingMailButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilityDescription = stringResource(id = R.string.upselling_button_item_content_description)
    Surface(
        modifier = modifier.semantics { contentDescription = accessibilityDescription },
        color = Color.Transparent,
        onClick = dropUnlessResumed { onButtonClick() },
        shape = ProtonTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = ProtonDimens.Spacing.Small)
                .padding(horizontal = ProtonDimens.Spacing.Compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_upselling_mail),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconNorm
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Tiny))
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconNorm
            )
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpsellingBlackFridayMailButton(
    visibility: UpsellingVisibility.Promotional.BlackFriday,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accessibilityDescription = stringResource(id = R.string.upselling_button_item_content_description)
    val (primaryIcon, secondaryIcon) = visibility.getHeaderIcons()

    Surface(
        modifier = modifier
            .semantics { contentDescription = accessibilityDescription },
        color = UpsellingLayoutValues.BlackFriday.mainColor,
        onClick = dropUnlessResumed { onButtonClick() },
        shape = ProtonTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = ProtonDimens.Spacing.Small)
                .padding(horizontal = ProtonDimens.Spacing.Compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = primaryIcon,
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = UpsellingLayoutValues.BlackFriday.upsellingButtonTint
            )
            Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Tiny))
            Icon(
                painter = secondaryIcon,
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = UpsellingLayoutValues.BlackFriday.upsellingButtonTint
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpsellingSpringPromoMailButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilityDescription = stringResource(id = R.string.upselling_button_item_content_description)

    IconButton(
        modifier = modifier,
        onClick = onButtonClick
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_upselling_spring),
            contentDescription = accessibilityDescription,
            tint = null
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun UpsellingPromotionalMailButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilityDescription = stringResource(id = R.string.upselling_button_item_content_description)
    Box {

        Surface(
            modifier = modifier.semantics { contentDescription = accessibilityDescription },
            color = ProtonTheme.colors.brandMinus40,
            onClick = dropUnlessResumed { onButtonClick() },
            border = null,
            shape = ProtonTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = ProtonDimens.Spacing.Small)
                    .padding(horizontal = ProtonDimens.Spacing.Compact),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_upselling_mail),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = ProtonTheme.colors.brandPlus30
                )
                Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Tiny))
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = NO_CONTENT_DESCRIPTION,
                    tint = ProtonTheme.colors.brandPlus30
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = ProtonDimens.Spacing.Tiny + ProtonDimens.Spacing.ExtraTiny, y = ProtonDimens.Spacing.Small)
        ) {
            Box(
                modifier = Modifier
                    .size(ProtonDimens.Spacing.ModeratelyLarge)
                    .background(color = ProtonTheme.colors.backgroundNorm, shape = CircleShape)
            )


            Box(
                modifier = Modifier
                    .size(ProtonDimens.Spacing.Compact)
                    .align(Alignment.Center)
                    .background(brush = UpsellingLayoutValues.UpsellingPromoButton.backgroundGradient, CircleShape)
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingMailButtonPreview() {
    ProtonTheme {
        UpsellingMailButton(onButtonClick = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingBFMailButtonWave1Preview() {
    ProtonTheme {
        UpsellingBlackFridayMailButton(
            visibility = UpsellingVisibility.Promotional.BlackFriday.Wave1,
            onButtonClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingBFMailButtonWave2Preview() {
    ProtonTheme {
        UpsellingBlackFridayMailButton(
            visibility = UpsellingVisibility.Promotional.BlackFriday.Wave2,
            onButtonClick = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingSpringPromoMailButtonPreview() {
    ProtonTheme {
        UpsellingSpringPromoMailButton(onButtonClick = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, backgroundColor = 0xFF000000)
@Composable
fun UpsellingMailButtonPreview_Promo() {
    ProtonTheme {
        UpsellingPromotionalMailButton(onButtonClick = {})
    }
}
