package ch.protonmail.android.e2e.steps

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.cucumber.java.After
import io.cucumber.java.Before
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val RULE_INIT_TIMEOUT_SECONDS = 10L
private const val TEARDOWN_JOIN_TIMEOUT_MS = 5_000L

class CucumberComposeRule {

    @Before(order = 0)
    fun setUp() {
        _rule = createEmptyComposeRule()
        initialized = CountDownLatch(1)
        testComplete = CountDownLatch(1)

        // `object : Statement() { }` — anonymous class (like Java's `new Statement() { ... }`).
        val wrappedStatement = _rule.apply(
            object : Statement() {
                override fun evaluate() {
                    initialized.countDown()
                    testComplete.await()
                }
            },
            Description.createTestDescription("Cucumber", "scenario")
        )
        // `Thread { ... }` — lambda as SAM conversion for Runnable.
        // `.apply { start() }` calls start() on the Thread and returns it for assignment.
        ruleThread = Thread {
            wrappedStatement.evaluate()
        }.apply { start() }

        val ready = initialized.await(RULE_INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        check(ready) { "ComposeTestRule failed to initialize within ${RULE_INIT_TIMEOUT_SECONDS}s" }
    }

    @After(order = 0)
    fun tearDown() {
        testComplete.countDown()
        ruleThread.join(TEARDOWN_JOIN_TIMEOUT_MS)
        if (ruleThread.isAlive) {
            ruleThread.interrupt()
        }
    }

    companion object {
        private lateinit var _rule: ComposeTestRule
        private lateinit var initialized: CountDownLatch
        private lateinit var testComplete: CountDownLatch
        private lateinit var ruleThread: Thread

        val rule: ComposeTestRule get() = _rule
    }
}
