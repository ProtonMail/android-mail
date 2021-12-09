rootProject.name = "ProtonMail"

// All Core modules are taken/overridden from the local Git submodule proton-libs.
// Comment the includeBuild line below to switch to Core modules published artefacts.
// includeBuild("proton-libs")

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

include(":compose")
