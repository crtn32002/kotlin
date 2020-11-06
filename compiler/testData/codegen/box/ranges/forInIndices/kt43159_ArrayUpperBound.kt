// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND: JVM, JVM_IR

import kotlin.test.assertEquals

fun <T : IntArray> test(array: T): Int {
    var sum = 0
    for (i in array.indices) {
        sum = sum * 10 + i
    }
    return sum
}

fun box(): String {
    assertEquals(123, test(intArrayOf(0, 0, 0, 0)))
    return "OK"
}