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

package ch.protonmail.android.mailupselling.presentation.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailupselling.domain.model.PlanUpgradeCycle
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.extension.formatted
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingPlanUpgradeUiModel
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.MailPurchaseButton
import ch.protonmail.android.mailupselling.presentation.ui.screen.footer.MailPurchaseButtonVariant

@Composable
internal fun PaidPlanCardContent(
    plan: OnboardingPlanUpgradeUiModel.Paid,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (plan.isBestValue) {
        UpsellingLayoutValues.UpsellCards.outlineUpsellingBorderStoke
    } else {
        null
    }

    Card(
        shape = ProtonTheme.shapes.huge,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .padding(ProtonDimens.Spacing.Large)
            .shadow(
                elevation = ProtonDimens.ShadowElevation.Raised,
                ambientColor = ProtonTheme.colors.shadowMedium,
                spotColor = ProtonTheme.colors.shadowMedium,
                shape = ProtonTheme.shapes.huge
            ),
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(ProtonDimens.Spacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Compact)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Compact),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.planInstance.name,
                    color = ProtonTheme.colors.brandPlus30,
                    style = ProtonTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                if (plan.isBestValue) BestValueBadge()
            }

            PricingSection(plan)

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            ExpandableFeatureList(plan.entitlements)

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))
            MailPurchaseButton(
                plan.planInstance.product,
                variant = MailPurchaseButtonVariant.Inverted,
                onSuccess = { _ -> onDismiss() },
                onErrorMessage = { errorMessage ->
                    onError(errorMessage)
                    onDismiss()
                },
                onCanceled = {}
            )

            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraTiny))
            OnboardingAutoRenewNotice(planUiModel = plan.planInstance)
        }
    }
}

@Composable
private fun BestValueBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = ProtonTheme.colors.brandMinus30,
                shape = UpsellingLayoutValues.UpsellOnboarding.bestValueShape
            )
            .padding(horizontal = ProtonDimens.Spacing.Standard, vertical = ProtonDimens.Spacing.Small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.upselling_onboarding_best_value),
            style = ProtonTheme.typography.labelMedium,
            color = ProtonTheme.colors.brandPlus30
        )
    }
}

@Composable
private fun PricingSection(plan: OnboardingPlanUpgradeUiModel.Paid) {
    val yearlySaving = plan.planInstance.yearlySaving

    Row(verticalAlignment = Alignment.CenterVertically) {
        val cycleText = when (plan.cycle) {
            PlanUpgradeCycle.Monthly -> stringResource(R.string.upselling_month)
            PlanUpgradeCycle.Yearly -> stringResource(R.string.upselling_year)
        }

        Text(
            text = plan.planInstance.primaryPrice.highlightedPrice.getFullFormat(),
            style = ProtonTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = ProtonTheme.colors.textNorm
        )

        Spacer(modifier = Modifier.width(ProtonDimens.Spacing.Compact))

        Text(
            text = cycleText,
            style = ProtonTheme.typography.bodySmall,
            color = ProtonTheme.colors.textWeak
        )
    }

    if (yearlySaving != null) {
        Text(
            text = stringResource(
                R.string.upselling_onboarding_yearly_savings,
                yearlySaving.formatted().string()
            ),
            style = ProtonTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = ProtonTheme.colors.brandNorm
        )
    }
}
