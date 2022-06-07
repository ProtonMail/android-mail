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

// Use core libs from maven artifacts or from git submodule using Gradle's included build:
// - to enable/disable locally: gradle.properties > useCoreGitSubmodule
// - to enable/disable on CI: .gitlab-ci.yml > ORG_GRADLE_PROJECT_useCoreGitSubmodule
val coreSubmoduleDir = rootDir.resolve("proton-libs")
extra.set("coreSubmoduleDir", coreSubmoduleDir)
val includeCoreLibsHelper = File(coreSubmoduleDir, "gradle/include-core-libs.gradle.kts")
if (includeCoreLibsHelper.exists()) {
    apply(from = "${coreSubmoduleDir.path}/gradle/include-core-libs.gradle.kts")
} else if (extensions.extraProperties["useCoreGitSubmodule"].toString().toBoolean()) {
    includeBuild("proton-libs")
    println("Core libs from git submodule `$coreSubmoduleDir`")
}

include(":app")
include(":test-data")

include(":mail-common:dagger")
include(":mail-common:data")
include(":mail-common:domain")
include(":mail-common:presentation")

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

include(":mail-label:dagger")
include(":mail-label:data")
include(":mail-label:domain")
include(":mail-label:presentation")

include(":mail-mailbox:data")
include(":mail-mailbox:domain")
include(":mail-mailbox:presentation")

include(":mail-settings:dagger")
include(":mail-settings:data")
include(":mail-settings:domain")
include(":mail-settings:presentation")

buildCache {
    local {
        if (System.getenv("CI") == "true") {
            directory = rootDir.resolve("build").resolve("gradle-build-cache")
        }
        removeUnusedEntriesAfterDays = 3
    }
}
