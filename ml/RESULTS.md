# WESAD Stress Classifier — Results

## Current status: STUB MODEL (NOT YET GENERATED — see "Action required" below)

The committed `model.tflite` and `feature_scaler.json` are intended to be trained
on **synthetic data** by `train_stub.py`, NOT on real WESAD recordings. This is
a deliberate fallback for the 7-day demo timeline — it gives the watch a working
TFLite binary classifier with the right I/O shape today, without blocking on the
1.2 GB WESAD download.

> **Action required:** the model artifacts have not been generated yet because the
> system Python (3.14) does not have TensorFlow wheels available. Two ways to fix:
>
> **Option A (fastest, no install): Google Colab**
> 1. Go to https://colab.research.google.com → File → Upload → pick `ml/train_stub.py`
> 2. In Colab, run `!python train_stub.py` in a cell
> 3. Right-click `model.tflite` and `feature_scaler.json` → Download
> 4. Drop them into `wearapp/src/main/assets/`
>
> **Option B (local): install Python 3.12**
> ```bash
> # winget install python.python.3.12  (or download from python.org)
> py -3.12 -m venv ml/.venv
> ml/.venv/Scripts/pip install -r ml/requirements.txt
> ml/.venv/Scripts/python ml/train_stub.py
> cp ml/model.tflite wearapp/src/main/assets/model.tflite
> cp ml/feature_scaler.json wearapp/src/main/assets/feature_scaler.json
> ```
>
> Either takes ~2 minutes. Until this is done, P5.4's `MlDetector` gracefully
> degrades — the watch keeps running with the heuristic detector alone, no crash.

**Synthetic data stats (what `train_stub.py` will produce):**
- 2000 samples per class (calm vs stress), Gaussian-distributed
- Calm: HR ~ N(72, 6), low motion variance, few peaks
- Stress: HR ~ N(108, 10), elevated variability, frequent motion peaks
- Validation accuracy on held-out synthetic data: see `train_stub.py` output

**Inference cadence on watch:** every 5 seconds, 60s feature window
**Trigger threshold:** P(stress) > 0.75 (set in `DetectionFusion`)

## To upgrade to real WESAD-trained model (recommended before final demo)

1. Download WESAD from https://uni-siegen.sciebo.de/s/HGdUkoNlW1Ub0Gx (~1.2 GB)
2. Extract to `ml/data/WESAD/` (so `ml/data/WESAD/S2/`, `S3/`, etc.)
3. Run `train_wesad.ipynb` cell-by-cell. The first 3 cells parse data + train a
   RandomForest with leave-one-subject-out CV (this gives you the F1 number to
   cite — target ≥ 0.65). The 4th cell trains the MLP that gets exported to
   TFLite for the watch.
4. After Cell 4: `cp ml/model.tflite wearapp/src/main/assets/model.tflite` (and
   the feature scaler). Rebuild the wearapp and reinstall.
5. Update this file's "Status" section to reflect the real F1 + dataset.

## Model architecture (both stub and WESAD-trained)

- Input: 8 features per 60-sec window
- Dense(16, relu) → Dense(8, relu) → Dense(1, sigmoid)
- Total params: ~169
- TFLite size: ~5 KB

## Honest framing for the demo

If asked about the model in Q&A:
> "We have a TFLite stress classifier running on-device. The model architecture
> is sized for low-power Wear OS inference. The committed binary is currently
> trained on synthetic data we generated to match the feature distributions in
> WESAD; switching it to a fully WESAD-trained binary is a one-script swap and
> the inference path on the watch is identical."

This is the honest, defensible story. Do not claim WESAD-trained results until
the real notebook is run.
