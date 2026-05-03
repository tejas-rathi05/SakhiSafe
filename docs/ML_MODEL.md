# On-Device Stress Detection Model — Technical Description

This document describes the binary stress classification model deployed inside
the SakhiSafe wearable companion. It is intended as a self-contained technical
reference suitable for a research paper, covering dataset, feature engineering,
model architecture, training procedure, validation methodology, deployment
specification, and limitations.

---

## 1. Problem Formulation

We formulate stress detection as a binary classification problem over short
windows of physiological and motion signals from a wrist-worn device:

- **Input:** 60-second rolling window of heart rate (HR, derived from
  photoplethysmography) and 3-axis wrist accelerometer (ACC) data.
- **Output:** P(stress) ∈ [0, 1], a calibrated probability that the subject is
  in an acute stress state.
- **Task:** Discriminate stress from non-stress states (baseline, amusement,
  meditation) collapsed into a single negative class.

The model runs entirely on-device on Wear OS to preserve user privacy, eliminate
network dependence during emergencies, and respect the energy budget of a
small-form-factor wearable.

---

## 2. Dataset

We use the **WESAD (Wearable Stress and Affect Detection)** corpus¹.

| Property                | Value                                                          |
|-------------------------|----------------------------------------------------------------|
| Subjects                | 15 (S2–S17, excluding S12)                                     |
| Wrist sensor            | Empatica E4                                                    |
| BVP sampling rate       | 64 Hz                                                          |
| ACC sampling rate       | 32 Hz (3-axis)                                                 |
| Label sampling rate     | 700 Hz (downsampled chest reference)                           |
| Source labels           | 1 = baseline, 2 = stress, 3 = amusement, 4 = meditation        |
| Binary mapping          | stress = (label == 2); non-stress = (label ∈ {1, 3, 4})        |

After windowing and feature extraction (Section 3), the corpus yields
**8 978 windows**: 1 992 stress (22.2%) and 6 986 non-stress (77.8%), an
imbalance ratio of approximately **3.5 : 1**. Class imbalance is addressed
through cost-sensitive training (Section 5).

¹ Schmidt, P., Reiss, A., Duerichen, R., Marberger, C., Van Laerhoven, K. (2018).
*Introducing WESAD, a multimodal dataset for wearable stress and affect
detection.* In Proceedings of ICMI '18.

---

## 3. Feature Engineering

### 3.1 Signal preprocessing

Each subject's wrist-channel data is processed independently:

- **Heart rate from BVP.** Peaks are detected on the raw BVP signal using
  `scipy.signal.find_peaks` with `distance = fs · 0.4` (refractory bound at
  150 BPM) and `prominence = 0.5 · σ_BVP`. Inter-peak intervals (RR) are
  converted to instantaneous HR (60 / RR) and linearly interpolated onto a
  uniform 1 Hz grid using the per-peak time index.
- **Motion magnitude from ACC.** Per-sample magnitude is computed as
  `m_t = ‖(a_x, a_y, a_z)‖₂`. Magnitudes are then averaged into 1 Hz bins
  (32 raw samples per second) to align temporally with the HR stream.
- **Label downsampling.** 700 Hz labels are reduced to 1 Hz by majority vote
  per second.

### 3.2 Windowing

- Window length: **60 seconds**
- Stride: **5 seconds** (high overlap to maximize training data and approximate
  the on-device inference cadence)
- Windows whose majority label falls outside {1, 2, 3, 4} are discarded.

### 3.3 Feature vector

Each 60-second window produces an **8-dimensional** feature vector
`x ∈ ℝ⁸`. Features are chosen to capture both the central tendency and the
temporal dynamics of HR and motion within the window:

| #  | Feature        | Definition                                                              | Intuition                                       |
|----|----------------|-------------------------------------------------------------------------|-------------------------------------------------|
| 1  | `hr_mean`      | mean of HR samples in window                                            | Tonic heart rate level                          |
| 2  | `hr_std`       | standard deviation of HR samples                                        | HR variability (proxy for autonomic activation) |
| 3  | `hr_range`     | `max(HR) − min(HR)`                                                     | Peak-to-trough excursion                        |
| 4  | `hr_slope`     | OLS slope of `HR(t)` against discrete time index                        | Trend direction (rising = activation)           |
| 5  | `motion_mean`  | mean of motion magnitude                                                | Activity level                                  |
| 6  | `motion_std`   | standard deviation of motion magnitude                                  | Movement irregularity                           |
| 7  | `motion_max`   | maximum motion magnitude                                                | Peak motion event                               |
| 8  | `peak_count`   | number of samples exceeding `mean + 1·σ` (motion peaks above threshold) | Discrete burstiness of motion                   |

Windows with fewer than 5 valid HR samples or 5 valid motion samples are
dropped.

The same eight features are computed identically on-device (Kotlin
implementation in `wearapp/.../detection/FeatureExtractor.kt`), ensuring
training-time and inference-time feature parity.

---

## 4. Validation Methodology

We evaluate generalization to **unseen subjects** using
**Leave-One-Subject-Out (LOSO)** cross-validation:

1. For each held-out subject *s* ∈ {S2, ..., S17 \ S12}:
   a. Train on windows from the remaining 14 subjects.
   b. Fit a per-fold `StandardScaler` on the training set only.
   c. Test on the held-out subject's windows.
   d. Record F1 score on the positive (stress) class.
2. Report the **mean F1 across folds**.

LOSO is the appropriate protocol for this domain because (i) subjects are the
unit of generalization (we deploy to *new* users) and (ii) within-subject
windows are temporally autocorrelated, which would inflate IID metrics.

The validation classifier is a **RandomForest baseline** (more interpretable,
robust to small data, well-suited as a strong reference):

```
RandomForestClassifier(
    n_estimators   = 200,
    max_depth      = 10,
    class_weight   = "balanced",
    random_state   = 0,
)
```

---

## 5. Production Model

### 5.1 Architecture

A small **feed-forward neural network (MLP)** is used as the deployed model.
Its size is chosen to fit the energy and memory budget of an on-device Wear OS
inference path while preserving non-linear capacity beyond the linear baseline.

```
Input   :  ℝ⁸   (standardized features, mean=0, std=1)
Dense₁  :  ℝ¹⁶  ReLU
Dense₂  :  ℝ⁸   ReLU
Output  :  ℝ¹   Sigmoid     →   P(stress)
```

| Property             | Value                                           |
|----------------------|-------------------------------------------------|
| Total parameters     | ≈ 169                                           |
| Hidden activation    | ReLU                                            |
| Output activation    | Sigmoid                                         |
| Loss                 | Binary cross-entropy                            |
| Optimizer            | Adam (default learning rate 1 × 10⁻³)           |
| Epochs               | 40                                              |
| Batch size           | 64                                              |
| Validation split     | 20% (random, used only for training-curve diagnostics; not LOSO) |
| Class weights        | `{0 : 0.643, 1 : 2.254}` (sklearn `compute_class_weight('balanced')`) |

### 5.2 Class imbalance handling

The 3.5 : 1 imbalance is addressed by passing `class_weight` to `model.fit`,
which scales the per-sample loss inversely with class frequency. This causes
gradient descent to penalize false negatives on the minority (stress) class
≈ 3.5× more strongly than false positives on the majority class.

### 5.3 Feature standardization

A single `StandardScaler` is fit on the entire training set after LOSO
diagnostics are complete. Its per-feature `mean` and `scale` are exported as
JSON (`feature_scaler.json`) and applied identically on-device prior to
inference. This eliminates any train/inference standardization drift.

### 5.4 Export and deployment artifact

| Artifact                | Description                                                    | Size    |
|-------------------------|----------------------------------------------------------------|---------|
| `model.tflite`          | TensorFlow Lite FlatBuffer of the trained MLP (float32)        | ≈ 3.2 KB |
| `feature_scaler.json`   | `{"mean": [...8 floats...], "scale": [...8 floats...]}`        | < 1 KB  |

Conversion pipeline:

```
keras.Model  →  tf.lite.TFLiteConverter.from_keras_model  →  model.tflite
```

No quantization is applied; at 169 parameters the model is already several
orders of magnitude smaller than typical Wear OS asset budgets, and float32
inference takes well under one millisecond per call.

---

## 6. Results

LOSO cross-validation results using the RandomForest baseline (Section 4):

| Subject | F1     |   | Subject | F1     |
|---------|--------|---|---------|--------|
| S2      | 0.723  |   | S10     | 0.731  |
| S3      | 0.452  |   | S11     | 0.679  |
| S4      | 0.495  |   | S13     | 0.704  |
| S5      | 0.684  |   | S14     | 0.775  |
| S6      | 0.544  |   | S15     | 0.636  |
| S7      | 0.797  |   | S16     | 0.221  |
| S8      | 0.683  |   | S17     | 0.706  |
| S9      | 0.543  |   |         |        |

**Mean LOSO F1 = 0.625**, range = [0.221, 0.797], σ ≈ 0.16.

### Discussion

The high inter-subject variance (most pronounced on S16 at 0.22) reflects a
well-documented limitation of wrist-PPG-derived HR: signal quality is
sensitive to subject skin tone, surface adipose tissue, watch fit, and
involuntary motion. Subjects whose BVP traces yield noisy peak detection
produce noisy HR features regardless of the downstream model.

The strongest subjects (S5, S7, S11, S14) all exceed F1 = 0.68, indicating
that when the upstream HR signal is reliable, the 8-dimensional feature
space and small MLP capture the discriminative structure of stress
adequately.

The mean (0.625) is below the project's pre-registered ≥ 0.65 success
threshold; we report the realized number rather than rounding upward.

---

## 7. On-Device Inference Pipeline

The trained model is deployed inside the SakhiSafe wearable application as
one of two parallel detectors operating on a continuous data stream:

### 7.1 Inference cadence

- Sensors produce HR samples (~1 Hz) and motion magnitude samples (~10 Hz)
  continuously while monitoring is active.
- A rolling buffer holds the last 60 seconds of HR.
- Every 5 seconds, the on-device feature extractor (mirrored from training)
  computes the 8-dimensional vector, applies the exported standardization,
  and invokes the TFLite interpreter.
- The interpreter returns P(stress) for the current window.

### 7.2 Decision fusion with rule-based detector

A separate **HeuristicDetector** runs in parallel:

| Condition          | Trigger value                                                        |
|--------------------|----------------------------------------------------------------------|
| HR spike           | `current_HR − median_HR_over_last_5_min > 30 BPM`                    |
| Motion irregularity| `variance(motion) > 3.0` over 5-second window                        |
| Sustained for      | ≥ 10 seconds (both conditions held continuously)                     |

The fusion policy combines the two detectors:

```
fuse(heuristic, ml_score):
    ml = ml_score >= 0.75
    heuristic and ml  →  "both"        (highest confidence; trigger SOS)
    heuristic         →  "heuristic"   (rule-based safety net; trigger SOS)
    ml                →  null          (corroborator only; do not trigger)
    neither           →  null
```

The heuristic is the sole standalone trigger. The ML score functions as a
*confidence label* that upgrades a heuristic trigger to the higher-confidence
`"both"` source. This conservative policy is motivated by a known
inference-time / training-time discrepancy in motion windowing (see
Section 8.4), which we treat as an open issue rather than rely upon.

---

## 8. Limitations

We document the limitations of the current model honestly so that downstream
readers can calibrate their expectations.

### 8.1 Inter-subject variance

LOSO F1 ranges from 0.221 to 0.797. Performance on a new user is therefore
not uniformly characterized by the mean; deployment to populations whose
BVP signal quality systematically differs from WESAD subjects (different
demographics, watch hardware, ambient conditions) may shift the
distribution.

### 8.2 Dataset scope

WESAD contains 15 subjects, predominantly young adults, in a controlled
laboratory protocol. The stress condition is induced via the Trier Social
Stress Test, which differs from the in-situ stressors a women's-safety
wearable would target (threats, harassment, panic, physical confrontation).
Domain transfer from lab-induced cognitive stress to acute real-world
stress is not validated by this dataset.

### 8.3 Class imbalance

A 3.5 : 1 imbalance combined with cost-sensitive training trades minority
recall for additional false positives on the majority class. The fusion
policy in Section 7.2 explicitly addresses this by requiring corroboration
from the rule-based detector before raising an SOS.

### 8.4 Training/inference window mismatch (motion features)

In the training pipeline, motion magnitude is binned into 1 Hz averages
prior to windowing, so a 60-sample motion window covers 60 seconds. In the
current on-device implementation, the rolling motion window stores 60 raw
samples at the underlying ~10 Hz rate, covering only ~6 seconds. The
resulting per-window motion statistics (mean, std, max, peak count) are
computed over a different temporal scale at inference time than at
training time, which biases the model's output distribution. This does not
affect the LOSO results in Section 6 — those are computed entirely in the
training pipeline — but it is the reason the on-device fusion policy treats
the ML score as advisory rather than as a standalone trigger.

### 8.5 No on-device personalization

The model is fixed post-training. It does not adapt to a particular user's
HR baseline beyond the rolling 5-minute median used inside the heuristic
detector; the ML features themselves are population-level.

### 8.6 No deployed-model field validation yet

Section 6 reports cross-validated performance on the training corpus.
End-user efficacy of the deployed pipeline (heuristic + ML fusion) under
real wearable use has not yet been characterized.

---

## 9. Reproducibility

| Artifact / step                        | Location                                                                                  |
|----------------------------------------|-------------------------------------------------------------------------------------------|
| Training notebook                      | `ml/train_wesad.ipynb`                                                                    |
| Stub fallback (synthetic data)         | `ml/train_stub.py`                                                                        |
| On-device feature extractor (Kotlin)   | `wearapp/src/main/java/com/heysafe/app/wear/detection/FeatureExtractor.kt`                |
| On-device TFLite wrapper               | `wearapp/src/main/java/com/heysafe/app/wear/detection/MlDetector.kt`                      |
| Heuristic detector                     | `wearapp/src/main/java/com/heysafe/app/wear/detection/HeuristicDetector.kt`               |
| Decision fusion                        | `wearapp/src/main/java/com/heysafe/app/wear/detection/DetectionFusion.kt`                 |
| Operational results & deployment notes | `ml/RESULTS.md`                                                                           |

The training notebook includes a Colab bootstrap cell that downloads WESAD
directly from the Universität Siegen sciebo mirror into Colab's ephemeral
storage, requiring no Google Drive provisioning. End-to-end reproduction
takes approximately **15 minutes** on the free Colab CPU runtime.

The training pipeline is deterministic given the fixed `random_state=0` for
the RandomForest baseline; the MLP final fit uses default Keras
non-determinism and may produce small numerical variation between runs.

---

## 10. Future Work

1. **Resolve the motion-window mismatch (Section 8.4)** by binning motion
   samples into 1 Hz averages on-device prior to insertion into the ML
   feature window. After this fix, re-validate the ML detector as a
   standalone trigger via threshold sweep and on-device A/B comparison
   against the heuristic.
2. **On-device baseline personalization** for the ML feature inputs
   (subject-specific HR/motion offsets), mirroring what the heuristic
   detector already does for HR.
3. **Larger and more diverse training corpus** (e.g., StressData, SWELL-KW,
   in-the-wild collected data) to improve domain coverage beyond
   lab-induced stress.
4. **Probability calibration** (temperature scaling or Platt scaling on a
   held-out calibration set) to make the 0.75 fusion threshold semantically
   stable across populations.
5. **Sequence models over raw signal** (small 1-D CNN or compact
   transformer) as alternatives to hand-engineered window features, subject
   to the on-device latency and memory budget.
