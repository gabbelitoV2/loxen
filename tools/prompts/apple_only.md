# Tier: apple_only

This file depends on an Apple-only service (Apple Watch connectivity, HealthKit, ReplayKit or similar) that has no Android counterpart in this port. Produce a Kotlin file with the same public API where every function body is TODO("no Android counterpart for <framework>") and list each in unsupported, so the rest of the code base compiles against it. Keep property declarations with sensible defaults so callers do not need changes.
