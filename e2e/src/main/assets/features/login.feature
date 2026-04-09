Feature: Login

  Scenario: Login with invalid credentials shows error
    Given "Bob" tries to login
    When they input wrong credentials
    Then they get an error

  Scenario: Login with valid credentials succeeds
    Given "Bob" tries to login
    When they input their credentials
    Then they see the mailbox
