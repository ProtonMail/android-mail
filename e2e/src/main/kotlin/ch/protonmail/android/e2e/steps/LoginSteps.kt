package ch.protonmail.android.e2e.steps

import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.e2e.adapters.AndroidAppDriver
import ch.protonmail.android.e2e.adapters.ComposeUIDriver
import ch.protonmail.android.e2e.questions.login.IsInboxVisible
import ch.protonmail.android.e2e.questions.login.IsStillOnLoginScreen
import ch.protonmail.android.e2e.screenplay.Actor
import ch.protonmail.android.e2e.screenplay.Credentials
import ch.protonmail.android.e2e.screenplay.getOrFail
import ch.protonmail.android.e2e.tasks.login.DismissOnboarding
import ch.protonmail.android.e2e.tasks.login.LaunchApp
import ch.protonmail.android.e2e.tasks.login.loginWithActorCredentials
import ch.protonmail.android.e2e.tasks.login.loginWithWrongPassword
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.util.Properties

// `private object` — a file-private singleton. Exactly one instance, created on first access.
private object TestUsers {

    // `by lazy { }` — delegated property that initializes on first read and caches the result.
    // `.apply { }` runs the block with `this` set to the new Properties(), then returns that instance.
    // `.use { }` is Kotlin's try-with-resources — auto-closes the InputStream after the block.
    private val props: Properties by lazy {
        Properties().apply {
            val context = InstrumentationRegistry.getInstrumentation().context
            context.assets.open("test-users.properties").use { load(it) }
        }
    }

    fun credentialsFor(name: String): Credentials {
        val key = name.lowercase()
        val username = props.getProperty("$key.username")
            ?: error("No username found for test user '$name' in test-users.properties")
        val password = props.getProperty("$key.password")
            ?: error("No password found for test user '$name' in test-users.properties")
        return Credentials(username, password)
    }
}

class LoginSteps {

    // `lateinit var` — promises the compiler this will be assigned before use, avoiding nullable types.
    // Accessing before assignment throws UninitializedPropertyAccessException.
    private lateinit var actor: Actor

    @Given("{string} tries to login")
    fun actorTriesToLogin(actorName: String) {
        actor = Actor(actorName, TestUsers.credentialsFor(actorName))
            .can(ComposeUIDriver(CucumberComposeRule.rule), AndroidAppDriver())
        actor.attemptsTo(LaunchApp()).getOrFail()
    }

    @When("they input wrong credentials")
    fun theyInputWrongCredentials() {
        actor.attemptsTo(loginWithWrongPassword()).getOrFail()
    }

    @When("they input their credentials")
    fun theyInputTheirCredentials() {
        actor.attemptsTo(loginWithActorCredentials()).getOrFail()
    }

    @Then("they get an error")
    fun theyGetAnError() {
        actor.asksFor(IsStillOnLoginScreen()).getOrFail()
    }

    @Then("they see the mailbox")
    fun theySeeTheMailbox() {
        actor.attemptsTo(DismissOnboarding()).getOrFail()
        actor.asksFor(IsInboxVisible()).getOrFail()
    }
}
