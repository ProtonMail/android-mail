rootProject.name = "ProtonMail"

// Use core libs from maven artifacts or from git submodule using Gradle's included build:
// - to enable/disable locally: gradle.properties > useCoreGitSubmodule
// - to enable/disable on CI: .gitlab-ci.yml > ORG_GRADLE_PROJECT_useCoreGitSubmodule
val coreSubmoduleDir = rootDir.resolve("proton-libs")
extra.set("coreSubmoduleDir", coreSubmoduleDir)
apply(from = "${coreSubmoduleDir.path}/gradle/include-core-libs.gradle.kts")

include(":app")
include(":test-data")

include(":mail-message:data")
include(":mail-message:domain")
include(":mail-message:presentation")

include(":mail-conversation:data")
include(":mail-conversation:domain")
include(":mail-conversation:presentation")

include(":mail-mailbox:data")
include(":mail-mailbox:domain")
include(":mail-mailbox:presentation")

include(":mail-settings:dagger")
include(":mail-settings:data")
include(":mail-settings:domain")
include(":mail-settings:presentation")
