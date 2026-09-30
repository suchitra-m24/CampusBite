function cbUrlBase64ToUint8Array(base64String) {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) outputArray[i] = rawData.charCodeAt(i);
  return outputArray;
}

async function cbInitPushNotifications() {
  if (!("serviceWorker" in navigator) || !("PushManager" in window)) return;
  const user = CBAuth.getUser();
  if (!user || (user.role !== "STUDENT" && user.role !== "TEACHER")) return;

  try {
    const registration = await navigator.serviceWorker.register("/sw.js");
    const existing = await registration.pushManager.getSubscription();
    window._cbPushRegistration = registration;
    renderPushChip(existing ? "on" : "off");
  } catch (e) { /* fails silently -- rest of the app works fine without it */ }
}

function renderPushChip(state) {
  const host = document.getElementById("cbPushChip");
  if (!host) return;
  if (typeof Notification === "undefined") { host.innerHTML = ""; return; }

  if (Notification.permission === "denied") {
    host.innerHTML = `<span class="pill" style="opacity:.6;" title="Blocked in your browser's site settings">🔕 Blocked</span>`;
    return;
  }

  host.innerHTML = state === "on"
    ? `<button class="pill" style="border-color:var(--neon-lime); color:var(--neon-lime);" onclick="cbDisablePush()">🔔 Notifications on</button>`
    : `<button class="pill" onclick="cbEnablePush()">🔕 Enable notifications</button>`;
}

async function cbEnablePush() {
  try {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") {
      cbToast("Notification permission was not granted", "error");
      renderPushChip("off");
      return;
    }

    const registration = window._cbPushRegistration || await navigator.serviceWorker.ready;
    const keyRes = await cbApi.get("/api/push/vapid-public-key");
    const applicationServerKey = cbUrlBase64ToUint8Array(keyRes.data);

    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey,
    });

    const json = subscription.toJSON();
    await cbApi.post("/api/student/push/subscribe", {
      endpoint: json.endpoint,
      p256dh: json.keys.p256dh,
      auth: json.keys.auth,
    });

    cbToast("Notifications enabled — you'll get an alert the moment your order is ready", "success");
    renderPushChip("on");
  } catch (e) {
    cbToast("Couldn't enable notifications: " + cbErrorMessage(e), "error");
  }
}

async function cbDisablePush() {
  try {
    const registration = window._cbPushRegistration || await navigator.serviceWorker.ready;
    const subscription = await registration.pushManager.getSubscription();
    if (subscription) {
      await cbApi.post("/api/student/push/unsubscribe", { endpoint: subscription.endpoint });
      await subscription.unsubscribe();
    }
    cbToast("Notifications turned off", "success");
    renderPushChip("off");
  } catch (e) {
    cbToast(cbErrorMessage(e), "error");
  }
}