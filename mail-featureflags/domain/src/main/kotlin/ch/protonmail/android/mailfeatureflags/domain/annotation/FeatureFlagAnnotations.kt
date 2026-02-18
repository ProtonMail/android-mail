/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailfeatureflags.domain.annotation

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsUpsellEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsOnboardingUpsellEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsBlackFridayWave1Enabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsBlackFridayWave2Enabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsSpringOffer2026Enabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsSpringOffer2026Wave2Enabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ComposerAutoCollapseQuotedTextEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsDebugInspectDbEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsLastMessageAutoExpandEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsInjectCssOverrideEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsRestrictMessageWebViewHeightEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsShowRatingBoosterEnabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsPrivacyBundle2601Enabled

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsFeatureSpotlightEnabled
