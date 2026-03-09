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

package ch.protonmail.android.mailtrackingprotection.data.wrapper

import uniffi.mail_uniffi.PrivacyInfo
import uniffi.mail_uniffi.StrippedUtmInfo
import uniffi.mail_uniffi.TrackerInfo
import uniffi.mail_uniffi.TrackerInfoWithStatus

data class PrivacyInfoWrapper(
    val trackerInfo: TrackerInfo,
    val strippedUtmInfo: StrippedUtmInfo
) {

    companion object {

        internal fun fromPrivacyInfo(rawPrivacyInfo: PrivacyInfo): PrivacyInfoState {
            return when (val trackers = rawPrivacyInfo.trackers) {
                is TrackerInfoWithStatus.Disabled -> PrivacyInfoState.Disabled
                is TrackerInfoWithStatus.Pending -> PrivacyInfoState.Pending
                is TrackerInfoWithStatus.Detected -> {
                    val utmLinks = rawPrivacyInfo.utmLinks
                        ?: return PrivacyInfoState.Pending
                    PrivacyInfoState.Detected(PrivacyInfoWrapper(trackers.v1, utmLinks))
                }
            }
        }
    }
}
