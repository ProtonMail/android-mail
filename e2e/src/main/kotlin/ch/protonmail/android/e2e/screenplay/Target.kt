package ch.protonmail.android.e2e.screenplay

// `sealed class` restricts all subclasses to this file. The compiler knows every variant,
// so `when` expressions over a Target are exhaustive — no `else` branch needed.
sealed class Target(val description: String) {
    class ByText(val text: String) : Target("text='$text'")
    class ByDescription(val contentDescription: String) : Target("desc='$contentDescription'")
}
