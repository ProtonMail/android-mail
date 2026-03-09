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

package ch.protonmail.android.mailupselling.presentation.ui.screen.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.viewmodel.hiltViewModelOrNull
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.android.core.payment.presentation.R
import me.proton.android.core.payment.presentation.component.PurchaseButtonAction
import me.proton.android.core.payment.presentation.component.PurchaseButtonState
import me.proton.android.core.payment.presentation.component.PurchaseButtonState.Idle
import me.proton.android.core.payment.presentation.component.PurchaseButtonState.Loading
import me.proton.android.core.payment.presentation.component.PurchaseButtonState.Pending
import me.proton.android.core.payment.presentation.component.PurchaseButtonState.Success
import me.proton.android.core.payment.presentation.component.PurchaseButtonViewModel
import me.proton.android.core.payment.presentation.model.Product

// Adaptation of PurchaseButton in me.proton.android.core.payment.presentation.component for Mail.
@Composable
fun MailPurchaseButton(
    product: Product,
    modifier: Modifier = Modifier,
    ctaText: String = stringResource(R.string.payment_purchase_button_get, product.header.title),
    variant: MailPurchaseButtonVariant,
    onSuccess: (Product) -> Unit = {},
    onErrorMessage: (String) -> Unit = {},
    viewModel: PurchaseButtonViewModel? = hiltViewModelOrNull<PurchaseButtonViewModel>(product.productId)
) {
    val state = viewModel?.state?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(state) {
        when (state) {
            is Success -> onSuccess(state.product)
            is PurchaseButtonState.Error -> onErrorMessage(state.message)
            else -> Unit
        }
    }

    LaunchedEffect(product) {
        viewModel?.perform(PurchaseButtonAction.Load(product))
    }

    MailPurchaseButton(
        product = product,
        ctaText = ctaText,
        state = state ?: Idle,
        variant = variant,
        modifier = modifier,
        onClick = { viewModel?.perform(PurchaseButtonAction.Purchase(product)) }
    )
}

@Composable
private fun MailPurchaseButton(
    product: Product,
    ctaText: String,
    state: PurchaseButtonState,
    variant: MailPurchaseButtonVariant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val text = when (state) {
        is Success -> stringResource(R.string.payment_purchase_button_subscribed, product.header.title)
        else -> ctaText
    }

    MailPurchaseButton(
        onClick = onClick,
        loading = state is Pending,
        enabled = state !is Loading && state !is Success && (state as? PurchaseButtonState.Error)?.enabled != false,
        variant = variant,
        modifier = modifier
    ) {
        val textColor = when (variant) {
            MailPurchaseButtonVariant.Default,
            MailPurchaseButtonVariant.BlackFriday -> Color.Black
            MailPurchaseButtonVariant.SpringPromo -> Color.White

            MailPurchaseButtonVariant.Inverted -> ProtonTheme.colors.textInverted
        }

        Column {
            Text(
                text = text,
                style = ProtonTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            (state as? PurchaseButtonState.Error)?.message?.let {
                Text(
                    text = it,
                    style = ProtonTheme.typography.labelSmall,
                    maxLines = 3,
                    color = textColor,
                    modifier = Modifier.padding(top = ProtonDimens.Spacing.MediumLight)
                )
            }
        }
    }
}

@Composable
private fun MailPurchaseButton(
    onClick: () -> Unit,
    loading: Boolean,
    enabled: Boolean,
    variant: MailPurchaseButtonVariant,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when (variant) {
        MailPurchaseButtonVariant.Default -> Color.White
        MailPurchaseButtonVariant.BlackFriday -> UpsellingLayoutValues.BlackFriday.mainColor
        MailPurchaseButtonVariant.SpringPromo,
        MailPurchaseButtonVariant.Inverted -> ProtonTheme.colors.interactionBrandDefaultNorm
    }
    val progressIndicatorColor = when (variant) {
        MailPurchaseButtonVariant.Default,
        MailPurchaseButtonVariant.BlackFriday,
        MailPurchaseButtonVariant.SpringPromo -> Color.Black

        MailPurchaseButtonVariant.Inverted -> Color.White
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ProtonDimens.Spacing.Massive)
            .background(
                color = if (enabled) {
                    if (isPressed) backgroundColor.copy(alpha = 0.95f) else backgroundColor
                } else {
                    backgroundColor.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(ProtonDimens.CornerRadius.Huge)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProtonDimens.IconSize.Default),
                color = progressIndicatorColor
            )
        } else {
            Box { content() }
        }
    }
}

enum class MailPurchaseButtonVariant {
    Default,
    Inverted,
    BlackFriday,
    SpringPromo
}
