import { initializeApp } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/10.13.0/firebase-auth.js";

const firebaseConfig = {
  apiKey: "AIzaSyATStY2eljhBrYZTABxLY_NcSIAGPZ3HrY",
  authDomain: "heysafe-demo-4bca5.firebaseapp.com",
  projectId: "heysafe-demo-4bca5",
  storageBucket: "heysafe-demo-4bca5.firebasestorage.app",
  messagingSenderId: "1067270702474",
  appId: "1:1067270702474:web:9770290964c7565c347473",
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

onAuthStateChanged(auth, user => {
  const path = location.pathname.split("/").pop() || "index.html";
  if (user && path === "login.html") location.href = "index.html";
  if (!user && (path === "index.html" || path === "")) location.href = "login.html";
});

const loginBtn = document.getElementById("loginBtn");
if (loginBtn) {
  loginBtn.addEventListener("click", async () => {
    const errorEl = document.getElementById("error");
    errorEl.textContent = "";
    loginBtn.disabled = true;
    loginBtn.textContent = "Signing in...";
    try {
      await signInWithEmailAndPassword(
        auth,
        document.getElementById("email").value.trim(),
        document.getElementById("password").value,
      );
    } catch (e) {
      errorEl.textContent = e.message;
      loginBtn.disabled = false;
      loginBtn.textContent = "Sign in";
    }
  });
}

// Export the initialized app + auth for index.html / app.js to reuse without re-init
window.__heysafe = { app, auth };
