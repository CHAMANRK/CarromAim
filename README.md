# Carrom Vision Assist

An Android Kotlin/Jetpack Compose app for **visual shot recommendations only**. The project continues the supplied starter; it does not automate taps, aiming gestures, or striker release.

## Current implementation status

- [x] Dark Compose UI and synthetic board demo
- [x] Deterministic direct-pocket shot recommendations (no bank shots) and ghost-ball contact geometry
- [x] Angle display and rough starting-power estimate for demo shots
- [x] Segment obstacle checks, pocket line-of-sight checks, reflection helpers, and boundary-ray helper
- [x] Unit tests for baseline physics
- [x] Explicit MediaProjection consent request passed to a mediaProjection foreground service
- [x] Bounded ImageReader queue (`maxImages=2`), latest-frame acquisition, callback registration, pause/resume/stop service actions, and resource cleanup
- [x] Platform-independent frame model, conservative color-based board plausibility heuristic, corner validation, normalized coordinates, confidence gate, and detection tracker
- [x] Board/frame/display/overlay mapping helpers including aspect-fit letterboxing and rotation helpers
- [x] GitHub Actions workflow for tests and debug APK artifact
- [ ] Full OpenCV perspective/contour/circle detector and manual corner/object correction UI
- [ ] Convert captured Android `Image` planes to vision frames and connect the detectors to the shot planner
- [ ] Full transparent non-touchable overlay, opacity control, and overlay coordinate integration
- [ ] Device-specific rotation/resizing and protected-content acceptance testing
- [ ] Install/launch verification on a real device or emulator

**Important:** The current capture service acquires and promptly closes frames but does not yet convert them to pixels or produce live recommendations. The board detector is a deliberately conservative baseline heuristic, not a general-purpose carrom detector. The app must not be described as a completed live vision assistant yet.

## Requirements

- Android Studio / Android SDK Platform 35 and Build Tools 35.0.0
- JDK 17
- Gradle 8.13 (the provided `gradlew` launcher uses an installed Gradle if available; otherwise it bootstraps the official distribution and needs network access)

## Build and test

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected APK path after a successful build: `app/build/outputs/apk/debug/app-debug.apk`.

The supplied ZIP did **not** include `gradle/wrapper/gradle-wrapper.jar`; `gradlew` is a small bootstrap launcher, not the standard Gradle Wrapper. In an environment with network access, the script downloads Gradle 8.13 from the official Gradle distribution. In restricted environments, install Gradle 8.13 and make `gradle` available on `PATH`.

## GitHub Actions

Push the project root to GitHub. The `Android CI` workflow runs on push, pull request, and manual dispatch; provisions JDK 17, Gradle 8.13 and Android SDK Platform 35; runs unit tests and `assembleDebug`; and uploads the debug APK and diagnostic logs. No signing secrets are needed for a debug APK.

## Permissions and privacy

- Screen capture begins only after explicit Android MediaProjection consent. Consent is session-specific and is not persisted.
- Android 14+ requires a foreground service of type `mediaProjection`, the corresponding permission, and fresh consent for each session.
- Screen frames are not uploaded. The current capture pipeline acquires frames locally and closes them; computer vision integration is incomplete.
- Overlay permission is a separate setting and is not yet used to render a live overlay.
- Protected windows may be blank or unavailable. No bypass is attempted.
- No Accessibility Service or gameplay automation is used.

## Physics limitations

Shot planning currently recommends only direct, unbanked pot shots. It is heuristic geometry, not a rigid-body simulation; it does not model spin, variable restitution, board-specific rules, or guarantee pocketing. The displayed angle and power are starting estimates for the synthetic demo board. Confidence is a geometry score, not a calibrated probability. Live Carrom Pool detection is not connected yet.

## License

MIT. See `LICENSE`.
