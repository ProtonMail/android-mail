/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.model

/**
 * An onboarding page, recorded in the SDK when it appears. Mirrors the SDK's `OnboardingStep` without `Finished`,
 * which is recorded separately after the last page.
 */
enum class ByoeOnboardingStep {
    Upsell,
    Welcome,
    Offline,
    Byoe,

    /** No Android call site: the notification permission is a Home interstitial, not an onboarding page. */
    Notifications
}
