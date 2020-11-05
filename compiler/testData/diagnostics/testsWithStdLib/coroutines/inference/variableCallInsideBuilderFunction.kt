// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FULL_JDK

fun foo(s: Any) {
    if (s is String) {
        s.length
    }
}
