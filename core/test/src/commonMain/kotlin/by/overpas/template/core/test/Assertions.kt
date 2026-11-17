package by.overpas.template.core.test

inline fun <reified T> assertOn(
    t: T,
    block: T.() -> Unit,
) {
    t.block()
}
