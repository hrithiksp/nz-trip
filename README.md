# South Island Road Trip

Interactive planner for our 24 Oct – 1 Nov 2026 trip.

- **Website:** https://hrithiksp.github.io/nz-trip/
- **Android app:** https://github.com/hrithiksp/nz-trip/releases/latest/download/nz-trip.apk

## Android app

`android/` is a small WebView app that bundles `index.html`, so the planner works with no signal
(Milford Road, Mt Cook). Map, booking and DOC links open in Google Maps or the browser.
Back steps out of a day to the overview, then closes the app.

Every push that changes `index.html` or `android/` rebuilds the app on GitHub Actions and publishes
it as the latest release. Builds are signed with `android/nztrip.keystore`, so a new build installs
over the old one. That key is only for this personal trip app; don't reuse it for anything else.

### Install on a Samsung phone
1. Open the APK link above on the phone and download `nz-trip.apk`.
2. Open it from the download notification or **My Files → Downloads**.
3. If asked, allow **Install unknown apps** for the browser or My Files.
4. If Samsung's **Auto Blocker** stops the install, turn it off for a moment:
   Settings → Security and privacy → Auto Blocker. Turn it back on afterwards.
5. If Google Play Protect warns about an unknown app, tap **More details → Install anyway**.
