package ch.protonmail.android.e2e.tasks.login

import arrow.core.Either
import ch.protonmail.android.e2e.screenplay.Actor
import ch.protonmail.android.e2e.screenplay.Performable
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.ports.AppDriver

class LaunchApp : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> = actor.abilityTo<AppDriver>().launch()
}
