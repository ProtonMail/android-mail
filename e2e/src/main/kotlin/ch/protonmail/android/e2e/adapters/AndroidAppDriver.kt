package ch.protonmail.android.e2e.adapters

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.e2e.screenplay.ScreenplayError
import ch.protonmail.android.e2e.screenplay.ports.AppDriver

class AndroidAppDriver : AppDriver {

    // `get() = ...` without a backing field makes these computed properties — re-evaluated on every access,
    // like a Java getter with no field. Ensures we always get the current instrumentation state.
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val targetContext get() = instrumentation.targetContext
    private val appPackage get() = targetContext.packageName

    // `runCatching { }.fold(...)` is Kotlin's functional try-catch:
    // the block runs inside a try, and `fold` maps success/failure to Either values.
    override fun launch(): Either<ScreenplayError.ActivityError, Unit> = runCatching {
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant $appPackage android.permission.POST_NOTIFICATIONS"
        ).close()

        val intent = targetContext.packageManager.getLaunchIntentForPackage(appPackage)
            ?: error("No launch intent for $appPackage. Is the app installed?")
        // `or` is Kotlin's infix bitwise OR — equivalent to Java's `|` for Int flags.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        targetContext.startActivity(intent)
    }.fold(
        onSuccess = { Unit.right() },
        // `it.message ?: "unknown"` — elvis operator: use the message if non-null, otherwise "unknown".
        onFailure = { ScreenplayError.ActivityError.LaunchFailed(it.message ?: "unknown").left() }
    )

    override fun stop(): Either<ScreenplayError.ActivityError, Unit> = runCatching {
        instrumentation.uiAutomation.executeShellCommand("am force-stop $appPackage").close()
    }.fold(
        onSuccess = { Unit.right() },
        onFailure = { ScreenplayError.ActivityError.NotRunning().left() }
    )
}
