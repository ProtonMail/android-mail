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

/** Where the user met BYOE. Mirrors the SDK's `ByoeSurface`. */
enum class ByoeSurface {

    /** The BYOE drawer of the app shell during onboarding. No Android call site: there is no such drawer. */
    OnboardingDrawer,

    /** The BYOE page inside the onboarding, and the intro opened from it. */
    OnboardingScreen,
    Spotlight,
    SidebarBanner,
    Settings,

    /** A link from outside the app: `protonmail://connect-gmail/start`, or an account settings web page. */
    DeepLink
}
