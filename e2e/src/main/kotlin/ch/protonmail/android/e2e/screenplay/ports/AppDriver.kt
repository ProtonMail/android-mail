package ch.protonmail.android.e2e.screenplay.ports

import arrow.core.Either
import ch.protonmail.android.e2e.screenplay.Ability
import ch.protonmail.android.e2e.screenplay.ScreenplayError

interface AppDriver : Ability {
    fun launch(): Either<ScreenplayError.ActivityError, Unit>
    fun stop(): Either<ScreenplayError.ActivityError, Unit>
}
