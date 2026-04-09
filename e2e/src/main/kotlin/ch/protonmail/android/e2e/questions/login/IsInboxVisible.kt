package ch.protonmail.android.e2e.questions.login

import arrow.core.Either
import ch.protonmail.android.e2e.screenplay.Actor
import ch.protonmail.android.e2e.screenplay.Answerable
import ch.protonmail.android.e2e.screenplay.LONG_WAIT_MS
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.ports.UIDriver
import ch.protonmail.android.e2e.targets.InboxLabel

class IsInboxVisible(private val timeoutMs: Long = LONG_WAIT_MS) : Answerable<Unit> {
    override fun answeredBy(actor: Actor): Either<ScreenplayError, Unit> = actor.abilityTo<UIDriver>()
        .waitUntilVisible(InboxLabel, timeoutMs)
        .mapLeft { ScreenplayError.AssertionError.ConditionNotMet("Inbox not visible after ${timeoutMs}ms") }
}
