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

package ch.protonmail.android.mailspotlight.data.mapper

import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailspotlight.domain.model.Announcement
import ch.protonmail.android.mailspotlight.domain.model.ByoeEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.ContentSearchEntryPoint
import ch.protonmail.android.mailspotlight.domain.model.FeatureEntryPoint
import uniffi.mail_uniffi.ByoeGuidanceTreatment
import uniffi.mail_uniffi.Feature
import uniffi.mail_uniffi.Announcement as SdkAnnouncement
import uniffi.mail_uniffi.ByoeEntryPoint as SdkByoeEntryPoint
import uniffi.mail_uniffi.ContentSearchEntryPoint as SdkContentSearchEntryPoint

internal fun SdkAnnouncement.toAnnouncement(): Announcement = when (this) {
    SdkAnnouncement.ContentSearchWhatsNew -> Announcement.ContentSearchWhatsNew
    is SdkAnnouncement.ByoeSpotlight -> Announcement.ByoeSpotlight(v1.toTreatment())
}

internal fun Announcement.toSdk(): SdkAnnouncement = when (this) {
    Announcement.ContentSearchWhatsNew -> SdkAnnouncement.ContentSearchWhatsNew
    is Announcement.ByoeSpotlight -> SdkAnnouncement.ByoeSpotlight(treatment.toSdk())
}

internal fun FeatureEntryPoint.toSdk(): Feature = when (this) {
    is FeatureEntryPoint.ContentSearch -> Feature.ContentSearch(entryPoint.toSdk())
    is FeatureEntryPoint.Byoe -> Feature.Byoe(entryPoint.toSdk())
}

private fun ContentSearchEntryPoint.toSdk() = when (this) {
    ContentSearchEntryPoint.SearchBar -> SdkContentSearchEntryPoint.SEARCH_BAR
    ContentSearchEntryPoint.SearchPromo -> SdkContentSearchEntryPoint.SEARCH_PROMO
    ContentSearchEntryPoint.Highlights -> SdkContentSearchEntryPoint.HIGHLIGHTS
    ContentSearchEntryPoint.Settings -> SdkContentSearchEntryPoint.SETTINGS
    ContentSearchEntryPoint.IndexingAlerts -> SdkContentSearchEntryPoint.INDEXING_ALERTS
}

private fun ByoeEntryPoint.toSdk() = when (this) {
    ByoeEntryPoint.OnboardingPage -> SdkByoeEntryPoint.ONBOARDING_PAGE
    ByoeEntryPoint.Spotlight -> SdkByoeEntryPoint.SPOTLIGHT
    ByoeEntryPoint.SidebarBanner -> SdkByoeEntryPoint.SIDEBAR_BANNER
    ByoeEntryPoint.Settings -> SdkByoeEntryPoint.SETTINGS
}

private fun ByoeGuidanceTreatment.toTreatment() = when (this) {
    ByoeGuidanceTreatment.TREATMENT1 -> ByoeTreatment.Treatment1
    ByoeGuidanceTreatment.TREATMENT2 -> ByoeTreatment.Treatment2
}

private fun ByoeTreatment.toSdk() = when (this) {
    ByoeTreatment.Treatment1 -> ByoeGuidanceTreatment.TREATMENT1
    ByoeTreatment.Treatment2 -> ByoeGuidanceTreatment.TREATMENT2
}
