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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ch.protonmail.android.design.compose.theme.ProtonDimens.Spacing
import ch.protonmail.android.design.compose.theme.ProtonTheme
import me.proton.android.core.payment.presentation.model.Product

@Composable
fun ProductItem(
    product: Product,
    modifier: Modifier = Modifier,
    onSuccess: (Product) -> Unit = {},
    onErrorMessage: (String) -> Unit = {},
    expandable: Boolean = true,
    expandedInitially: Boolean = true
) {
    ProductCard(
        product = product,
        modifier = modifier,
        expandable = expandable,
        expandedInitially = expandedInitially
    ) {
        ProductCardContent {
            PurchaseButton(
                product = product,
                modifier = Modifier.padding(top = Spacing.Medium).fillMaxWidth(),
                onSuccess = onSuccess,
                onErrorMessage = onErrorMessage
            )
        }
    }
}

@Composable
@PreviewLightDark
internal fun ProductItemPreview() {
    ProtonTheme {
        ProductItem(
            product = Product.test,
            modifier = Modifier.padding(Spacing.Medium)
        )
    }
}
