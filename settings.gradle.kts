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
