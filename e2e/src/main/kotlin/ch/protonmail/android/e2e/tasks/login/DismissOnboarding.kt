package ch.protonmail.android.e2e.tasks.login

import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.e2e.screenplay.Actor
import ch.protonmail.android.e2e.screenplay.Performable
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.SHORT_WAIT_MS
import ch.protonmail.android.e2e.screenplay.ports.UIDriver
import ch.protonmail.android.e2e.targets.GetStartedButton
import ch.protonmail.android.e2e.targets.NextButton

private const val ONBOARDING_NEXT_PAGES = 2

/**
 * Dismisses the onboarding flow if it appears after first login.
 * The onboarding has 2 "Next" screens followed by a "Get started" screen.
 * If onboarding is not shown (e.g. returning user), this task succeeds silently.
 */
class DismissOnboarding : Performable {

    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> {
        val ui = actor.abilityTo<UIDriver>()

        // Onboarding has 2 pages with a "Next" button
        // `repeat(n) { }` runs the block n times — stdlib alternative to a counting for-loop.
        repeat(ONBOARDING_NEXT_PAGES) {
            // `.fold(ifLeft = ..., ifRight = ...)` pattern-matches on Either's two branches.
            ui.waitUntilVisible(NextButton, SHORT_WAIT_MS).fold(
                ifLeft = { return Unit.right() }, // No onboarding shown — nothing to dismiss
                ifRight = { ui.click(NextButton) }
            )
        }

        // Final page has a "Get started" button
        // `.onRight { }` runs the lambda only if Either is Right (success), otherwise does nothing.
        ui.waitUntilVisible(GetStartedButton, SHORT_WAIT_MS)
            .onRight { ui.click(GetStartedButton) }

        return Unit.right()
    }
}
