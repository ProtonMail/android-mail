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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonMainSettingsItem
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.viewmodel.hiltViewModelOrNull
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import ch.protonmail.android.mailupselling.presentation.viewmodel.PlusToUnlimitedUpsellViewModel

@Composable
fun PlusToUnlimitedUpsellItem(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val viewModel = hiltViewModelOrNull<PlusToUnlimitedUpsellViewModel>() ?: return
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    PlusToUnlimitedUpsellItem(
        theme = theme ?: return,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun PlusToUnlimitedUpsellItem(
    theme: UpsellContentTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        ProtonMainSettingsItem(
            name = stringResource(id = theme.rowTitleRes),
            hint = {
                Text(
                    modifier = Modifier.padding(top = ProtonDimens.Spacing.Small),
                    text = stringResource(id = theme.rowSubtitleRes),
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.bodyMedium
                )
            },
            icon = {
                Image(
                    modifier = Modifier.size(ProtonDimens.IconSize.ExtraLarge),
                    painter = painterResource(id = R.drawable.illustration_pu_settings_item),
                    contentDescription = null
                )
            },
            onClick = onClick
        )
    }
}

internal class UpsellContentThemePreviewProvider : PreviewParameterProvider<UpsellContentTheme> {
    override val values: Sequence<UpsellContentTheme>
        get() = UpsellContentTheme.values().asSequence()
}

@Preview(name = "Plus to Unlimited upsell item", showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "Plus to Unlimited upsell item - dark", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PlusToUnlimitedUpsellItemPreview(
    @PreviewParameter(UpsellContentThemePreviewProvider::class) theme: UpsellContentTheme
) {
    ProtonInvertedTheme {
        PlusToUnlimitedUpsellItem(
            theme = theme,
            modifier = Modifier.padding(ProtonDimens.Spacing.Large),
            onClick = {}
        )
    }
}
