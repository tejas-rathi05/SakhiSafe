# WESAD Stress Classifier — Results

(This file is populated by P5.2 after training. Placeholder for now.)

**Dataset:** WESAD (15 subjects, wrist-worn Empatica E4)
**Features:** 8 — HR mean/std/range/slope, ACC magnitude mean/std/max, peak count
**Window:** 60s, stride 5s
**Train (RF):** RandomForestClassifier (100 trees, depth 8)
**Eval:** Leave-one-subject-out cross-validation
**Mean F1 (stress class):** _<TBD — fill from notebook>_

**Deployment model:** Tiny MLP (16 → 8 → 1) trained on all data, exported to TFLite (~XX KB)
**Inference cadence on watch:** every 5 seconds, 60s feature window
**Trigger threshold:** P(stress) > 0.75
