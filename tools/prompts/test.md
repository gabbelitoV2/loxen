# Tier: test

This file is a Swift Testing suite. Translate it to a JUnit 4 test class with kotlin.test assertions so the ported logic can be verified.

- struct FooSuite -> class FooSuite. @Test func x() -> @Test fun x(). #expect(a == b) -> assertEquals(b, a). #expect(cond) -> assertTrue(cond). #expect(a != b) -> assertNotEquals(b, a). #expect(throws:) -> assertFailsWith. Suite-level setup in init -> @Before fun setUp().
- Test data helpers stay in the same file. Byte literals stay byte-identical.
- The types under test live in the packages listed in the glossary. Import them from there.
