yw# SakhiSafe — 7-Day MVP Design Spec

**Date:** 2026-04-25
**Author:** Karan + Claude
**Demo target:** SRM Major Project final demo, ~2026-05-02
**Approach selected:** Option C (Thesis + Guardian Dashboard) + WESAD ML model

---

## 1. Goal of the demo

Land the research-paper thesis in a 3-minute live demo: **passive, biometric, ML-assisted detection of distress that the wearer never had to manually trigger**, with an end-to-end pipeline from watch sensor to a guardian/police dashboard.

The judges should leave believing:

1. The system **works** end-to-end (watch → phone → cloud → dashboard).
2. The detection is **passive** (no button press required), differentiating from existing panic-button apps.
3. There is a **real ML model** (not just heuristics) doing inference on-device.
4. The team is **honest** about what's heuristic, what's ML, and what's future work.

## 2. Non-goals (explicitly out of scope for the demo)

- Real Node.js/Express/Mongo/Postgres backend (Firebase replaces it).
- Real police-station integration (the Guardian Dashboard *simulates* this view).
- Real SMS via SMSManager (Google has restricted; we use WhatsApp deep-link).
- Cloud Functions / push notifications (would require Blaze billing — we stay on Spark/free).
- EDA (electrodermal activity) — Fossil Gen 5 has no GSR sensor. Honestly listed as future work.
- Multi-class threat classification — binary anomaly is enough for the demo.
- Production-grade security (Firebase Auth defaults are sufficient; no custom JWT).
- Continuous HR streaming to cloud (privacy + cost; only alert windows go to Firestore).
- Offline-first sync, account recovery flows, profile pictures, social features.

## 3. Tech stack (locked decisions)

| Concern | Decision | Why |
|---|---|---|
| Phone app language | **Kotlin + Jetpack Compose + Material 3** | Figma uses gradient cards, animated rings, custom illustrations — Compose builds these in 1/3 the code of XML. Keeps language consistent with the watch app. |
| Watch app language | **Kotlin + Wear Compose** (already there) | No change. |
| Phone app architecture | **Single-Activity + Compose Navigation + ViewModels** | Standard 2026 Android pattern. |
| Backend | **Firebase (Spark/free tier)** — Auth, Firestore, Storage, Hosting | Zero server code, real cloud, free, judges see "the data goes somewhere". |
| Watch ↔ Phone transport | **Wear Data Layer API** (already wired) | Working today, no Bluetooth GATT needed. |
| Phone ↔ Cloud transport | **Firebase Android SDK** | Direct, no intermediary. |
| Dashboard ↔ Cloud transport | **Firebase JS SDK** (`v10`, modular) | Real-time `onSnapshot` listeners give the "lights up live" demo moment. |
| ML inference | **TFLite on-device** (watch APK) | Matches paper's "edge processing" claim. |
| ML training | **Python + scikit-learn → TFLite** in Colab on **WESAD** dataset | Public, peer-reviewed, citable. |
| Alert delivery | **WhatsApp deep-link** (`https://wa.me/<number>?text=<msg>`) | Free, instant, judges see the message land. Twilio rejected for setup cost. |
| Multi-contact strategy | **Fan-out** — message all contacts in parallel | Figma shows "emergency circle" UI; matches PPT messaging. |
| Maps | **Leaflet + OpenStreetMap** in dashboard; **`Intent` to Google Maps** in WhatsApp link | OSM is free, no API key. |
| Dashboard hosting | **Firebase Hosting** (single site, static HTML/JS) | Same Firebase project, no extra account. |
| Package ID | **`com.sakhisafe.app`** for both modules | Wear pairing model. Replaces `com.example.myapp` placeholder. |

## 4. Architecture

```
┌─────────────────────┐         ┌─────────────────────────┐
│  WEAR OS (Fossil 5) │         │   ANDROID (Moto G71)    │
│                     │         │                         │
│ • HR sensor         │ Wear    │ • Splash + Auth         │
│ • Accelerometer     │ Data    │ • Home (avatars, SOS,   │
│ • Heuristic detector│ Layer   │   sound alarm)          │
│ • TFLite (WESAD)    │ ──────► │ • Vitals (live HR + ECG)│
│ • 3× button SOS     │         │ • Contacts (Family/     │
│ • "Are you safe?"   │         │   Friends tabs)         │
│   countdown         │         │ • Help, About           │
│                     │         │ • Long-press SOS        │
│                     │         │ • Sound Alarm           │
│                     │         │ • Alert orchestrator    │
└─────────────────────┘         │   (GPS, audio, fan-out  │
                                │   WhatsApp)             │
                                └────────────┬────────────┘
                                             │ Firebase SDK
                                             ▼
                          ┌────────────────────────────────┐
                          │   FIREBASE (Spark/free)        │
                          │ • Auth (email/password)        │
                          │ • Firestore                    │
                          │   - users/{uid}                │
                          │   - users/{uid}/contacts/{id}  │
                          │   - alerts/{alertId}           │
                          │ • Storage                      │
                          │   - audio/{alertId}.m4a        │
                          │ • Hosting (dashboard.html)     │
                          └────────────────┬───────────────┘
                                           │ realtime
                                           ▼
                          ┌────────────────────────────────┐
                          │  GUARDIAN DASHBOARD (web)      │
                          │ • Auth-gated (guardian login)  │
                          │ • Live map (Leaflet + OSM)     │
                          │ • Active alert banner          │
                          │ • HR timeline (Chart.js)       │
                          │ • Audio playback               │
                          │ • Alert history                │
                          └────────────────────────────────┘
```

### Module structure

```
SakhiSafe/
├── phoneapp/              (renamed from app/, full rewrite to Compose)
│   └── src/main/java/com/sakhisafe/app/
│       ├── ui/            (Compose screens)
│       ├── data/          (Firebase repositories)
│       ├── domain/        (alert orchestrator, models)
│       └── wear/          (Data Layer listener)
├── wearapp/               (renamed from sakhisafeapp/)
│   └── src/main/java/com/sakhisafe/app/wear/
│       ├── ui/            (existing Compose, restyled)
│       ├── sensors/       (HR + ACC collection)
│       ├── detection/     (heuristic + TFLite)
│       └── transport/     (Data Layer sender)
├── ml/                    (NEW — Python training, not in APK)
│   ├── train_wesad.ipynb
│   ├── data/              (gitignored, WESAD download)
│   └── model.tflite       (generated, committed)
├── dashboard/             (NEW — static web app)
│   ├── index.html
│   ├── app.js
│   ├── style.css
│   └── firebase.json      (hosting config)
├── design/figma/          (mockup PNGs, already there)
└── docs/superpowers/specs/(this file)
```

The empty `sakhisafe/` module is **deleted** in Phase 1.

## 5. Phase breakdown (the 8 phases)

Each phase is independently shippable. If we have to stop early, we still have a working demo at the cut point.

### Phase 1 — Project Hygiene & Firebase Bootstrap

**Goal:** Clean foundation. Project builds with Firebase initialized.

**Tasks:**
- Delete the empty `sakhisafe/` module from `settings.gradle.kts`.
- Rename modules: `app/ → phoneapp/`, `sakhisafeapp/ → wearapp/`.
- Rename packages: `com.example.myapp → com.sakhisafe.app` (and `.wear` for watch).
- Create Firebase project on console, register Android app, download `google-services.json` for both modules.
- Add Firebase BOM, Auth, Firestore, Storage SDKs to `phoneapp/build.gradle.kts`.
- Update `libs.versions.toml` to add Compose BOM, Material 3, Coil, Lottie, Firebase BOM. **No Hilt** for MVP — single-instance manual DI is fine for 7 days.
- Verify clean build, app launches with Firebase initialized (log a token).

**Done when:** `./gradlew assembleDebug` is green; both APKs install; logs show Firebase initialized.

### Phase 1.5 — Compose Migration Scaffold

**Goal:** Phone app rewritten to single-Activity Compose shell with placeholder routes for every screen.

**Tasks:**
- Replace `MainActivity` (Java) with `MainActivity.kt` hosting `setContent { SakhiSafeApp() }`.
- Set up `NavHost` with routes: `splash`, `auth/login`, `auth/register`, `home`, `vitals`, `contacts`, `contacts/add`, `help`, `about`, `sos/countdown`, `sos/active`.
- Build `MaterialTheme` with the design tokens from §7.
- Create `BottomNavBar` Composable (Home / Vitals / Help / Info — matches Figma).
- Wire up Compose Navigation transitions.
- All screens are placeholders that show "screen: <name>".
- Delete old XML layouts in `phoneapp/res/layout/`.

**Done when:** App boots into a styled splash, you can tab through all 4 bottom-nav screens and reach every route via test buttons.

### Phase 2 — Real Auth + Emergency Contacts

**Goal:** Real authentication + working contact management. Replaces fake login.

**Tasks:**
- Build `LoginScreen.kt` and `RegisterScreen.kt` matching the modern look (inspired by Figma — clean white surface, red CTA, sub-text).
- `AuthRepository` wrapping `FirebaseAuth.getInstance()` (sign-up, sign-in, sign-out, current-user observation).
- On successful login, navigate to `home`.
- `ContactsScreen` with **Family / Friends** tabs (Figma-style red underline).
- `AddContactScreen`: fields for name, phone, relationship, group (Family/Friends).
- `ContactsRepository` writing to `users/{uid}/contacts/{contactId}` in Firestore.
- Use the system contact picker as an alternative to typing.
- Profile-pic placeholders (initials in colored circles) — no upload UI in MVP.
- Persist auth state across app kills (Firebase does this for free).

**Firestore schema:**

```js
users/{uid}: {
  email: string,
  displayName: string,
  createdAt: timestamp
}

users/{uid}/contacts/{contactId}: {
  name: string,
  phone: string,        // E.164: "+91…"
  relationship: string, // "Mother", "Brother", etc.
  group: "family" | "friends",
  createdAt: timestamp
}
```

**Firestore rules (locked-down):**

```
match /users/{uid} {
  allow read, write: if request.auth.uid == uid;
  match /contacts/{contactId} {
    allow read, write: if request.auth.uid == uid;
  }
}
```

**Done when:** Sign up → sign in → add 2 contacts → kill app → reopen → still signed in, contacts still there. Inspect Firestore console and see them.

### Phase 3 — Watch Sensors → Phone Live Stream

**Goal:** Watch streams HR + motion to phone in real time. Phone Vitals screen shows it live.

**Tasks:**
- **Watch:** Add accelerometer collection alongside existing HR. Compute **motion intensity** = sliding-window magnitude of `sqrt(ax² + ay² + az²) - g`.
- **Watch:** Maintain **rolling HR baseline** (5-minute median). Send `{ hr, baseline, motion, timestamp }` payload over Data Layer ~1 Hz (not the current ad-hoc rate).
- **Phone:** `WearDataListener` service receives the payload, writes to a `MutableStateFlow` exposed via DI / singleton.
- **Phone:** `VitalsScreen` (Figma-styled) shows:
  - Top: "My Devices" card with Fossil Gen 5 + green dot if connected.
  - Below: dark gradient panel with bold "XXX BPM" + animated ECG-style line (Compose `Canvas` drawing the rolling HR window as a stylized polyline).
- **Watch:** Foreground service to keep collecting when app is in background (not killed by Doze).

**Done when:** Wear watch on wrist → phone Vitals screen shows BPM updating ~1 Hz and the ECG line animates as new samples arrive.

### Phase 4 — Heuristic Detector + Alert Pipeline (END-TO-END)

**Goal:** The thesis demo. Anomaly on watch → alert lands in WhatsApp on the contact's phone, written to Firestore.

**Tasks:**

**Watch — heuristic detector:**
- Trigger anomaly when **all** of these hold for ≥ 10 seconds:
  1. `hr - baseline > 30 BPM` (significant elevation)
  2. `motion_variance > threshold_M` (struggle / running, not just walking)
- Tunable thresholds in `DetectorConfig`.
- On trigger: launch existing `SosActivity` ("Are You Safe?" 15s countdown) — already built.
- On user "YES" within 15s: cancel, send `alert_canceled` to phone.
- On timeout: send `alert_confirmed` to phone via Data Layer with `{ triggerSource: "heuristic", hrWindow: [...], motionWindow: [...] }`.

**Watch — manual override (existing):** 3× side-button press → same flow.

**Phone — alert orchestrator:** when `alert_confirmed` arrives:
1. Get current GPS via FusedLocationProviderClient (single high-accuracy fix, 5s timeout, fall back to last known).
2. Start MediaRecorder for **30 sec** AAC audio to a temp file.
3. Create Firestore `alerts/{alertId}` doc:
   ```js
   {
     userId, userName, triggerSource,
     status: "active",
     createdAt: serverTimestamp(),
     location: { lat, lng, accuracy },
     hrWindow: [...], // 60s leading up to alert
     motionWindow: [...],
     audioStorageUrl: null,         // filled when upload completes
     contactsNotified: [phoneNumbers],
     resolvedAt: null
   }
   ```
4. Upload audio to `gs://.../audio/{alertId}.m4a` when recording completes; patch `audioStorageUrl`.
5. **Fan-out WhatsApp:** for each contact, build URL:
   `https://wa.me/<phoneE164>?text=<urlencoded>`
   Message: `🚨 SOS from ${userName}. I need help. Live location: https://maps.google.com/?q=${lat},${lng}`
   For demo: open the **first** contact in WhatsApp via `Intent.ACTION_VIEW` (only one can be opened directly; the rest are stored as a queue and shown on an "Active Alert" screen with one-tap-to-send buttons per contact).

**Phone — Active Alert screen** (Figma-style — dark gradient + red ring):
- Shows "Sending alert to your emergency circle"
- List of contacts with status pills: ✅ Sent / ⏱ Pending / ❌ Skipped
- "I'm Safe" button → updates Firestore `status: "resolved"`, `resolvedAt: now`.

**Done when:** Shake watch hard for 10s + run in place → countdown appears → wait it out → phone Active Alert screen appears, GPS captured, audio recording, WhatsApp opens prefilled with map link, alert visible in Firestore console.

### Phase 5 — WESAD TFLite Model

**Goal:** Real ML model running on-device alongside heuristic. Defensible "we trained on WESAD" claim.

**Tasks:**

**Training (Python, in `ml/train_wesad.ipynb`):**
1. Download WESAD dataset from https://uni-siegen.sciebo.de/s/HGdUkoNlW1Ub0Gx (15 subjects, ~1.2GB; gitignored).
2. Use only **wrist-worn Empatica E4** signals: BVP (→ HR), ACC.
3. Resample to 1 Hz (matches our watch sample rate).
4. Window: 60-second sliding windows, stride 5s.
5. Features per window: HR mean, HR std, HR slope, HR max-min range, ACC magnitude mean/std/max, ACC peak count.
6. Labels: stress (label 2 in WESAD) = 1; baseline/amusement/meditation (1, 3, 4) = 0.
7. Train **scikit-learn `RandomForestClassifier`** (interpretable + small TFLite size).
8. Convert to TFLite via the standard `sklearn-onnx → onnx-tf → tflite` pipeline, or train a tiny MLP in Keras for direct TFLite export.
9. Hold-out validation: leave-one-subject-out cross-validation. **Target F1 ≥ 0.70** on stress class.
10. Export: `model.tflite` (+ `feature_scaler.json` for normalization params).
11. Commit `model.tflite` and `feature_scaler.json` to `wearapp/src/main/assets/`.

**Watch integration:**
- Add `org.tensorflow:tensorflow-lite:2.14.0` dependency (~1MB AAR).
- `MlDetector` class loads model, computes the 8 features over the same 60s window the heuristic uses, runs inference every 5s.
- Outputs probability of stress.
- **Trigger logic:** alert fires when `(heuristic OR mlProbability > 0.75)` — OR-gate keeps recall high. Send `triggerSource: "heuristic" | "ml" | "both"` in payload.

**Done when:** Watch logs show both heuristic and ML probability per second; can demo "ML predicted distress" event in logcat; the Vitals screen shows current ML score; held-out F1 metric documented in `ml/RESULTS.md`.

### Phase 6 — Guardian Dashboard (web)

**Goal:** Laptop screen lights up live when watch fires alert. Visual demo wow.

**Tasks:**
- `dashboard/index.html` — single-page app, no build step, no framework.
- Firebase JS SDK v10 modular imports via CDN.
- **Login screen:** Firebase email/password auth (separate "guardian" account; we'll create one for the demo).
- **Active Alert banner:** `onSnapshot` on `alerts` where `status == "active"`. When one appears: red flash, siren sound, all alert details below.
- **Live map** (Leaflet + OSM): pin at `alert.location`, auto-pan/zoom.
- **HR timeline chart:** Chart.js line chart of `alert.hrWindow`.
- **Audio player:** `<audio>` tag pointed at the Storage signed URL.
- **History table:** all alerts, newest first, status pills, click to view.
- **"Mark Resolved" button:** writes `status: "resolved"` back to Firestore.
- Deploy via `firebase init hosting` + `firebase deploy --only hosting`. URL like `https://sakhisafe-demo.web.app`.

**Firestore rules** (extend Phase 2):
```
match /alerts/{alertId} {
  allow read: if request.auth != null;  // any signed-in user can read
                                         // (in production: scoped by guardian relationship)
  allow create: if request.auth.uid == request.resource.data.userId;
  allow update: if request.auth != null;  // for "mark resolved"
}
```

**Done when:** Open dashboard URL on laptop, log in as guardian → trigger alert on watch → within ~3 seconds the dashboard shows the alert with map pin, HR chart, audio playback control.

### Phase 7 — UI Polish + Sound Alarm + About Screen

**Goal:** App matches Figma fidelity. Honest "About" screen. Sound Alarm feature.

**Tasks:**
- Polish each screen against Figma — spacing, shadows, micro-animations, transitions.
- **Home screen** (Figma-styled):
  - Top: avatar + "Hi, {name}" + bell icon (notifications screen, can be empty list for MVP).
  - Family / Friends tabs with avatar carousel.
  - Quick action pills: Video Call (opens dialer/WhatsApp call), Message (opens default SMS app).
  - Sound Alarm card.
  - Big circular **Press-and-hold SOS** button (3-second long press triggers same alert pipeline as watch).
- **Sound Alarm:** asset = ~10sec siren `.mp3` in `phoneapp/res/raw/siren.mp3`. Tap card → `MediaPlayer` plays at max volume. Tap again or "Stop" button to cancel.
- **Help screen:** static cards — Indian women's helpline 181, police 112, NCW 7827170170, basic safety tips.
- **About screen** (the integrity story):
  - "What this prototype does" — passive HR + motion detection, manual SOS, contact fan-out.
  - "What's heuristic vs ML" — heuristic thresholds + WESAD-trained TFLite model on-device, OR-gate.
  - "Limitations / Future work" — EDA needs different hardware, multi-class classification needs labeled assault data (ethically unavailable), real police integration needs department APIs, audio is local-only privacy mode.
  - "Research basis" — list the 5 references from PPT slide 10.
  - Team credits.
- Splash screen with logo + subtle Lottie animation (optional).

**Done when:** App visually matches the Figma look; About screen exists; long-press SOS works; sound alarm plays.

### Phase 8 — Demo Script + Rehearsal + Contingency

**Goal:** You can do the demo cold, twice in a row, even if Wi-Fi sucks.

**Tasks:**
- Write `docs/DEMO_SCRIPT.md` — exact 3-minute walkthrough with timestamps and what to say at each step.
- **Pre-record a backup screen-capture video** of the full happy-path flow (watch + phone + dashboard, screen-recorded with OBS or Android scrcpy). If demo-day Bluetooth fails, you play the video.
- Test on **mobile hotspot** from the phone (judges' Wi-Fi is unreliable).
- Pre-create test data: 3 emergency contacts already in Firestore; 1 historical "resolved" alert for the dashboard history table; guardian-account credentials written on a sticky note.
- **Failure mode rehearsal:**
  - Watch loses Bluetooth → Phone long-press SOS still works.
  - Phone has no GPS lock → fallback to "Last known location, ±100m".
  - WhatsApp blocked → still demo via dashboard "live update" view.
- Two clean dress-rehearsals end-to-end.

**Done when:** You can do the demo without looking at notes; the backup video is on your phone; you have the dashboard URL written down somewhere not on your phone.

### Phase dependency graph

```
1 ──► 1.5 ──► 2 ──► 4 ──► 6 ──► 7 ──► 8
              └► 3 ──┘    │
                          └──► 5 (parallelizable with 6)
```

**Cut order if time runs short:**
1. Drop **Phase 5** (ship heuristic-only ML, claim "TFLite scaffold present, training in progress")
2. Drop **Sound Alarm** + Help/About content reductions in Phase 7
3. Drop **Phase 6 dashboard** (last resort — kills the visual wow)

Phases 1, 1.5, 2, 3, 4, 8 are non-negotiable — they are the demo.

## 6. Detection strategy

### Heuristic (Phase 4)

```
Trigger anomaly if (for ≥ 10 consecutive seconds):
    hr_current - hr_baseline_5min_median > 30 BPM
  AND
    motion_variance_5sec > 3.0 m/s²²    (default; tune empirically in Phase 4)
```

**Calibration plan:** during Phase 4 dev, record 5 minutes each of (a) walking, (b) sitting, (c) running, (d) shaking watch deliberately. Pick `threshold_M` so (d) triggers and (a-c) don't.

### ML model (Phase 5)

- Trained on **WESAD wrist data** (HR + ACC), binary stress classifier.
- Inference every 5s on the same 60s window the heuristic uses.
- Trigger if `P(stress) > 0.75`.
- **Combined trigger:** `heuristic_fired OR ml_fired`. OR-gate maximizes recall, which is what matters for the demo (false positives are recoverable via the 15s "Are You Safe?" countdown).

## 7. Design system tokens

```kotlin
// colors
val Primary = Color(0xFFE53935)         // SOS red
val PrimaryDark = Color(0xFFC62828)
val Accent = Color(0xFFF4B400)          // amber (sound alarm)
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceDarkTop = Color(0xFF1A1F2E)  // dark gradient top
val SurfaceDarkBot = Color(0xFF0E1218)  // dark gradient bottom
val TextPrimary = Color(0xFF0A0A0A)
val TextSecondary = Color(0xFF7B7B85)
val Divider = Color(0xFFE6E6EB)
val Success = Color(0xFF34C759)
val Error = Color(0xFFFF3B30)

// shapes
val ShapeCard = RoundedCornerShape(20.dp)
val ShapeButton = RoundedCornerShape(16.dp)
val ShapePill = RoundedCornerShape(50)

// typography (Inter Variable, fallback to system)
val DisplayLarge = 48.sp / FontWeight.Bold       // "110 BPM", countdown numerals
val HeadingLarge = 24.sp / FontWeight.Bold       // "Stay Calm, Ishita."
val HeadingMedium = 18.sp / FontWeight.SemiBold  // section titles
val BodyLarge = 16.sp / FontWeight.Normal
val BodyMedium = 14.sp / FontWeight.Normal
val Label = 12.sp / FontWeight.Medium

// spacing
val Spacing4 = 4.dp; val Spacing8 = 8.dp; val Spacing12 = 12.dp
val Spacing16 = 16.dp; val Spacing24 = 24.dp; val Spacing32 = 32.dp

// elevation (light theme cards)
val Elevation1 = 2.dp; val Elevation2 = 6.dp; val Elevation3 = 12.dp
```

Add **Inter Variable** font to `phoneapp/res/font/inter.ttf` (download free from https://rsms.me/inter/). Use `FontFamily(Font(R.font.inter))`.

## 8. Demo script (3 minutes)

| Time | Action | Spoken line |
|---|---|---|
| 0:00 | Open dashboard on laptop, log in as guardian. Pre-show recent alert history. | "This is the Guardian Dashboard — what a parent or law-enforcement contact would see." |
| 0:20 | Open SakhiSafe phone app, walk through Home, Vitals (live HR from watch), Contacts (with the 3 pre-added). | "On the user's side: live vitals from the wearable, emergency contacts, and a manual SOS." |
| 0:50 | Show About screen briefly. | "We're transparent about what's heuristic vs what's ML. Our model was trained on the WESAD public dataset." |
| 1:10 | Put watch on, show Vitals screen updating in real time. | "The watch streams heart rate and motion at 1 Hz over the Wear Data Layer." |
| 1:30 | Perform the **distress simulation**: shake watch hard while running in place for 12s. | "I'm simulating a struggle — elevated heart rate plus erratic motion." |
| 1:42 | Watch shows "Are You Safe?" countdown — let it time out. | "The user is given 15 seconds to cancel. If they can't respond…" |
| 1:57 | Phone Active Alert screen lights up; WhatsApp opens with a prefilled message + location URL. | "…the alert fan-outs to all emergency contacts via WhatsApp with live location." |
| 2:10 | Switch to laptop — dashboard banner is red, map pin visible, HR chart drawn, audio play button. | "And on the guardian dashboard — the alert appears in real time with location, the heart-rate window leading up to it, and 30 seconds of audio evidence." |
| 2:30 | Click play on audio. Click "Mark Resolved." | "When the situation is handled, it's resolved on the dashboard." |
| 2:45 | Q&A buffer. | (Be ready for: "what about false positives?" → countdown lets user cancel; "what about EDA?" → future work, hardware-limited; "is the ML real?" → yes, TFLite, F1 = X on WESAD held-out.) |

## 9. Risk register

| Risk | Likelihood | Mitigation |
|---|---|---|
| Bluetooth pairing fails on demo day | Medium | Phone long-press SOS as fallback; pre-recorded video as final fallback. |
| Wi-Fi unavailable for dashboard | Medium | Use phone hotspot; dashboard pre-loaded in browser tab to keep listener alive. |
| Watch sensor permissions reset | Low | Test permission flow before demo; pre-grant via Settings. |
| WESAD model F1 < 0.6 | Low-medium | Try MLP if RandomForest underperforms; worst case fall back to heuristic-only and label model "in training". |
| Firebase quota hit | Very low | Spark limits are far above demo usage. |
| WhatsApp blocked on demo network | Low | Show via dashboard instead — same alert visible there. |
| Compose migration burns more than half a day | Medium | Time-box to 1 day in Phase 1.5; if blown, fall back to keeping existing XML for Login/Register and Compose for new screens (mixed). |
| ML training takes longer than 1 day | Medium | If Day 4 of training fails, ship Phase 6 dashboard first, finish ML model after. |
| Audio recording permission denied | Low | Request at first launch alongside location; degrade gracefully (alert still fires without audio). |

## 10. What to do after this spec is approved

Invoke the `superpowers:writing-plans` skill to expand each phase into a step-by-step implementation plan with file-level tasks. The plan is what we'll execute against.
