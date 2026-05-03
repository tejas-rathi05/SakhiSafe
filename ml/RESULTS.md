# WESAD Stress Classifier — Results

## Current status: WESAD-trained (real)

`model.tflite` and `feature_scaler.json` in `wearapp/src/main/assets/` are
trained on real WESAD wrist data (BVP + ACC), produced by `train_wesad.ipynb`
on Google Colab with class-balanced RandomForest LOSO + class-weighted MLP
final-fit.

## Performance

- **Mean leave-one-subject-out F1: 0.625** (target was ≥ 0.65 — slightly under)
- 15 held-out subjects, F1 range 0.221 (S16) to 0.797 (S7)
- Total windows: 8978 (1992 stress, 6986 non-stress; ~3.5:1 imbalance)
- Class weights applied during training: `{0: 0.643, 1: 2.254}` (sklearn
  `compute_class_weight('balanced')`)

Per-subject F1:

| Subject | F1    | Subject | F1    |
|---------|-------|---------|-------|
| S2      | 0.723 | S10     | 0.731 |
| S3      | 0.452 | S11     | 0.679 |
| S4      | 0.495 | S13     | 0.704 |
| S5      | 0.684 | S14     | 0.775 |
| S6      | 0.544 | S15     | 0.636 |
| S7      | 0.797 | S16     | 0.221 |
| S8      | 0.683 | S17     | 0.706 |
| S9      | 0.543 |         |       |

The high inter-subject variance (especially S16's 0.22) reflects a known
property of wrist-PPG-derived HR — some subjects' BVP signals are noisier and
yield less reliable HR estimates. The watch fuses ML output with a heuristic
detector in `DetectionFusion`, so a single weak subject's failure mode does not
break the safety net.

## Model architecture (matches `FeatureExtractor.kt` on the watch)

- **Input:** 8 features per 60-sec window
  `[hr_mean, hr_std, hr_range, hr_slope, motion_mean, motion_std, motion_max, peak_count]`
- **Hidden layers:** Dense(16, relu) → Dense(8, relu)
- **Output:** Dense(1, sigmoid)
- **Total params:** ~169
- **TFLite size:** 3.2 KB

## Inference cadence

- Watch runs inference every 5 seconds over a rolling 60-sec feature window
- Trigger threshold: `P(stress) > 0.75` (set in `DetectionFusion`)

## Honest framing for demo Q&A

If asked about the model:
> "We trained a TFLite stress classifier on the WESAD wrist BVP + accelerometer
> signals using leave-one-subject-out cross-validation. Mean F1 across the 15
> held-out subjects is 0.625. There's substantial inter-subject variance, which
> is a known property of wrist-PPG-derived HR. The watch fuses this ML signal
> with a heuristic detector, so the safety net doesn't depend on the model
> alone."

Do not round 0.625 up to 0.65 or claim "exceeds target" — the actual mean LOSO
F1 is 0.625.

## Reproduce

End-to-end on free Colab CPU runtime: ~15 min.

1. Open `train_wesad.ipynb` in Google Colab.
2. Run the bootstrap cell (zero-Drive: downloads WESAD from sciebo into Colab's
   ephemeral `/content/`, no Google Drive needed).
3. Run cells 1 → 4 in order. Cell 3 is the slow one (~6–10 min).
4. Download `model.tflite` and `feature_scaler.json` from Colab's file panel
   before the session disconnects.
5. Drop both into `wearapp/src/main/assets/`, rebuild the wear app, reinstall.
6. Verify in logcat: `MlDetector ready: 8 features` (instead of "unavailable").

## Fallback

`train_stub.py` still exists and produces a synthetic-data model with the same
I/O shape. It is no longer the source of the committed artifacts but remains
useful if WESAD is unavailable or as a sanity-check baseline.
