# Tier: test

This file is a Swift Testing suite. Translate it to a JUnit 4 test class with kotlin.test assertions so the ported logic can be verified.

- struct FooSuite -> class FooSuite. @Test func x() -> @Test fun x(). #expect(a == b) -> assertEquals(b, a). #expect(cond) -> assertTrue(cond). #expect(a != b) -> assertNotEquals(b, a). #expect(throws:) -> assertFailsWith. Suite-level setup in init -> @Before fun setUp().
- Test data helpers stay in the same file. Byte literals stay byte-identical.
- The types under test live in the packages listed in the glossary. Import them from there.
- Swift `Data == Data` and `[UInt8] == [UInt8]` -> assertContentEquals, never assertEquals on ByteArray.
- @Test(arguments: [...]) -> one @Test that loops over the arguments and creates every local object (writer, generator, mock) inside the loop body. Never use the JUnit Parameterized runner.
- Copy long literals (hex, base64, JSON, expected text) character for character, including runs of repeated characters and non-breaking spaces (U+00A0, U+202F).
- A Swift enum case literal (.speed(.system), .goLive) -> the Kotlin constant or sealed subclass, never a lookup through fromRawValue/fromString.
- A @MainActor or async Swift test whose code under test uses the main queue -> `fun x() = runMainTest { ... }` (com.moblin.android, TestUtils.kt), which runs the body on Dispatchers.Main and advances the Robolectric main looper. Never runBlocking on the main thread for such tests.
- The Swift suites run with the en_SE locale and the Europe/Stockholm time zone. A suite whose results depend on locale or time zone gets @Config(qualifiers = "en-rSE") and sets TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm")) in @Before.
- A Swift sealed/associated-value enum that the Kotlin code encodes with hand-written toJsonElement()/fromJsonElement() is encoded the same way in the test, not with kotlinx encodeToJsonElement.
- Test helpers from MoblinTests/TestUtils.swift live in com.moblin.android (TestUtils.kt); MessageQueue.get() times out after 10 s, so a missing callback fails the test instead of hanging the run.

