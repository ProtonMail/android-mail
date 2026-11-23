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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ch.protonmail.android.design.compose.component.ExpandableHeader
import ch.protonmail.android.design.compose.theme.ProtonDimens.BorderSize
import ch.protonmail.android.design.compose.theme.ProtonDimens.Spacing
import ch.protonmail.android.design.compose.theme.ProtonTheme
import me.proton.android.core.payment.presentation.model.Product
import me.proton.core.presentation.R

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    expandable: Boolean = true,
    expandedInitially: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = ProtonTheme.colors.backgroundSecondary
    ),
    colorsExpanded: CardColors = CardDefaults.outlinedCardColors(
        containerColor = ProtonTheme.colors.backgroundNorm
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = BorderStroke(
        width = BorderSize.Default,
        color = ProtonTheme.colors.borderNorm
    ),
    borderExpanded: BorderStroke? = BorderStroke(
        width = BorderSize.Default,
        color = ProtonTheme.colors.interactionBrandDefaultNorm
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(expandedInitially) }
    Card(
        onClick = { if (expandable) { expanded = !expanded } },
        modifier = modifier,
        shape = shape,
        colors = if (expanded) colorsExpanded else colors,
        elevation = elevation,
        border = if (expanded) borderExpanded else border
    ) {
        val icon = when {
            !expandable -> null
            !expanded -> painterResource(R.drawable.ic_proton_chevron_down)
            else -> painterResource(R.drawable.ic_proton_chevron_up)
        }
        ExpandableHeader(
            modifier = Modifier.padding(Spacing.Medium),
            expanded = expanded,
            header = {
                ProductHeader(product.header, icon = icon)
            },
            content = {
                EntitlementList(product.entitlements)
                content()
            }
        )
    }
}

@Composable
fun ProductCardContent(modifier: Modifier = Modifier, button: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        button()
    }
}

@Composable
@PreviewLightDark
internal fun ProductCardPreview() {
    ProtonTheme {
        ProductCard(
            product = Product.test,
            modifier = Modifier.padding(Spacing.Medium),
            expandable = true,
            expandedInitially = false
        ) {
            ProductCardContent {
                PurchaseButton(
                    product = Product.test,
                    modifier = Modifier.padding(top = Spacing.Medium).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
internal fun PurchaseCardExpandedPreview() {
    ProtonTheme {
        ProductCard(
            product = Product.test,
            modifier = Modifier.padding(Spacing.Medium),
            expandable = false,
            expandedInitially = true
        ) {
            ProductCardContent {
                PurchaseButton(
                    product = Product.test,
                    modifier = Modifier.padding(top = Spacing.Medium).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
internal fun PurchaseCardDarkPreview() {
    ProtonTheme {
        ProductCard(
            product = Product.test,
            modifier = Modifier.padding(Spacing.Medium),
            expandable = true,
            expandedInitially = false
        ) {
            ProductCardContent {
                PurchaseButton(
                    product = Product.test,
                    modifier = Modifier.padding(top = Spacing.Medium).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
internal fun PurchaseCardExpandedDarkPreview() {
    ProtonTheme {
        ProductCard(
            product = Product.test,
            modifier = Modifier.padding(Spacing.Medium),
            expandable = false,
            expandedInitially = true
        ) {
            ProductCardContent {
                PurchaseButton(
                    product = Product.test,
                    modifier = Modifier.padding(top = Spacing.Medium).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
internal fun PurchaseCardContentPreview() {
    ProtonTheme {
        ProductCardContent {
            PurchaseButton(
                product = Product.test,
                modifier = Modifier.padding(Spacing.Medium).fillMaxWidth()
            )
        }
    }
}
