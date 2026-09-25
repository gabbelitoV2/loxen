<!-- scope: Moblin/Various/Model/, Moblin Live Activity/ -->
## Live Activities (LA, package com.moblin.android.platform.activitykit)
- ActivityKit has a shim that shows the Live Activity as Moblin's ongoing notification: import `Activity`, `ActivityAttributes`, `ActivityContent`, `ActivityAuthorizationInfo`, `ActivityAuthorizationError` and `ActivityUIDismissalPolicy` from `com.moblin.android.platform.activitykit`. Never TODO() them and never use NotificationManager.
- `struct X: ActivityAttributes { struct ContentState: Codable, Hashable { ... } }` -> `class X : ActivityAttributes { @Serializable data class ContentState(...) }`; `ActivityAttributes` is an interface without members. Other `Codable, Hashable` structs there -> `@Serializable data class` with the same properties.
- `X.ContentState` stays nested in its attributes class: import only `X` and write `X.ContentState(...)` (`LiveActivityAttributes.ContentState`), never a top-level `ContentState`.
- `Activity<X>?` keeps that type (`var liveActivity: Activity<LiveActivityAttributes>? = null`), never `Any?`.
- `ActivityAuthorizationInfo().areActivitiesEnabled` keeps its shape, a Boolean property (false when Moblin may not post notifications).
- `try Activity.request(attributes: a, content: .init(state: s, staleDate: nil))` -> `Activity.request(attributes = a, content = ActivityContent(state = s, staleDate = null))`; it throws `ActivityAuthorizationError`, an Exception. `staleDate` is a `java.time.Instant?`.
- `await activity.update(.init(state: s, staleDate: nil))` -> `activity.update(ActivityContent(state = s, staleDate = null))`; `await activity.end(nil, dismissalPolicy: .immediate)` -> `activity.end(null, dismissalPolicy = ActivityUIDismissalPolicy.immediate)` (also `.default`, `.after(date)`). Both are suspend functions that never suspend and never need the main thread.
- `Activity<X>.activities` -> `Activity.activities<X>()`.
- A `DispatchSemaphore` that waits for a `DispatchQueue.global().async { Task { ... } }` around `end` or `update` -> `runBlocking { ... }` around the same calls, never a semaphore waiting for a main-thread coroutine.
