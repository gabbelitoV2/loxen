# Incremental update

This file was translated before and the Swift source has changed since. You receive the unified diff of the Swift change, the new Swift file, and the current Kotlin file.

The current Kotlin file is the truth for everything the diff does not touch. It contains hand-written Android fixes, calls into com.moblin.android.platform and deliberate deviations from the Swift code. Keep all of them.

- Apply only the change described by the Swift diff to the Kotlin file, using the same translation rules as for a full translation.
- Every Kotlin line that does not correspond to a changed Swift line must stay byte-identical, including formatting, imports and declaration order.
- When the diff removes Swift code, remove the matching Kotlin code. When it adds Swift code, add the Kotlin translation at the matching position. When it renames or moves code, do the same in Kotlin.
- If a changed Swift line maps to Kotlin code that was hand-written for Android, keep the Android behaviour and adapt it to the new Swift intent.
- Return the complete updated Kotlin file in the kotlin block, with the json block first as usual.
