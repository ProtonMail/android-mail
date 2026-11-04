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

package ch.protonmail.android.mailonboarding.presentation.model

/**
 * [showGmailPage] is true for users in a BYOE onboarding treatment, who get the Gmail page in place of the second
 * and third pages.
 */
sealed interface OnboardingState {
    data class NoUpsell(val showGmailPage: Boolean) : OnboardingState
    data class ToUpsell(val showGmailPage: Boolean) : OnboardingState
    data object Loading : OnboardingState
}
