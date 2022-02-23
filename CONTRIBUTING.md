# ProtonMail contributions guidelines

Every contribution to the project needs to follow some rules.

**Team decisions are tracked in form of ADRs in /docs folder**

### Planning

Every significant change needs to be **discussed with the team** and go through a **planning process**; *this excludes minor contributions, simple bug fixes, or performance improvements that don't impact the app's behavior.*

If the contribution is from a Proton member, the presence on the team daily standups is preferred.

### Git best practices

**Merge/Pull Requests must be of a reasonable size**, without exceeding the +**500 LOC**; more significant changes must be split on more separate Pull Requests or part of a feature branch.

The **branch name** must include the **ticket(s) number** whenever possible, with the project prefix if not `MAILAND`, followed by a brief description of the change: e.g., `fix/123_crash-on-login` or `chore/L10N-234_capilalize-cancel-button`

The **commit message** must be imperative and include a brief description if needed. The last line must include the whole ticket: e.g.

```
Fix crash on login

A network request was being launched on the Main thread,
now it has been moved inside a coroutine

MAILAND-123
```

### Architecture

Every change must follow the target **architecture of the project**: a complete example can be found in the `labels` folder of the `app` module.

### Tests

Every change must be **widely covered by Unit Tests**.

### Code style

**Code style** must be compliant with **Kotlin/Android conventions** and follow the **internal rules** agreed by the team.

**Installing the Detekt plugin is a must** as it helps to follow the defined rules naturally.

<u>Merge/Pull Requests that introduce new Detekt issues won't be accepted.</u>
