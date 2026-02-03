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

rootProject.name = "ProtonMail"

include(":shared:core:account-manager:dagger")
include(":shared:core:account-manager:data")
include(":shared:core:account-manager:domain")
include(":shared:core:account-manager:presentation")
include(":shared:core:account:dagger")
include(":shared:core:account:data")
include(":shared:core:account:domain")
include(":shared:core:auth:dagger")
include(":shared:core:auth:data")
include(":shared:core:auth:presentation")
include(":shared:core:account-recovery:presentation")
include(":shared:core:device-migration:dagger")
include(":shared:core:device-migration:presentation")
include(":shared:core:humanverification:dagger")
include(":shared:core:humanverification:domain")
include(":shared:core:humanverification:presentation")
include(":shared:core:payment:dagger")
include(":shared:core:payment:data")
include(":shared:core:payment:domain")
include(":shared:core:payment:presentation")
include(":shared:core:payment-google:dagger")
include(":shared:core:payment-google:data")
include(":shared:core:payment-google:domain")
include(":shared:core:payment-google:presentation")

includeBuild("build-plugin")
include(":app")
include(":benchmark")
include(":coverage")
include(":detekt-rules")

include(":mail-attachments:dagger")
include(":mail-attachments:data")
include(":mail-attachments:domain")
include(":mail-attachments:presentation")

include(":mail-tracking-protection:dagger")
include(":mail-tracking-protection:data")
include(":mail-tracking-protection:domain")
include(":mail-tracking-protection:presentation")

include(":mail-bugreport:dagger")
include(":mail-bugreport:data")
include(":mail-bugreport:domain")
include(":mail-bugreport:presentation")

include(":mail-common:dagger")
include(":mail-common:data")
include(":mail-common:domain")
include(":mail-common:presentation")

include(":mail-composer:dagger")
include(":mail-composer:data")
include(":mail-composer:domain")
include(":mail-composer:presentation")

include(":mail-contact:dagger")
include(":mail-contact:data")
include(":mail-contact:domain")
include(":mail-contact:presentation")

include(":mail-pagination:dagger")
include(":mail-pagination:data")
include(":mail-pagination:domain")
include(":mail-pagination:presentation")

include(":mail-message:dagger")
include(":mail-message:data")
include(":mail-message:domain")
include(":mail-message:presentation")

include(":mail-conversation:dagger")
include(":mail-conversation:data")
include(":mail-conversation:domain")
include(":mail-conversation:presentation")

include(":mail-detail:dagger")
include(":mail-detail:data")
include(":mail-detail:domain")
include(":mail-detail:presentation")

include(":mail-events:dagger")
include(":mail-events:data")
include(":mail-events:domain")
include(":mail-events:presentation")

include(":mail-featureflags:dagger")
include(":mail-featureflags:data")
include(":mail-featureflags:domain")
include(":mail-featureflags:presentation")

include(":mail-label:dagger")
include(":mail-label:data")
include(":mail-label:domain")
include(":mail-label:presentation")

include(":mail-mailbox:dagger")
include(":mail-mailbox:data")
include(":mail-mailbox:domain")
include(":mail-mailbox:presentation")

include(":mail-onboarding:dagger")
include(":mail-onboarding:data")
include(":mail-onboarding:domain")
include(":mail-onboarding:presentation")

include(":mail-notifications")

include(":mail-settings:dagger")
include(":mail-settings:data")
include(":mail-settings:domain")
include(":mail-settings:presentation")

include(":mail-pin-lock:dagger")
include(":mail-pin-lock:domain")
include(":mail-pin-lock:data")
include(":mail-pin-lock:presentation")

include(":mail-padlocks:dagger")
include(":mail-padlocks:data")
include(":mail-padlocks:domain")
include(":mail-padlocks:presentation")

include(":mail-sidebar:dagger")
include(":mail-sidebar:data")
include(":mail-sidebar:domain")
include(":mail-sidebar:presentation")

include(":mail-session:dagger")
include(":mail-session:data")
include(":mail-session:domain")
include(":mail-session:presentation")

include(":mail-spotlight:dagger")
include(":mail-spotlight:data")
include(":mail-spotlight:domain")
include(":mail-spotlight:presentation")

include(":mail-upselling:dagger")
include(":mail-upselling:data")
include(":mail-upselling:domain")
include(":mail-upselling:presentation")

include(":mail-snooze:dagger")
include(":mail-snooze:data")
include(":mail-snooze:domain")
include(":mail-snooze:presentation")

include(":mail-legacy-migration:dagger")
include(":mail-legacy-migration:data")
include(":mail-legacy-migration:domain")
include(":mail-legacy-migration:presentation")

include(":mail-crash-record:dagger")
include(":mail-crash-record:data")
include(":mail-crash-record:domain")

include(":uicomponents")
include(":design-system")
include(":presentation-compose")

include(":test:annotations")
include(":test:network-mocks")
include(":test:robot:core")
include(":test:robot:ksp:annotations")
include(":test:robot:ksp:processor")
include(":test:test-data")
include(":test:utils")

buildCache {
    val remoteCacheUrl = providers.environmentVariable("GRADLE_REMOTE_CACHE_URL").orNull
        ?: providers.gradleProperty("remoteCacheUrl").orNull
        ?: ""

    if (remoteCacheUrl.isNotEmpty()) {
        remote<HttpBuildCache> {
            url = uri(remoteCacheUrl)
            isPush = providers.environmentVariable("CI_PIPELINE_SOURCE").orNull == "push"
            credentials {
                username = providers.environmentVariable("GRADLE_REMOTE_CACHE_USERNAME").orNull
                    ?: providers.gradleProperty("remoteCacheUsername").orNull
                        ?: ""
                password = providers.environmentVariable("GRADLE_REMOTE_CACHE_PASSWORD").orNull
                    ?: providers.gradleProperty("remoteCachePassword").orNull
                        ?: ""
            }
        }
    }
}
