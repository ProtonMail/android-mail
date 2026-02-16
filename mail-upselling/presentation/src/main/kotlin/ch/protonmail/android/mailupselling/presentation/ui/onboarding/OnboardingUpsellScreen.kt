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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonHorizontallyCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingPlanUpgradeUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingPlanUpgradesListUiModel
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import ch.protonmail.android.mailupselling.presentation.viewmodel.OnboardingUpsellViewModel
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import ch.protonmail.android.uicomponents.bottomsheet.BottomSheetAnimatedContent

@Composable
fun OnboardingUpsellScreen(onDismiss: () -> Unit, onError: (String) -> Unit) {
    val viewModel = hiltViewModel<OnboardingUpsellViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle dismiss states with LaunchedEffect to ensure single execution
    LaunchedEffect(state) {
        when (state) {
            is OnboardingUpsellState.Error,
            is OnboardingUpsellState.UnsupportedFlow -> onDismiss()

            is OnboardingUpsellState.Data,
            is OnboardingUpsellState.Loading -> Unit
        }
    }

    BottomSheetAnimatedContent(
        state = state,
        loadingState = OnboardingUpsellState.Loading,
        errorStates = setOf(OnboardingUpsellState.Error, OnboardingUpsellState.UnsupportedFlow),
        animationKey = { state is OnboardingUpsellState.Data }
    ) { currentState ->
        when (currentState) {
            is OnboardingUpsellState.Data -> OnboardingUpsellScreen(
                state = currentState,
                onDismiss = onDismiss,
                onError = onError
            )

            is OnboardingUpsellState.Loading -> OnboardingUpsellLoadingScreen(onDismiss)

            is OnboardingUpsellState.Error,
            is OnboardingUpsellState.UnsupportedFlow -> {
                // Handle dismissal via LaunchedEffect, as the animated content may recompose
                // and call onDismiss() multiple times if placed here.
            }
        }
    }
}

@Composable
private fun OnboardingUpsellLoadingScreen(onDismiss: () -> Unit) {
    Column {
        Box {
            IconButton(
                modifier = Modifier
                    .padding(ProtonDimens.Spacing.Tiny)
                    .align(alignment = Alignment.TopEnd)
                    .zIndex(1f),
                onClick = onDismiss
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(UpsellingLayoutValues.closeButtonSize)
                        .background(
                            color = if (isSystemInDarkTheme()) {
                                UpsellingLayoutValues.closeButtonBackgroundColor
                            } else {
                                UpsellingLayoutValues.closeButtonLightBackgroundColor
                            },
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.upselling_close_button_content_description)
                    )
                }
            }
        }

        ProtonHorizontallyCenteredProgress(
            modifier = Modifier.padding(vertical = ProtonDimens.Spacing.Large)
        )
        BottomNavigationBarSpacer()
    }
}

@Composable
private fun OnboardingUpsellScreen(
    state: OnboardingUpsellState.Data,
    onError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(YEARLY_TAB_INDEX) }

    val plans = when (selectedTabIndex) {
        YEARLY_TAB_INDEX -> state.planUiModels.yearlyPlans
        MONTHLY_TAB_INDEX -> state.planUiModels.monthlyPlans
        else -> return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProtonTheme.colors.backgroundInvertedNorm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ProtonDimens.Spacing.Large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier,
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = stringResource(R.string.upselling_close_button_content_description)
                )
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(),
                text = stringResource(R.string.upselling_onboarding_choose_plan),
                style = ProtonTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.padding(end = ProtonDimens.Spacing.Large))
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = ProtonTheme.colors.backgroundInvertedNorm,
            contentColor = ProtonTheme.colors.backgroundInvertedNorm,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(selectedTabIndex)
                        .padding(bottom = UpsellingLayoutValues.UpsellOnboarding.tabIndicatorHeightOffset),
                    color = ProtonTheme.colors.brandPlus20,
                    shape = RoundedCornerShape(
                        topStart = ProtonDimens.Spacing.Standard,
                        topEnd = ProtonDimens.Spacing.Standard
                    ),
                    width = UpsellingLayoutValues.UpsellOnboarding.tabIndicatorWidth,
                    height = UpsellingLayoutValues.UpsellOnboarding.tabIndicatorHeight
                )
            },
            divider = { HorizontalDivider(color = ProtonTheme.colors.borderNorm) }
        ) {
            Tab(
                selected = selectedTabIndex == YEARLY_TAB_INDEX,
                onClick = { selectedTabIndex = YEARLY_TAB_INDEX },
                text = {
                    Text(
                        text = stringResource(R.string.upselling_onboarding_choose_plan_yearly),
                        color = ProtonTheme.colors.brandPlus20,
                        style = ProtonTheme.typography.titleSmall
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == MONTHLY_TAB_INDEX,
                onClick = { selectedTabIndex = MONTHLY_TAB_INDEX },
                text = {
                    Text(
                        text = stringResource(R.string.upselling_onboarding_choose_plan_monthly),
                        color = ProtonTheme.colors.brandPlus20,
                        style = ProtonTheme.typography.titleSmall
                    )
                }
            )
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            plans.forEach { plan ->
                when (plan) {
                    is OnboardingPlanUpgradeUiModel.Free -> FreePlanCard(plan, onDismiss)
                    is OnboardingPlanUpgradeUiModel.Paid -> PaidPlanCardContent(
                        plan, onDismiss, onError
                    )
                }
            }

            BottomNavigationBarSpacer()
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
fun OnboardingUpsellScreenPreview() {
    ProtonTheme {
        OnboardingUpsellScreen(
            OnboardingUpsellState.Data(
                OnboardingPlanUpgradesListUiModel(
                    monthlyPlans = listOf(
                        OnboardingUpsellPreviewData.mailPlusMonthly,
                        OnboardingUpsellPreviewData.unlimitedMonthly,
                        OnboardingUpsellPreviewData.freePlan
                    ),
                    yearlyPlans = listOf(
                        OnboardingUpsellPreviewData.mailPlusYearly,
                        OnboardingUpsellPreviewData.unlimitedYearly,
                        OnboardingUpsellPreviewData.freePlan
                    )
                )
            ),
            onDismiss = {},
            onError = {}
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
fun OnboardingUpsellLoadingScreenPreview() {
    ProtonTheme {
        OnboardingUpsellLoadingScreen { }
    }
}

private const val YEARLY_TAB_INDEX = 0
private const val MONTHLY_TAB_INDEX = 1
