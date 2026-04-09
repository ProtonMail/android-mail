package ch.protonmail.android.e2e.screenplay

import arrow.core.Either
import arrow.core.left
import arrow.core.right

// `data class` auto-generates equals(), hashCode(), toString(), and copy() from the constructor params.
data class Credentials(val username: String, val password: String)

interface Ability

// `fun interface` = SAM (Single Abstract Method). Lets callers pass a lambda instead of
// an anonymous object: `Performable { actor -> ... }` instead of `object : Performable { ... }`.
fun interface Performable {
    fun performAs(actor: Actor): Either<ScreenplayError, Unit>
}

fun interface Answerable<T> {
    fun answeredBy(actor: Actor): Either<ScreenplayError, T>
}

class Actor(
    val name: String,
    val credentials: Credentials
) {
    private val abilities = mutableMapOf<Class<out Ability>, Ability>()
    private val notes = mutableMapOf<String, Any>()

    // `= apply { ... }` runs the block with `this` as receiver and returns `this`,
    // enabling fluent chaining: actor.can(driver1, driver2).attemptsTo(...)
    fun can(vararg newAbilities: Ability): Actor = apply {
        for (ability in newAbilities) {
            // Register by concrete class and all Ability-extending interfaces
            abilities[ability::class.java] = ability
            ability::class.java.interfaces
                .filter { Ability::class.java.isAssignableFrom(it) }
                .forEach { abilities[it as Class<out Ability>] = ability }
        }
    }

    // `@PublishedApi internal` makes this visible to the inline function below but not to external callers.
    // `as? T` is a safe cast — returns null instead of throwing ClassCastException.
    // `?: error(...)` is the elvis operator — if the left side is null, throw IllegalStateException.
    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Ability> abilityTo(type: Class<T>): T = abilities[type] as? T
        ?: error("$name does not have the ability ${type.simpleName}")

    // `inline` + `reified` preserves the type parameter T at runtime (normally erased).
    // Callers write `abilityTo<UIDriver>()` instead of passing a Class object.
    inline fun <reified T : Ability> abilityTo(): T = abilityTo(T::class.java)

    fun remember(key: String, value: Any) { notes[key] = value }

    @Suppress("UNCHECKED_CAST")
    fun <T> recall(key: String): T = notes[key] as? T ?: error("$name has no note for '$key'")

    fun attemptsTo(vararg tasks: Performable): Either<ScreenplayError, Unit> {
        for (task in tasks) {
            // `.onLeft { return ... }` — Arrow's Either method. The `return` here is a *non-local return*:
            // it exits `attemptsTo`, not just the lambda, because `onLeft` is an inline function.
            task.performAs(this).onLeft { return it.left() }
        }
        // `Unit.right()` wraps Kotlin's Unit (≈ void) in Either.Right — the "success with no value" case.
        return Unit.right()
    }

    fun <T> asksFor(question: Answerable<T>): Either<ScreenplayError, T> = question.answeredBy(this)
}
