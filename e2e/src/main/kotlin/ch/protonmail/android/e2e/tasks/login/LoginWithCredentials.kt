package ch.protonmail.android.e2e.tasks.login

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.e2e.screenplay.Actor
import ch.protonmail.android.e2e.screenplay.DEFAULT_WAIT_MS
import ch.protonmail.android.e2e.screenplay.MEDIUM_WAIT_MS
import ch.protonmail.android.e2e.screenplay.Performable
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.ports.UIDriver
import ch.protonmail.android.e2e.targets.AccountSelectionSignIn
import ch.protonmail.android.e2e.targets.PasswordField
import ch.protonmail.android.e2e.targets.SignInButton
import ch.protonmail.android.e2e.targets.UsernameField

class NavigateToLoginForm : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> {
        val ui = actor.abilityTo<UIDriver>()
        ui.waitUntilVisible(AccountSelectionSignIn, MEDIUM_WAIT_MS)
            .onLeft { return it.left() }
        return ui.click(AccountSelectionSignIn)
    }
}

class EnterUsername(private val text: String) : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> {
        val ui = actor.abilityTo<UIDriver>()
        ui.waitUntilVisible(UsernameField, DEFAULT_WAIT_MS)
            .onLeft { return it.left() }
        return ui.enterText(UsernameField, text)
    }
}

class EnterPassword(private val text: String) : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> =
        actor.abilityTo<UIDriver>().enterText(PasswordField, text)
}

class TapSignIn : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> =
        actor.abilityTo<UIDriver>().click(SignInButton)
}

class LoginWithCredentials(
    private val username: String,
    private val password: String
) : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> = actor.attemptsTo(
        NavigateToLoginForm(),
        EnterUsername(username),
        EnterPassword(password),
        TapSignIn()
    )
}

// `Performable { actor -> ... }` is SAM conversion — the lambda becomes a Performable instance.
// This works because Performable is a `fun interface` (single abstract method).
fun loginWithActorCredentials() = Performable { actor ->
    LoginWithCredentials(actor.credentials.username, actor.credentials.password)
        .performAs(actor)
}

fun loginWithWrongPassword() = Performable { actor ->
    LoginWithCredentials(actor.credentials.username, "definitely-wrong-password")
        .performAs(actor)
}
