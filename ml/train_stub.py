"""
Stub trainer for HeySafe MVP demo.

Trains a tiny MLP on SYNTHETIC stress-vs-calm samples so we have a working TFLite
model + scaler artifacts on day 1 — without requiring the WESAD dataset (1.2 GB).
The model has the same I/O shape as the real WESAD-trained model, so the watch
code is identical. When real WESAD training is run via train_wesad.ipynb,
model.tflite + feature_scaler.json overwrite this stub.

Features (must match FeatureExtractor on the watch):
  hr_mean, hr_std, hr_range, hr_slope, motion_mean, motion_std, motion_max, peak_count

Run: python train_stub.py
"""
import json
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import StandardScaler

rng = np.random.default_rng(42)
N = 2000  # synthetic samples per class

# "calm" — HR ~ 70 BPM, low motion
calm_hr_mean   = rng.normal(72,  6,  N)
calm_hr_std    = rng.normal(2,   0.7, N).clip(0)
calm_hr_range  = rng.normal(8,   3,  N).clip(0)
calm_hr_slope  = rng.normal(0,   0.05, N)
calm_mo_mean   = rng.normal(1.0, 0.5, N).clip(0)
calm_mo_std    = rng.normal(0.5, 0.2, N).clip(0)
calm_mo_max    = rng.normal(2.0, 0.8, N).clip(0)
calm_peak      = rng.poisson(2,  N).astype(float)

# "stress" — HR elevated + variable, motion erratic
stress_hr_mean   = rng.normal(108, 10, N)
stress_hr_std    = rng.normal(8,   3,  N).clip(0)
stress_hr_range  = rng.normal(28,  10, N).clip(0)
stress_hr_slope  = rng.normal(0.4, 0.2, N)
stress_mo_mean   = rng.normal(4.5, 1.5, N).clip(0)
stress_mo_std    = rng.normal(2.5, 1.0, N).clip(0)
stress_mo_max    = rng.normal(9.0, 3.0, N).clip(0)
stress_peak      = rng.poisson(12, N).astype(float)

X_calm = np.stack([calm_hr_mean, calm_hr_std, calm_hr_range, calm_hr_slope,
                   calm_mo_mean, calm_mo_std, calm_mo_max, calm_peak], axis=1)
X_stress = np.stack([stress_hr_mean, stress_hr_std, stress_hr_range, stress_hr_slope,
                     stress_mo_mean, stress_mo_std, stress_mo_max, stress_peak], axis=1)
X = np.concatenate([X_calm, X_stress], axis=0).astype(np.float32)
y = np.concatenate([np.zeros(N), np.ones(N)]).astype(np.float32)

# shuffle
idx = rng.permutation(len(X))
X = X[idx]; y = y[idx]

scaler = StandardScaler().fit(X)
Xs = scaler.transform(X).astype(np.float32)

mlp = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(8,)),
    tf.keras.layers.Dense(16, activation='relu'),
    tf.keras.layers.Dense(8, activation='relu'),
    tf.keras.layers.Dense(1, activation='sigmoid'),
])
mlp.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
mlp.fit(Xs, y, epochs=20, batch_size=64, validation_split=0.2, verbose=0)
loss, acc = mlp.evaluate(Xs, y, verbose=0)
print(f'Stub MLP val acc on synthetic data: {acc:.3f}  (loss {loss:.3f})')

converter = tf.lite.TFLiteConverter.from_keras_model(mlp)
tflite_model = converter.convert()
with open('model.tflite', 'wb') as f: f.write(tflite_model)
with open('feature_scaler.json', 'w') as f:
    json.dump({'mean': scaler.mean_.tolist(), 'scale': scaler.scale_.tolist()}, f)
print(f'Saved stub model.tflite ({len(tflite_model)/1024:.1f} KB) + feature_scaler.json')
