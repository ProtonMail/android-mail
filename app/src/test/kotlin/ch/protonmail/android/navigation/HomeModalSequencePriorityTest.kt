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

package ch.protonmail.android.navigation

import ch.protonmail.android.feature.spotlight.RecategoriseSpotlightState
import ch.protonmail.android.mailcontentsearch.presentation.bottomsheet.ContentSearchBottomSheetState
import ch.protonmail.android.mailnotifications.presentation.model.NotificationsPermissionState
import ch.protonmail.android.mailnotifications.presentation.model.NotificationsPermissionStateType
import ch.protonmail.android.mailonboarding.domain.model.OnboardingEligibilityState
import ch.protonmail.android.mailspotlight.presentation.model.FeatureSpotlightState
import ch.protonmail.android.mailspotlight.presentation.model.SpotlightUserType
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.model.blackfriday.BlackFridayModalState
import ch.protonmail.android.mailupselling.presentation.model.springsale.SpringPromoModalState
import ch.protonmail.android.mailupselling.presentation.model.summercampaign.SummerCampaignModalState
import kotlin.test.Test
import kotlin.test.assertEquals

internal class HomeModalSequencePriorityTest {

    @Test
    fun `resolves to loading when the content search bottom sheet state is still loading`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Loading,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Hide
        )

        // Then
        assertEquals(HomeInterstitialPriority.Loading, result)
    }

    @Test
    fun `resolves to content search when its bottom sheet should show and nothing else takes priority`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Show,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Hide
        )

        // Then
        assertEquals(HomeInterstitialPriority.ContentSearch, result)
    }

    @Test
    fun `resolves to notifications permissions over the content search bottom sheet`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.RequiresInteraction(
                NotificationsPermissionStateType.FirstTime
            ),
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Show,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Hide
        )

        // Then
        assertEquals(
            HomeInterstitialPriority.NotificationsPermissions(NotificationsPermissionStateType.FirstTime),
            result
        )
    }

    @Test
    fun `resolves to feature spotlight over the content search bottom sheet`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Show(SpotlightUserType.B2C),
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Show,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Hide
        )

        // Then
        assertEquals(HomeInterstitialPriority.FeatureSpotlight(SpotlightUserType.B2C), result)
    }

    @Test
    fun `resolves to recategorise over the content search bottom sheet`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Show,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Show
        )

        // Then
        assertEquals(HomeInterstitialPriority.Recategorise, result)
    }

    @Test
    fun `resolves to loading when the recategorise state is still loading`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Hide,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Loading
        )

        // Then
        assertEquals(HomeInterstitialPriority.Loading, result)
    }

    @Test
    fun `resolves to content search over the summer campaign promo`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Show,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.Show(UpsellingVisibility.Promotional.SummerCampaign.Wave1),
            recategoriseState = RecategoriseSpotlightState.Hide
        )

        // Then
        assertEquals(HomeInterstitialPriority.ContentSearch, result)
    }

    @Test
    fun `resolves to none when the content search bottom sheet should not show and nothing else applies`() {
        // When
        val result = resolveHomeInterstitialPriority(
            onboardingState = OnboardingEligibilityState.NotRequired,
            notificationsState = NotificationsPermissionState.Granted,
            featureSpotlightState = FeatureSpotlightState.Hide,
            contentSearchBottomSheetState = ContentSearchBottomSheetState.Hide,
            blackFridayState = BlackFridayModalState.NotRequired,
            springSaleState = SpringPromoModalState.NotRequired,
            summerCampaignState = SummerCampaignModalState.NotRequired,
            recategoriseState = RecategoriseSpotlightState.Hide
        )

        // Then
        assertEquals(HomeInterstitialPriority.None, result)
    }
}
