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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingButtonViewModel

@Composable
fun UpsellBannerButton(
    ctaText: String,
    modifier: Modifier = Modifier,
    onClick: (type: UpsellingVisibility) -> Unit = {}
) {

    val viewModel = hiltViewModel<UpsellingButtonViewModel, UpsellingButtonViewModel.Factory> { factory ->
        factory.create(UpsellingEntryPoint.Feature.AutoDelete)
    }

    val state = viewModel.state.collectAsStateWithLifecycle()
    val type = state.value.visibility

    AnimatedVisibility(
        visible = state.value.isShown,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        when (type) {
            is UpsellingVisibility.Hidden -> Unit
            is UpsellingVisibility.Promotional,
            is UpsellingVisibility.Normal ->
                UpsellBannerButtonContent(modifier = modifier, onClick = { onClick(type) }) {
                    Text(
                        text = ctaText,
                        style = ProtonTheme.typography.bodyMediumNorm,
                        textAlign = TextAlign.Center
                    )
                }
        }
    }
}

@Composable
private fun UpsellBannerButtonContent(
    onClick: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ProtonDimens.Spacing.Massive)
            .border(UpsellingLayoutValues.UpsellCards.outlineBorderStoke, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = ProtonDimens.Spacing.Large, vertical = ProtonDimens.Spacing.Small),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Preview(name = "Light mode", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpsellButtonPreview() {
    ProtonTheme {
        UpsellBannerButton(ctaText = "Upgrade to Auto-delete")
    }
}
