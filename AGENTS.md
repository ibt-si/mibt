# MiBT — agent notes

Single-module Android app (`:app`, namespace `org.interferolog.mibt`). Kotlin, Jetpack Compose, Navigation 3, Room, Glance widget. `minSdk 24`, `targetSdk 37`, `compileSdk 37` (Android 17). JDK 21 required; the Gradle 9.9.1 wrapper is Unix-only (no `gradlew.bat`). SDK path is in git-ignored `local.properties` (`sdk.dir=/opt/sdk`).

## Build & verify

Run from the repo root:

- `./gradlew assembleDebug` → APK in `app/build/outputs/apk/debug/`.
- `./gradlew test` — all local unit tests (executes `:app:testDebugUnitTest`; only JUnit4 tests under `app/src/test`, no instrumented tests).
- One test/class: `./gradlew :app:testDebugUnitTest --tests "org.interferolog.mibt.domain.JournalRulesTest"` (append `.methodName` for one method). `--tests` is **not** valid on the aggregating `test` task.
- `./gradlew assembleNightly` — release-type build signed with the debug key.
- `./gradlew assembleRelease` — minify/shrink on; needs git-ignored root `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), otherwise produces an unsigned APK.
- CodeQL CI uses `./gradlew --no-daemon --no-build-cache clean assembleDebug` — the clean, uncached build is intentional so Kotlin sources are actually compiled and extractable.
- Avoid `./gradlew :app:tasks --all`: with AGP 9.5.1 the `generateDebugComposePreviewRunfiles` task fails to configure because unit tests exclude Android resources.

## Dependencies & conventions

- All versions live in `gradle/libs.versions.toml`; declare libs via `libs.*` aliases in `app/build.gradle.kts`. Kotlin, Compose compiler and KSP versions must move together (Dependabot groups them).
- Core library desugaring is on (`isCoreLibraryDesugaringEnabled` + `coreLibraryDesugaring(libs.desugar.jdk.libs)`); the app uses `java.time` throughout, so keep it while `minSdk < 26`. `minSdk 24` is the floor set by Material3 1.5 and Navigation 3 1.2 — going lower means downgrading those.
- GPL-3.0-or-later: every `.kt` file starts with the copyright line and `// SPDX-License-Identifier: GPL-3.0-or-later`.
- User-facing copy is Russian and lives as literals in Compose files and `content/JournalCanon.kt`, not in `res/values/strings.xml`.

## UI — Material 3 Expressive (strict)

- `ui/theme/MibtTheme.kt` wraps everything in `MaterialExpressiveTheme` with `MotionScheme.expressive()`; dynamic color on Android 12+ (API 31+), otherwise `expressiveLightColorScheme()` / `expressiveDarkColorScheme()`.
- Use `MaterialTheme.colorScheme` and `MaterialTheme.typography` tokens, including Emphasized roles (`titleLargeEmphasized`, `headlineSmallEmphasized`, `labelLargeEmphasized`). Reuse shared composables in `ui/Fields.kt`.
- Expressive components (`LargeFlexibleTopAppBar`, `FloatingActionButtonMenu`, `ToggleFloatingActionButton`) require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.
- Do not hardcode colors or spacing; take them from existing M3E tokens/logic. The only intentional raw colors are the four on-container overrides in `MibtTheme.kt` (mirroring Material 3 1.5's on-container tone shifts).

## Architecture map

- `content/JournalCanon.kt` — canonical book texts, week titles, forms and formulas. The book's Flight log is the source of truth: where app and log disagree, the log wins. Do not invent lines, examples or numbers.
- `domain/` — pure logic (formulas, `RoutePlanner`, `Money`, enums). Money is integer kopecks (e.g. `60_000_00`). This is where unit tests concentrate.
- `data/` — Room entities, DAO, `JournalRepository`, telemetry export. One `MibtDatabase` singleton via `MibtDatabase.get(context)` (`mibt.db`, v1, `exportSchema = false`); no migrations yet.
- `navigation/` — Navigation 3. Every `NavKey` route subclass must be registered in `MibtNav.kt`'s `navConfig` polymorphic serializer. `MainActivity` deep-link intent extra `"open"` accepts `"pause"` / `"pointc"`.
- `JournalViewModel` (root) — `AndroidViewModel` exposing one `ui` StateFlow from the repository; all writes go through its coroutine-launching methods.
- `reminder/` — silent Point C reminders and `BOOT_COMPLETED` receiver; `widget/` — Glance "3-second pause" widget. `MibtApplication` creates the `point-c` notification channel on API 26+ and reschedules reminders at startup.

## Releases

Push to `master`/`main` runs `.github/workflows/release.yml`. It derives tag `v<versionName>-<versionCode>` from `app/build.gradle.kts`, so bump `versionCode`/`versionName` to publish (re-pushing the same version is a no-op). Requires secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Manual dispatch with `force` replaces an existing release's APK.
