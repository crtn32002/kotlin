// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

import kotlin.test.assertEquals

class Value<T>(val value: T) {
    inline fun <R> runBlock(block: (T) -> R) = block(value)
}
fun <T> Value<Array<T>>.test() =
    runBlock {
        var sum = 0
        for (i in it.indices)
            sum = sum * 10 + i
        sum
    }

fun box(): String {
    assertEquals(123, Value<Array<Int>>(Array<Int>(4) { 0 }).test())
    return "OK"
}