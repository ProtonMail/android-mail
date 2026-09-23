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

import android.content.res.Configuration
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.component.ShimmerBox
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcontentsearch.presentation.R
import ch.protonmail.android.mailcontentsearch.presentation.settings.model.IndexSizeUiModel

/**
 * What content search occupies on this device, and the way to get it back.
 *
 * The size row is always shown. It shimmers while Rust measures the index.
 * It shows a dash when Rust cannot say.
 */
@Composable
internal fun OnThisDeviceCard(
    indexSize: IndexSizeUiModel,
    onClearDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        IndexSizeRow(indexSize = indexSize)
        MailDivider()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClearDataClick)
                .padding(ProtonDimens.Spacing.Large)
        ) {
            Text(
                text = stringResource(id = R.string.mail_settings_content_search_clear_data_button),
                style = ProtonTheme.typography.bodyLarge,
                color = ProtonTheme.colors.notificationError
            )
        }
    }
}

@Composable
private fun IndexSizeRow(indexSize: IndexSizeUiModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(ProtonDimens.Spacing.Large)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.mail_settings_content_search_index_size),
            style = ProtonTheme.typography.bodyLarge
        )
        when (indexSize) {
            IndexSizeUiModel.Loading -> ShimmerBox(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(width = IndexSizeShimmerWidth, height = IndexSizeShimmerHeight)
            )

            is IndexSizeUiModel.Loaded -> IndexSizeValue(Formatter.formatShortFileSize(context, indexSize.bytes))
            IndexSizeUiModel.Unavailable -> IndexSizeValue(UNAVAILABLE_INDEX_SIZE)
        }
    }
}

@Composable
private fun IndexSizeValue(text: String) {
    Text(
        text = text,
        style = ProtonTheme.typography.bodyLarge,
        color = ProtonTheme.colors.textWeak
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun OnThisDeviceCardPreview() {
    ProtonInvertedTheme {
        OnThisDeviceCard(indexSize = IndexSizeUiModel.Loaded(bytes = 1_288_490_188L), onClearDataClick = {})
    }
}

@Preview(showBackground = true, name = "Measuring")
@Composable
private fun OnThisDeviceCardLoadingPreview() {
    ProtonInvertedTheme {
        OnThisDeviceCard(indexSize = IndexSizeUiModel.Loading, onClearDataClick = {})
    }
}

private val IndexSizeShimmerWidth = 56.dp
private val IndexSizeShimmerHeight = 16.dp
private const val UNAVAILABLE_INDEX_SIZE = "\u2014"
