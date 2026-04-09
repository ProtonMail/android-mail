package ch.protonmail.android.e2e.screenplay.ports

import arrow.core.Either
import ch.protonmail.android.e2e.screenplay.Ability
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.Target
import ch.protonmail.android.e2e.screenplay.DEFAULT_WAIT_MS

interface UIDriver : Ability {
    fun click(target: Target): Either<ScreenplayError, Unit>
    fun enterText(target: Target, text: String): Either<ScreenplayError, Unit>
    fun isVisible(target: Target): Boolean
    fun waitUntilVisible(target: Target, timeoutMs: Long = DEFAULT_WAIT_MS): Either<ScreenplayError, Unit>
}
