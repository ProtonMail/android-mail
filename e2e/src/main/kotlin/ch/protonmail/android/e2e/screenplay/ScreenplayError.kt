package ch.protonmail.android.e2e.screenplay

import arrow.core.Either

// `sealed interface` — like sealed class but allows multiple inheritance.
// All implementations must be in this file, giving exhaustive `when` matching.
sealed interface ScreenplayError {
    val message: String

    sealed interface ElementError : ScreenplayError {
        data class TargetNotFound(
            val target: String,
            override val message: String = "Element $target not found"
        ) : ElementError
        data class WaitTimeout(
            val target: String,
            val timeoutMs: Long,
            override val message: String = "Element $target not found within ${timeoutMs}ms"
        ) : ElementError
    }

    sealed interface AssertionError : ScreenplayError {
        data class ConditionNotMet(
            val condition: String,
            override val message: String = "Condition not met: $condition"
        ) : AssertionError
    }

    sealed interface InteractionError : ScreenplayError {
        data class ClickFailed(
            val target: String,
            val cause: String,
            override val message: String = "Failed to click $target: $cause"
        ) : InteractionError
        data class InputFailed(
            val target: String,
            val cause: String,
            override val message: String = "Failed to enter text into $target: $cause"
        ) : InteractionError
    }

    sealed interface ActivityError : ScreenplayError {
        data class LaunchFailed(
            val reason: String,
            override val message: String = "App launch failed: $reason"
        ) : ActivityError
        data class NotRunning(override val message: String = "App is not running") : ActivityError
    }
}

// Extension function — adds `getOrFail()` to any `Either<ScreenplayError, T>` without modifying the class.
// `fold` destructures the Either: `ifLeft` handles the error branch, `ifRight` handles the success branch.
// `it::class.simpleName` uses Kotlin reflection to get the runtime class name (e.g. "WaitTimeout").
fun <T> Either<ScreenplayError, T>.getOrFail(): T = fold(
    ifLeft = { throw AssertionError("[${it::class.simpleName}] ${it.message}") },
    ifRight = { it }
)
