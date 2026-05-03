# VSafe — 3-Minute Demo Script

> Run-time target: 3 min walkthrough + 2 min Q&A buffer.

## Pre-demo setup (the morning of)

- [ ] Both APKs installed: `phoneapp` on Moto G71 + `wearapp` on Fossil Gen 5
- [ ] Watch paired with phone (verify in Wear OS app)
- [ ] Phone tethering ON (mobile hotspot — judges' Wi-Fi is unreliable)
- [ ] Laptop connected to the phone hotspot
- [ ] Dashboard URL bookmarked: `https://heysafe-demo-4bca5.web.app`
- [ ] Dashboard already signed in as guardian (KEEP TAB OPEN to maintain Firestore listener)
- [ ] Firestore rules deployed (Firebase console → Firestore → Rules → published)
- [ ] In the app: signed-in test user (`demo-user@heysafe.demo` / `demoSafe123`)
- [ ] 3 emergency contacts pre-saved (Family + Friends mix; valid +91 phone numbers, ideally on 2-3 phones in airplane mode that can receive WhatsApp later)
- [ ] At least 1 historical resolved alert present so dashboard history isn't empty
- [ ] Watch battery > 50%
- [ ] Backup video on phone (transferred via `adb push` to `/sdcard/Movies/heysafe-demo.mp4`)
- [ ] Sticky note with: dashboard URL, guardian creds, demo-user creds

## Demo flow (3 minutes)

| Time  | Screen / surface              | Action                                                              | Spoken line                                                                                       |
|-------|-------------------------------|---------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| 0:00  | Laptop dashboard              | Already signed in, history visible                                  | "This is the Guardian Dashboard — what a parent or responder sees."                              |
| 0:15  | Phone — Home                  | Walk through Home, show contacts in Family tab, show Sound Alarm card | "On the user's side: emergency circle, manual SOS, sound alarm — all in one tap."                |
| 0:35  | Phone — Vitals                | Wear watch on wrist, show live HR + ECG line                       | "The watch streams heart rate and motion at 1 Hz over the Wear Data Layer."                      |
| 0:55  | Phone — Info (About)          | Briefly scroll through Limitations section                          | "We're transparent about what's heuristic vs ML, hardware limits, and ethical limits."           |
| 1:15  | Watch + activity              | Run in place + shake watch hard for ~12 sec                        | "I'm simulating a struggle — elevated HR plus erratic motion."                                   |
| 1:30  | Watch — SOS countdown         | Countdown appears, do NOT tap YES                                  | "User has 15 seconds to cancel a false positive."                                                |
| 1:45  | Phone — Active Alert          | Alert screen appears with pulsing red ring; WhatsApp opens         | "On timeout, the alert fan-outs to emergency contacts via WhatsApp with live location."          |
| 2:00  | Laptop dashboard              | Banner lights up red, map pin, HR chart                            | "Simultaneously the Guardian Dashboard receives the alert in real time."                         |
| 2:25  | Laptop dashboard              | After 30 sec, audio play button appears                            | "30 seconds of audio evidence is auto-uploaded — base64-inlined in the alert document."          |
| 2:45  | Laptop dashboard              | Click "Mark resolved"                                              | "When safe, the alert is resolved — and that propagates back to the user's phone."               |
| 2:55  | —                             | Q&A buffer                                                         | (See anticipated Q&A below.)                                                                      |

## Anticipated Q&A

**Q: How accurate is your ML model?**
> "On WESAD held-out subjects, our trained model targets F1 ≥ 0.65 for the stress class. We use it OR-gated with a heuristic — either path can fire — and the 15-sec countdown protects against false positives. The committed binary is currently a stub trained on synthetic data; swapping in the WESAD-trained binary is a one-script change and the inference path is identical."

**Q: What about EDA (electrodermal activity)?**
> "It's in our research design but Fossil Gen 5 doesn't expose a GSR sensor. Listed as future work — would need an Empatica E4 or similar wearable."

**Q: How is this different from existing panic-button apps?**
> "Passive detection. The user doesn't need to be conscious or able to press a button. The watch detects elevated HR + erratic motion sustained for 10 seconds and triggers automatically. Manual SOS is the override path, not the primary."

**Q: Privacy?**
> "Heart-rate samples never leave the watch except in the 60-second window leading up to a confirmed alert. Audio uploads only when an alert fires. Firestore security rules scope every document to the owning user."

**Q: Why WhatsApp?**
> "Free, works on every Indian smartphone, judges see the message land in real time during the demo. SMS via SMSManager has been restricted by Google for new apps; Twilio adds setup friction. WhatsApp deep-link is the cleanest path for an MVP."

**Q: Can law enforcement actually use this?**
> "The Guardian Dashboard *simulates* what a responder would see — live map, biometric history, audio evidence. Real police integration requires department APIs we don't have. We've built the architecture so a future production version can plug in."

## Failure-mode contingencies

| If this fails                              | Fall back to                                                                          |
|--------------------------------------------|---------------------------------------------------------------------------------------|
| Bluetooth pairing fails                    | Use phone's long-press SOS — same alert pipeline, no watch needed                     |
| Watch sensor permission reset              | Re-grant in Watch Settings → Apps → VSafe → Permissions                              |
| GPS lock takes too long                    | Alert still fires; mention "fallback to last known location, ±100 m accuracy"        |
| WhatsApp not installed on contact's phone  | Skip; dashboard still updates — say "this would normally land in WhatsApp"           |
| Dashboard fails to load                    | Pre-recorded backup video on phone (`/sdcard/Movies/heysafe-demo.mp4`)                |
| Firestore connection slow                  | Pre-cache the active alert in browser tab (already-open tab keeps listener warm)     |
| Watch battery dies mid-demo                | Backup video; explain demo would normally use watch                                   |
| Mobile hotspot drops                       | Most things still work locally; dashboard won't update — fall back to logcat output  |
| Audio recording fails (permission denied)  | Alert still fires without audio; mention in About screen this is the documented behavior |

## Recovery if demo derails

1. Stay calm. Pause for 2 seconds.
2. Switch to backup video — *"For time, let me show the recorded version of this flow"* — then play `/sdcard/Movies/heysafe-demo.mp4`.
3. Continue narration over the video so judges still hear your story.

## Post-demo cleanup (optional)

- Resolve any active alerts in Firestore so the next dry-run starts clean
- Clear the audio playback element in dashboard
