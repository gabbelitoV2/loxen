<!-- scope: Moblin/View/Settings/DeepLinkCreator/ -->
## Percent encoding and deep links (DLC, package com.moblin.android.platform.core)
- `string.addingPercentEncoding(withAllowedCharacters: CharacterSet.urlQueryAllowed)` -> `string.addingPercentEncoding(withAllowedCharacters = CharacterSet.urlQueryAllowed)` (returns `String?`), with `import com.moblin.android.platform.core.CharacterSet` and `import com.moblin.android.platform.core.addingPercentEncoding`. Never `Uri.encode` or `URLEncoder`: they encode `:` and `/`, which Apple's set keeps.
- `try settings.toString()` on a `MoblinSettingsUrl` -> `settings.toString()`, which already sorts keys and keeps slashes like Swift's JSONEncoder options. Never `Json.encodeToString(settings)`.
