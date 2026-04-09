package ch.protonmail.android.e2e.adapters

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.ScreenplayError.ElementError
import ch.protonmail.android.e2e.screenplay.ScreenplayError.InteractionError
import ch.protonmail.android.e2e.screenplay.Target
import ch.protonmail.android.e2e.screenplay.ports.UIDriver

class ComposeUIDriver(private val rule: ComposeTestRule) : UIDriver {

    override fun click(target: Target): Either<ScreenplayError, Unit> = runCatching {
        // `and` here is an infix function on SemanticsMatcher that combines two matchers with AND logic.
        rule.onNode(matcherFor(target) and hasClickAction()).performClick()
        rule.waitForIdle()
    }.fold(
        onSuccess = { Unit.right() },
        onFailure = { InteractionError.ClickFailed(target.description, it.message ?: "unknown").left() }
    )

    override fun enterText(target: Target, text: String): Either<ScreenplayError, Unit> = runCatching {
        val matcher = matcherFor(target) and hasSetTextAction()
        rule.onNode(matcher).performTextClearance()
        rule.onNode(matcher).performTextInput(text)
        rule.waitForIdle()
    }.fold(
        onSuccess = { Unit.right() },
        onFailure = { InteractionError.InputFailed(target.description, it.message ?: "unknown").left() }
    )

    override fun isVisible(target: Target): Boolean = runCatching {
        rule.onAllNodes(matcherFor(target)).assertAny(matcherFor(target))
    }.isSuccess

    override fun waitUntilVisible(target: Target, timeoutMs: Long): Either<ScreenplayError, Unit> = runCatching {
        rule.waitUntil(timeoutMs) { isVisible(target) }
    }.fold(
        onSuccess = { Unit.right() },
        onFailure = { ElementError.WaitTimeout(target.description, timeoutMs).left() }
    )

    // `when` used as an expression (its result is returned). Because Target is a sealed class,
    // the compiler verifies all variants are handled — no `else` needed.
    // `is Target.ByText` is a smart cast: after the check, `target.text` is accessible without an explicit cast.
    private fun matcherFor(target: Target): SemanticsMatcher = when (target) {
        is Target.ByText -> hasText(target.text, substring = true, ignoreCase = true)
        is Target.ByDescription -> hasContentDescription(target.contentDescription)
    }
}
