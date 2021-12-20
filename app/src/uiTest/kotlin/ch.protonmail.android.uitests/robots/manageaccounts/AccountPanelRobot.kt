package ch.protonmail.android.uitests.robots.manageaccounts

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot

/**
 * [AccountPanelRobot] class contains actions and verifications for Account Manager functionality.
 */
@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
open class AccountPanelRobot {

    fun addAccount(): LoginRobot {
        return LoginRobot()
    }

    fun logoutAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .logout()
    }

    fun logoutSecondaryAccount(email: String): AccountPanelRobot {
        return accountMoreMenu(email)
            .logoutSecondaryAccount()
    }

    fun logoutLastAccount(email: String): LoginRobot {
        return accountMoreMenu(email)
            .logoutLastAccount()
    }

    fun removeAccount(email: String): AccountPanelRobot {
        return accountMoreMenu(email)
            .remove()
    }

    fun removeSecondaryAccount(email: String): InboxRobot {
        return accountMoreMenu(email)
            .removeSecondaryAccount()
    }

    fun removeLastAccount(email: String): LoginRobot {
        return accountMoreMenu(email)
            .removeLastAccount()
    }

    fun switchToAccount(accountPosition: Int): InboxRobot {
        return InboxRobot()
    }

    private fun logout(): InboxRobot {
        return InboxRobot()
    }

    private fun logoutSecondaryAccount(): AccountPanelRobot {
        return AccountPanelRobot()
    }

    private fun logoutLastAccount(): LoginRobot {
        return LoginRobot()
    }

    private fun remove(): AccountPanelRobot {
        return AccountPanelRobot()
    }

    private fun removeSecondaryAccount(): InboxRobot {
        return InboxRobot()
    }

    private fun removeLastAccount(): LoginRobot {
        return LoginRobot()
    }

    private fun accountMoreMenu(email: String): AccountPanelRobot {
        return AccountPanelRobot()
    }

    /**
     * Contains all the validations that can be performed by [AccountPanelRobot].
     */
    inner class Verify : AccountPanelRobot() {

        fun accountsListOpened(): AccountPanelRobot {
            return AccountPanelRobot()
        }

        @SuppressWarnings("EmptyFunctionBlock")
        fun accountAdded(email: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun accountLoggedOut(email: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun accountRemoved(username: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun switchedToAccount(username: String) {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as AccountPanelRobot

    companion object {
        const val accountsRecyclerViewId = R.id.account_list_recyclerview
    }
}
