/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.presentation.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonDimens.IconSize
import ch.protonmail.android.design.compose.theme.ProtonDimens.Spacing
import ch.protonmail.android.design.compose.theme.ProtonTheme
import me.proton.android.core.payment.domain.model.ProductDetailHeader
import me.proton.android.core.payment.presentation.model.Subscription
import me.proton.core.presentation.R

@Composable
fun ProductHeader(
    header: ProductDetailHeader,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    showPrice: Boolean = true
) {
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    modifier = Modifier.padding(end = Spacing.Small),
                    text = header.title,
                    style = ProtonTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (header.starred) {
                    Icon(
                        modifier = Modifier
                            .size(IconSize.Small)
                            .align(Alignment.CenterVertically),
                        painter = painterResource(R.drawable.ic_proton_star_filled),
                        tint = ProtonTheme.colors.iconAccent,
                        contentDescription = null
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = Spacing.Medium, end = Spacing.Medium),
                text = header.description,
                style = ProtonTheme.typography.bodyMedium,
                color = ProtonTheme.colors.textHint
            )
        }
        Column {
            if (showPrice) {
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = header.priceText,
                    style = ProtonTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.align(Alignment.End),
                    text = header.cycleText,
                    style = ProtonTheme.typography.labelMedium,
                    color = ProtonTheme.colors.textHint
                )
            }
            if (icon != null) {
                Icon(
                    modifier = Modifier
                        .then(if (showPrice) Modifier.padding(top = Spacing.Large) else Modifier)
                        .size(IconSize.Medium)
                        .align(Alignment.End),
                    painter = icon,
                    tint = ProtonTheme.colors.iconAccent,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
private fun ProductHeaderPreview() {
    ProtonTheme {
        ProductHeader(
            header = Subscription.test.header,
            modifier = Modifier.padding(Spacing.Medium)
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
private fun ProductHeaderNoPricePreview() {
    ProtonTheme {
        ProductHeader(
            header = Subscription.test.header,
            modifier = Modifier.padding(Spacing.Medium),
            showPrice = false
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
private fun PlanHeaderIconPreview() {
    ProtonTheme {
        ProductHeader(
            header = Subscription.test.header,
            modifier = Modifier.padding(Spacing.Medium),
            icon = painterResource(R.drawable.ic_proton_chevron_down)
        )
    }
}
