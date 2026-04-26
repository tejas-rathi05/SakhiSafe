import { getApps, getApp, initializeApp } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-app.js";
import { getAuth, signOut } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-auth.js";
import {
  getFirestore,
  collection,
  query,
  orderBy,
  onSnapshot,
  doc,
  updateDoc,
  serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.13.0/firebase-firestore.js";

// Reuse the app already initialized by auth.js (loaded first via the <script type="module">
// ordering in index.html). If for some reason it isn't ready, initialize a no-op fallback.
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

// ---- Map (Leaflet + OSM) ----
let map, marker;
function ensureMap() {
  if (map) return;
  map = L.map("map").setView([20, 78], 5);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: "© OpenStreetMap contributors",
  }).addTo(map);
}

// ---- HR Chart (Chart.js) ----
let chart;
function ensureChart(data) {
  const ctx = document.getElementById("hrChart").getContext("2d");
  if (chart) {
    chart.data.labels = data.map((_, i) => i);
    chart.data.datasets[0].data = data;
    chart.update();
    return;
  }
  chart = new Chart(ctx, {
    type: "line",
    data: {
      labels: data.map((_, i) => i),
      datasets: [{
        label: "HR (bpm)",
        data,
        borderColor: "#E53935",
        backgroundColor: "rgba(229,57,53,0.15)",
        tension: 0.3,
        pointRadius: 0,
        fill: true,
      }],
    },
    options: {
      animation: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { display: false },
        y: { ticks: { color: "#fff" }, grid: { color: "rgba(255,255,255,0.1)" } },
      },
    },
  });
}

// ---- Active alert renderer ----
function renderActive(alertDoc) {
  const a = alertDoc.data();
  const el = document.getElementById("active");
  el.style.display = "block";
  document.getElementById("activeTitle").textContent =
    `🚨 SOS from ${a.userName || "user"} (source: ${a.triggerSource || "unknown"})`;
  document.getElementById("activeMeta").textContent =
    `Started: ${a.createdAt?.toDate?.()?.toLocaleString() || "—"}`;

  if (a.location) {
    ensureMap();
    const ll = [a.location.lat, a.location.lng];
    if (marker) marker.remove();
    marker = L.marker(ll).addTo(map);
    map.setView(ll, 16);
  }

  if (a.hrWindow?.length) ensureChart(a.hrWindow);

  const audioEl = document.getElementById("audio");
  if (a.audioBase64) {
    audioEl.src = "data:audio/mp4;base64," + a.audioBase64;
    audioEl.style.display = "block";
  } else {
    audioEl.removeAttribute("src");
    audioEl.style.display = "none";
  }

  document.getElementById("resolveBtn").onclick = async () => {
    await updateDoc(doc(db, "alerts", alertDoc.id), {
      status: "resolved",
      resolvedAt: serverTimestamp(),
    });
  };
}

// ---- History renderer ----
function renderHistory(snap) {
  const div = document.getElementById("history");
  div.innerHTML = "";
  if (snap.empty) {
    div.innerHTML = '<p style="color:var(--muted)">No alerts yet.</p>';
    return;
  }
  snap.forEach(d => {
    const a = d.data();
    const row = document.createElement("div");
    row.className = "history-row";
    row.innerHTML = `
      <div>
        <strong>${a.userName || "user"}</strong>
        <span style="color:var(--muted); font-size:14px; margin-left:12px">
          ${a.createdAt?.toDate?.()?.toLocaleString() || ""}
        </span>
      </div>
      <span class="pill ${a.status === "active" ? "pill-active" : "pill-resolved"}">${a.status}</span>`;
    div.appendChild(row);
  });
}

// ---- Live subscription ----
const q = query(collection(db, "alerts"), orderBy("createdAt", "desc"));
onSnapshot(q, snap => {
  renderHistory(snap);
  const active = snap.docs.find(d => d.data().status === "active");
  if (active) renderActive(active);
  else document.getElementById("active").style.display = "none";
});
