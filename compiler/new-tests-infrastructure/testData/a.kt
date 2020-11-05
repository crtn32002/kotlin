// MODULE: common
// FILE: b.kt

fun foo(): String = Utils.create()

// FILE: Utils.java

public class Utils {
    public static String create() {
        return "hello";
    }
}

// MODULE: main
// DEPENDENCY: common SOURCE
// FILE: a.kt

fun test() = foo().length

fun test_2() = Utils.create()

fun test_3(s: Any) {
    if (s is String) {
        <!DEBUG_INFO_SMARTCAST, SOME_ERROR{OI}!>s<!>.length
    }
    return <!TYPE_MISMATCH("Unit; String")!>""<!>
}
