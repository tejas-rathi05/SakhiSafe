import { getApps, getApp, initializeApp } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-app.js";
import { getAuth, signOut } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-auth.js";
import {
  getFirestore,
  collection,
  query,
  where,
  orderBy,
  onSnapshot,
  doc,
  updateDoc,
  addDoc,
  serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.13.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyATStY2eljhBrYZTABxLY_NcSIAGPZ3HrY",
  authDomain: "heysafe-demo-4bca5.firebaseapp.com",
  projectId: "heysafe-demo-4bca5",
  storageBucket: "heysafe-demo-4bca5.firebasestorage.app",
  messagingSenderId: "1067270702474",
  appId: "1:1067270702474:web:9770290964c7565c347473",
};
const app = getApps().length ? getApp() : initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

document.getElementById("logoutBtn").addEventListener("click", () => signOut(auth));

// ---- Browser notifications + alert sound ----
let notificationsEnabled = false;
if ("Notification" in window) {
  Notification.requestPermission().then(p => {
    notificationsEnabled = p === "granted";
    const badge = document.getElementById("liveBadge");
    if (badge) badge.textContent = notificationsEnabled ? "Live monitoring · alerts on" : "Live monitoring · connected";
  });
}

let audioCtxBeep;
function playAlertChime() {
  try {
    if (!audioCtxBeep) audioCtxBeep = new (window.AudioContext || window.webkitAudioContext)();
    const ctx = audioCtxBeep;
    const now = ctx.currentTime;
    [880, 1320, 880].forEach((freq, i) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(freq, now + i * 0.18);
      gain.gain.setValueAtTime(0, now + i * 0.18);
      gain.gain.linearRampToValueAtTime(0.25, now + i * 0.18 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + i * 0.18 + 0.16);
      osc.connect(gain).connect(ctx.destination);
      osc.start(now + i * 0.18);
      osc.stop(now + i * 0.18 + 0.18);
    });
  } catch {}
}

const seenAlertIds = new Set();
function notifyNewAlert(a) {
  if (notificationsEnabled) {
    const n = new Notification("🚨 VSafe — SOS Alert", {
      body: `${a.userName || "user"} · ${a.triggerSource || "manual"} trigger`,
      tag: "heysafe-alert",
      requireInteraction: true,
    });
    n.onclick = () => { window.focus(); n.close(); };
  }
  playAlertChime();
}

// ---- Map ----
let map, marker;
function ensureMap() {
  if (map) return;
  map = L.map("map", { zoomControl: false, attributionControl: false }).setView([20, 78], 5);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(map);
}

// ---- HR Chart ----
let chart;
function ensureChart(data) {
  const ctx = document.getElementById("hrChart").getContext("2d");
  const gradient = ctx.createLinearGradient(0, 0, 0, 140);
  gradient.addColorStop(0, "rgba(225,29,72,0.30)");
  gradient.addColorStop(1, "rgba(225,29,72,0.02)");
  if (chart) {
    chart.data.labels = data.map((_, i) => i);
    chart.data.datasets[0].data = data;
    chart.data.datasets[0].backgroundColor = gradient;
    chart.update();
    return;
  }
  chart = new Chart(ctx, {
    type: "line",
    data: {
      labels: data.map((_, i) => i),
      datasets: [{
        label: "HR",
        data,
        borderColor: "#E11D48",
        backgroundColor: gradient,
        tension: 0.35,
        pointRadius: 0,
        borderWidth: 2,
        fill: true,
      }],
    },
    options: {
      animation: { duration: 600, easing: "easeOutQuart" },
      plugins: { legend: { display: false }, tooltip: { mode: "index", intersect: false } },
      scales: {
        x: { display: false },
        y: {
          ticks: { color: "#64748B", font: { size: 11 } },
          grid: { color: "rgba(15,23,42,0.06)" },
        },
      },
    },
  });
}

// ---- Stats cards ----
function updateStats(docs) {
  const now = Date.now();
  const dayAgo = now - 24 * 60 * 60 * 1000;
  const weekAgo = now - 7 * 24 * 60 * 60 * 1000;

  let activeCount = 0;
  let todayCount = 0;
  let resolvedWeekCount = 0;
  const recentHrSamples = [];

  docs.forEach(d => {
    const a = d.data();
    const t = a.createdAt?.toMillis?.() || 0;
    if (a.status === "active") activeCount += 1;
    if (t >= dayAgo) todayCount += 1;
    if (a.status !== "active" && t >= weekAgo) resolvedWeekCount += 1;
    if (t >= weekAgo && a.hrWindow?.length) {
      const valid = a.hrWindow.filter(v => v > 30 && v < 220);
      recentHrSamples.push(...valid);
    }
  });

  const $ = id => document.getElementById(id);
  $("statActive").textContent = activeCount;
  $("statActiveMeta").textContent = activeCount === 0
    ? "No active emergencies"
    : (activeCount === 1 ? "1 emergency in progress" : `${activeCount} emergencies in progress`);
  $("statToday").textContent = todayCount;
  $("statResolved").textContent = resolvedWeekCount;

  if (recentHrSamples.length) {
    const avg = Math.round(recentHrSamples.reduce((a, b) => a + b, 0) / recentHrSamples.length);
    $("statAvgHr").textContent = avg;
  } else {
    $("statAvgHr").textContent = "—";
  }
}

function setHrStats(arr) {
  const wrap = document.getElementById("hrStats");
  if (!arr?.length) { wrap.style.display = "none"; return; }
  const valid = arr.filter(v => v > 30 && v < 220);
  if (!valid.length) { wrap.style.display = "none"; return; }
  const avg = Math.round(valid.reduce((a, b) => a + b, 0) / valid.length);
  const peak = Math.round(Math.max(...valid));
  wrap.style.display = "flex";
  document.getElementById("hrAvg").textContent = avg;
  document.getElementById("hrPeak").textContent = peak;
  document.getElementById("hrCount").textContent = valid.length;
}

// ---- Audio waveform from base64 m4a ----
let audioCtx;
async function drawWaveform(base64) {
  const canvas = document.getElementById("audioWave");
  const ctx2d = canvas.getContext("2d");
  const w = canvas.width = canvas.clientWidth * (window.devicePixelRatio || 1);
  const h = canvas.height = 48 * (window.devicePixelRatio || 1);
  ctx2d.clearRect(0, 0, w, h);
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const bin = Uint8Array.from(atob(base64), c => c.charCodeAt(0));
    const audio = await audioCtx.decodeAudioData(bin.buffer.slice(0));
    const data = audio.getChannelData(0);
    const bars = 96;
    const block = Math.floor(data.length / bars);
    const grad = ctx2d.createLinearGradient(0, 0, w, 0);
    grad.addColorStop(0, "#ff6b6b");
    grad.addColorStop(1, "#E53935");
    ctx2d.fillStyle = grad;
    for (let i = 0; i < bars; i++) {
      let max = 0;
      for (let j = 0; j < block; j++) max = Math.max(max, Math.abs(data[i * block + j] || 0));
      const bh = Math.max(2, max * h * 0.9);
      const x = (i / bars) * w;
      const bw = (w / bars) * 0.7;
      ctx2d.fillRect(x, (h - bh) / 2, bw, bh);
    }
  } catch (e) {
    // Decoding can fail (browser format support varies). Fall back to flat-line silence.
    ctx2d.fillStyle = "rgba(255,255,255,0.15)";
    ctx2d.fillRect(0, h / 2 - 1, w, 2);
  }
}

// ---- Latest alert renderer ----
function renderLatest(alertDoc) {
  const a = alertDoc.data();
  const el = document.getElementById("active");
  el.style.display = "block";
  const isActive = a.status === "active";
  el.classList.toggle("alert-card", true);
  el.classList.toggle("resolved", !isActive);

  const stateLabel = isActive ? "Active SOS" : "Resolved";
  document.getElementById("activeTitle").textContent =
    `${stateLabel} — ${a.userName || "user"}`;
  const started = a.createdAt?.toDate?.()?.toLocaleString() || "—";
  const resolved = a.resolvedAt?.toDate?.()?.toLocaleString();
  const sourceTag = a.triggerSource ? ` · trigger: ${a.triggerSource}` : "";
  document.getElementById("activeMeta").textContent =
    (resolved ? `Started ${started} · Resolved ${resolved}` : `Started ${started}`) + sourceTag;

  const mapsBtn = document.getElementById("mapsBtn");
  if (a.location) {
    ensureMap();
    const ll = [a.location.lat, a.location.lng];
    if (marker) marker.remove();
    marker = L.circleMarker(ll, {
      radius: 10,
      color: isActive ? "#E53935" : "#34C759",
      fillColor: isActive ? "#E53935" : "#34C759",
      fillOpacity: 0.9,
      weight: 3,
    }).addTo(map);
    map.setView(ll, 16);
    mapsBtn.style.display = "inline-flex";
    mapsBtn.href = `https://www.google.com/maps/search/?api=1&query=${ll[0]},${ll[1]}`;
    mapsBtn.textContent = "Open in Maps";
  } else {
    mapsBtn.style.display = "none";
  }

  if (a.hrWindow?.length) {
    ensureChart(a.hrWindow);
    setHrStats(a.hrWindow);
  } else {
    setHrStats([]);
  }

  const audioEl = document.getElementById("audio");
  const audioWrap = document.getElementById("audioWrap");
  if (a.audioBase64) {
    audioEl.src = "data:audio/mp4;base64," + a.audioBase64;
    audioWrap.classList.add("visible");
    drawWaveform(a.audioBase64);
  } else {
    audioEl.removeAttribute("src");
    audioWrap.classList.remove("visible");
  }

  const callBtn = document.getElementById("callBtn");
  callBtn.onclick = () => {
    const phone = (a.contactsNotified && a.contactsNotified[0]) || "";
    if (phone) window.location.href = `tel:${phone}`;
  };
  callBtn.style.display = (a.contactsNotified?.length) ? "inline-flex" : "none";

  const resolveBtn = document.getElementById("resolveBtn");
  if (isActive) {
    resolveBtn.style.display = "inline-flex";
    resolveBtn.onclick = async () => {
      resolveBtn.disabled = true;
      resolveBtn.textContent = "Resolving…";
      await updateDoc(doc(db, "alerts", alertDoc.id), {
        status: "resolved",
        resolvedAt: serverTimestamp(),
      });
      resolveBtn.disabled = false;
      resolveBtn.textContent = "Mark resolved";
    };
  } else {
    resolveBtn.style.display = "none";
  }
}

// ---- History ----
let selectedAlertId = null;
function renderHistory(snap) {
  const div = document.getElementById("history");
  div.innerHTML = "";
  if (snap.empty) {
    div.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🛡️</div>No alerts yet. Trigger one from the watch or use the Simulate button.</div>';
    return;
  }
  snap.forEach(d => {
    const a = d.data();
    const row = document.createElement("div");
    row.className = "history-row";
    const time = a.createdAt?.toDate?.()?.toLocaleString() || "";
    const cls = a.status === "active" ? "pill-active" : "pill-resolved";
    row.innerHTML = `
      <div class="history-left">
        <span class="history-name">${a.userName || "user"}</span>
        <span class="history-time">${time}${a.triggerSource ? " · " + a.triggerSource : ""}</span>
      </div>
      <span class="pill ${cls}">${a.status}</span>`;
    row.addEventListener("click", () => {
      selectedAlertId = d.id;
      renderLatest(d);
      document.getElementById("active").scrollIntoView({ behavior: "smooth", block: "start" });
    });
    div.appendChild(row);
  });
}

// ---- Demo simulator ----
const simulateBtn = document.getElementById("simulateBtn");
if (simulateBtn) {
  simulateBtn.addEventListener("click", async () => {
    simulateBtn.classList.add("firing");
    simulateBtn.querySelector(".fab-label").textContent = "Sending…";
    const hr = Array.from({ length: 60 }, (_, i) => 78 + 22 * Math.sin(i / 5) + Math.random() * 6);
    const motion = Array.from({ length: 60 }, () => Math.random() * 0.4);
    try {
      await addDoc(collection(db, "alerts"), {
        userId: "demo",
        userName: auth.currentUser?.email || "demo user",
        triggerSource: "demo",
        status: "active",
        createdAt: serverTimestamp(),
        location: { lat: 28.6139, lng: 77.2090, accuracy: 12 },
        hrWindow: hr,
        motionWindow: motion,
        contactsNotified: ["+91 99999 99999"],
        audioBase64: null,
      });
    } finally {
      setTimeout(() => {
        simulateBtn.classList.remove("firing");
        simulateBtn.querySelector(".fab-label").textContent = "Simulate";
      }, 1200);
    }
  });
}

// ---- Trip subscription ----
let activeTrip = null;
function renderTrip() {
  const card = document.getElementById("trip");
  if (!activeTrip) { card.style.display = "none"; return; }
  const total = (activeTrip.deadlineAt - (activeTrip.startedAtMs || activeTrip.deadlineAt));
  const remaining = activeTrip.deadlineAt - Date.now();
  if (remaining <= 0) { card.style.display = "none"; activeTrip = null; return; }
  const elapsed = total - remaining;
  const progress = Math.max(0, Math.min(1, elapsed / total));
  const overdueSoon = remaining < 60_000;
  card.style.display = "block";
  card.classList.toggle("overdue-soon", overdueSoon);
  document.getElementById("tripTitle").textContent =
    `${activeTrip.userName || "user"} → ${activeTrip.destinationLabel}`;
  document.getElementById("tripMeta").textContent =
    overdueSoon ? "Auto-SOS imminent if no check-in" : "Will SOS automatically if not checked in";
  const mm = Math.floor(remaining / 60_000);
  const ss = Math.floor((remaining / 1000) % 60);
  document.getElementById("tripCountdown").textContent = `${mm}:${String(ss).padStart(2, "0")}`;
  document.getElementById("tripProgress").style.width = `${progress * 100}%`;
}
setInterval(renderTrip, 1_000);

const tripsQ = query(collection(db, "trips"), where("status", "==", "active"));
onSnapshot(tripsQ, snap => {
  if (snap.empty) { activeTrip = null; renderTrip(); return; }
  // Use the most recent active trip.
  const sorted = snap.docs.slice().sort((a, b) => (b.data().deadlineAt || 0) - (a.data().deadlineAt || 0));
  const d = sorted[0].data();
  activeTrip = {
    destinationLabel: d.destinationLabel,
    userName: d.userName,
    deadlineAt: d.deadlineAt,
    startedAtMs: d.startedAt?.toMillis?.() || (d.deadlineAt - 30 * 60_000),
  };
  renderTrip();
});

// ---- Live subscription ----
let firstSnapshot = true;
const q = query(collection(db, "alerts"), orderBy("createdAt", "desc"));
onSnapshot(q, snap => {
  // Detect newly active alerts (skip ones we've already seen, and the initial backfill)
  snap.docChanges().forEach(change => {
    if (change.type === "added" && change.doc.data().status === "active" && !seenAlertIds.has(change.doc.id)) {
      seenAlertIds.add(change.doc.id);
      if (!firstSnapshot) notifyNewAlert(change.doc.data());
    } else if (change.type === "added") {
      seenAlertIds.add(change.doc.id);
    }
  });
  firstSnapshot = false;

  updateStats(snap.docs);
  renderHistory(snap);
  const active = snap.docs.find(d => d.data().status === "active");
  const selected = selectedAlertId ? snap.docs.find(d => d.id === selectedAlertId) : null;
  const latest = active || selected || snap.docs[0];
  if (latest) renderLatest(latest);
  else document.getElementById("active").style.display = "none";
});
