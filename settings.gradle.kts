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

plugins {
    id("me.proton.core.gradle-plugins.include-core-build") version "1.3.0"
}

includeCoreBuild {
    branch.set("main")
    includeBuild("gopenpgp")
}

include(":app")
include(":benchmark")
include(":coverage")
include(":detekt-rules")

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

include(":mail-upselling:dagger")
include(":mail-upselling:data")
include(":mail-upselling:domain")
include(":mail-upselling:presentation")

include(":mail-sidebar:dagger")
include(":mail-sidebar:data")
include(":mail-sidebar:domain")
include(":mail-sidebar:presentation")

include(":mail-bugreport:dagger")
include(":mail-bugreport:data")
include(":mail-bugreport:domain")
include(":mail-bugreport:presentation")

include(":uicomponents")

include(":test:annotations")
include(":test:idlingresources")
include(":test:network-mocks")
include(":test:robot:core")
include(":test:robot:ksp:annotations")
include(":test:robot:ksp:processor")
include(":test:test-data")
include(":test:utils")

buildCache {
    local {
        if (System.getenv("CI") == "true") {
            directory = rootDir.resolve("build").resolve("gradle-build-cache")
        }
        removeUnusedEntriesAfterDays = 3
    }
}
