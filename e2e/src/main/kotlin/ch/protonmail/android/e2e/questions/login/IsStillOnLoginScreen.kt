package ch.protonmail.android.e2e.questions.login

import arrow.core.Either
import ch.protonmail.android.e2e.screenplay.Actor
import ch.protonmail.android.e2e.screenplay.Answerable
import ch.protonmail.android.e2e.screenplay.MEDIUM_WAIT_MS
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.ports.UIDriver
import ch.protonmail.android.e2e.targets.SignInButton

class IsStillOnLoginScreen : Answerable<Unit> {
    override fun answeredBy(actor: Actor): Either<ScreenplayError, Unit> = actor.abilityTo<UIDriver>()
        .waitUntilVisible(SignInButton, MEDIUM_WAIT_MS)
        // `.mapLeft` transforms the error (Left) side of Either, leaving Right untouched.
        .mapLeft {
            ScreenplayError.AssertionError.ConditionNotMet(
                "Expected to remain on login screen but Sign In button is gone"
            )
        }
}
