rootProject.name = "ProtonMail"

// Use core libs from maven artifacts or from git submodule
val useCoreGitSubmoduleAsBoolean: Boolean = extensions.extraProperties
    .properties["useCoreGitSubmodule"].toString().toBoolean()
if (useCoreGitSubmoduleAsBoolean) {
    println("Use core libs from git submodule \'./proton-libs\'")
    includeBuild("proton-libs")
} else {
    println("Use core libs from Maven artifacts")
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
