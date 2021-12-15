rootProject.name = "ProtonMail"

// All Core modules are taken/overridden from the local Git submodule proton-libs.
// Comment the includeBuild line below to switch to Core modules published artefacts.
val coreCommitSha: String? = System.getenv("CORE_COMMIT_SHA")
if (coreCommitSha?.isNotBlank() == true) {
    println("Use core libs as included build with commit ref \'$coreCommitSha\'")
    includeBuild("proton-libs")
} else {
    println("Use core libs binaries from Maven")
}

include(":app")
include(":mail-message:data")
include(":mail-message:domain")
include(":mail-message:presentation")

include(":mail-conversation:data")
include(":mail-conversation:domain")
include(":mail-conversation:presentation")

include(":mail-mailbox:data")
include(":mail-mailbox:domain")
include(":mail-mailbox:presentation")
