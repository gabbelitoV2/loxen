## Documents directory (SYS, package com.moblin.android.platform)
- `URL.documentsDirectory` -> `com.moblin.android.platform.Documents.directory`, a java.io.File for `<filesDir>/Documents`; never derive it from java.io.tmpdir, the cache directory's parent or `filesDir` itself. `URL.documentsDirectory.appending(component: x)` -> `File(com.moblin.android.platform.Documents.directory, x)`.
