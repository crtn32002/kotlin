// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect interface My {
    open fun bar()
    <!EXPECTED_DECLARATION_WITH_BODY!>open fun bas()<!> {}
    <!INCOMPATIBLE_OPEN_AND_ABSTRACT_MODIFIER_IN_EXPECT_INTERFACE!><!REDUNDANT_MODIFIER!>open<!> abstract fun bat(): Int<!>

    fun foo()


    open val a: Int
    open val b: String
    open val c: String <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = ""
    <!REDUNDANT_MODIFIER!>open<!> abstract val e: Int

    val f: Int
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class MyImpl1<!>: My

class MyImpl2: My {
    override fun foo() {}
    override val f = 0
    override val e = 1 // TODO NOW
}

