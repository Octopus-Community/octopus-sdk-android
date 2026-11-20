# Keep the entire public domain API (models, repositories, use cases,
# OctopusResult, events). The `class` keyword already matches interfaces and
# enums, and `**` matches nested `$` (sealed) types, so this single rule covers
# everything the previous separate enum/interface/nested rules did.
-keep class com.octopuscommunity.sdk.domain.** { *; }
