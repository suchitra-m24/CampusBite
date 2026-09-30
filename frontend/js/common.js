/* Shared top-bar rendering + a tiny STOMP-over-SockJS wrapper used by all
   three dashboards to receive live menu / kitchen-load / order updates. */

function cbEscapeHtml(value) {
  return String(value ?? "").replace(/[&<>\"\']/g, ch => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;",
    "\"": "&quot;", "\'": "&#39;"
  }[ch]));
}

function cbSafeImageUrl(value) {
  const raw = String(value ?? "").trim();
  if ((/^https?:\/\//i.test(raw) || /^data:image\/(png|jpeg|jpg|webp);base64,/i.test(raw)) && !/[\"\'()\\\r\n]/.test(raw)) return raw;
  return "";
}

function cbRenderTopbar(activePath) {
  const user = CBAuth.getUser();
  if (!user) return;
  const navsByRole = {
    STUDENT: [
      { href: "/student/dashboard.html", label: "Menu" },
      { href: "/student/orders.html", label: "My Orders" },
      { href: "/account.html", label: "Account" },
    ],
    TEACHER: [
      { href: "/student/dashboard.html", label: "Menu" },
      { href: "/student/orders.html", label: "My Orders" },
      { href: "/account.html", label: "Account" },
    ],
    STAFF: [
      { href: "/staff/dashboard.html", label: "Kitchen Queue" },
      { href: "/account.html", label: "Account" },
    ],
    ADMIN: [
      { href: "/admin/dashboard.html", label: "Menu" },
      { href: "/admin/analytics.html", label: "Analytics" },
      { href: "/admin/settings.html", label: "Slots & Users" },
      { href: "/account.html", label: "Account" },
    ],
  };

  const links = (navsByRole[user.role] || []).map(n =>
    `<a href="${n.href}" class="${activePath === n.href ? "active" : ""}">${n.label}</a>`
  ).join("");

  const el = document.getElementById("cb-topbar");
  if (!el) return;
  el.innerHTML = `
    <div class="cb-topbar-inner">
      <div style="display:flex; align-items:center; gap:28px;">
        <div class="cb-brand"><span class="cb-brand-mark">CB</span> CampusBite</div>
        <nav class="cb-nav">${links}</nav>
      </div>
          <div style="display:flex; align-items:center; gap:14px;">
           ${user.role === "STUDENT" && user.paymentMode === "WALLET" ? `<span id="cbWalletChip"></span>` : ""}
           ${(user.role === "STUDENT" || user.role === "TEACHER") ? `<span id="cbPushChip"></span>` : ""}
            <span class="cb-user-chip">${cbEscapeHtml(user.fullName)} · ${user.role.charAt(0) + user.role.slice(1).toLowerCase()}</span>
            <button class="cb-logout-btn" onclick="CBAuth.logoutRedirect()">Log out</button>
          </div>
        </div>`;

     if (user.role === "STUDENT" && user.paymentMode === "WALLET") {
         cbInitWalletWidget();
     }
     if (user.role === "STUDENT" || user.role === "TEACHER") {
         if (typeof cbInitPushNotifications === "function") cbInitPushNotifications();
     }
  }

/* ---------------- Wallet chip + top-up modal (student pages) ----------------
   Injects a balance chip into the topbar and a lightweight modal for adding
   funds, so top-up no longer requires calling the API from devtools. */
let cbWalletBalance = 0;

async function cbInitWalletWidget() {
  cbEnsureWalletModal();
  await cbRefreshWalletChip();
}

async function cbRefreshWalletChip() {
  try {
    const res = await cbApi.get("/api/student/wallet");
    cbWalletBalance = res.data.balance;
    const chip = document.getElementById("cbWalletChip");
    if (chip) {
      chip.innerHTML = `
        <button class="pill" style="display:flex; align-items:center; gap:8px; border-color:var(--neon-lime); color:var(--neon-lime);" onclick="cbOpenWalletModal()">
          💰 ${cbFormatMoney(cbWalletBalance)} <span style="font-weight:400; opacity:.8;">· Top up</span>
        </button>`;
    }
  } catch (e) { /* wallet loads lazily elsewhere too; non-fatal here */ }
}

function cbEnsureWalletModal() {
  if (document.getElementById("cbWalletModalBackdrop")) return;
  const div = document.createElement("div");
  div.id = "cbWalletModalBackdrop";
  div.style.cssText = "display:none; position:fixed; inset:0; background:rgba(0,0,0,.55); z-index:180; align-items:center; justify-content:center;";
  div.innerHTML = `
    <div class="cb-card cb-card-pad" style="width:380px; max-width:90vw;">
      <h3 style="margin-bottom:6px;">Top up wallet</h3>
      <p style="font-size:0.85rem; color:var(--text-soft); margin-bottom:18px;">Current balance: <span id="cbWalletModalBalance" class="mono">₹0.00</span></p>
      <div style="display:flex; gap:8px; margin-bottom:14px;">
        <button class="pill" onclick="cbSetTopUpAmount(100)">+₹100</button>
        <button class="pill" onclick="cbSetTopUpAmount(200)">+₹200</button>
        <button class="pill" onclick="cbSetTopUpAmount(500)">+₹500</button>
      </div>
      <div class="form-cb" style="margin-bottom:18px;">
        <label>Amount (₹)</label>
        <input type="number" min="1" step="1" class="form-control" id="cbTopUpAmount" placeholder="e.g. 300">
      </div>
      <div style="display:flex; gap:10px;">
        <button class="btn-cb btn-cyan" style="flex:1;" id="cbTopUpConfirmBtn" onclick="cbConfirmTopUp()">Add funds</button>
        <button class="btn-cb btn-ghost" onclick="cbCloseWalletModal()">Cancel</button>
      </div>
    </div>`;
  document.body.appendChild(div);
}

function cbOpenWalletModal() {
  document.getElementById("cbWalletModalBalance").textContent = cbFormatMoney(cbWalletBalance);
  document.getElementById("cbTopUpAmount").value = "";
  document.getElementById("cbWalletModalBackdrop").style.display = "flex";
}

function cbCloseWalletModal() {
  document.getElementById("cbWalletModalBackdrop").style.display = "none";
}

function cbSetTopUpAmount(amount) {
  document.getElementById("cbTopUpAmount").value = amount;
}

async function cbConfirmTopUp() {
  const amount = parseFloat(document.getElementById("cbTopUpAmount").value);
  if (!amount || amount <= 0) {
    cbToast("Enter an amount greater than ₹0", "error");
    return;
  }
  const btn = document.getElementById("cbTopUpConfirmBtn");
  btn.disabled = true; btn.textContent = "Adding…";
  try {
    const res = await cbApi.post("/api/student/wallet/top-up", { amount });
    cbWalletBalance = res.data.balance;
    cbToast(`₹${amount.toFixed(2)} added — new balance ${cbFormatMoney(cbWalletBalance)}`, "success");
    cbCloseWalletModal();
    await cbRefreshWalletChip();
  } catch (e) {
    cbToast(cbErrorMessage(e), "error");
  } finally {
    btn.disabled = false; btn.textContent = "Add funds";
  }
}

/* Live updates via STOMP/SockJS. Uses the @stomp/stompjs v7 UMD bundle
   (global `StompJs`), not the old stompjs v2 API. Falls back silently if
   the socket can't connect -- polling fallbacks are used where that
   matters (see each dashboard's refresh calls). */
async function cbConnectSocket(subscriptions) {
  try {
    const token = await CBAuth.ensureAccessToken();
    if (!token) return null;
    const client = new StompJs.Client({
      webSocketFactory: () => new SockJS(CB_WS_BASE),
      connectHeaders: {
        Authorization: `Bearer ${CBAuth.getAccessToken()}`
      },
      beforeConnect: async () => {
        client.connectHeaders = {
          Authorization: `Bearer ${CBAuth.getAccessToken()}`
        };
      },
      reconnectDelay: 4000,
      onConnect: () => {
        subscriptions.forEach(({ topic, handler }) => {
          client.subscribe(topic, (message) => {
            try { handler(JSON.parse(message.body)); }
            catch (e) { handler(message.body); }
          });
        });
      },
      onStompError: (frame) => {
        console.warn("CampusBite WebSocket/STOMP error; polling fallback remains active.", frame?.headers?.message || frame);
      },
      onWebSocketError: (event) => {
        console.warn("CampusBite WebSocket connection failed; polling fallback remains active.", event);
      },
    });
    client.activate();
    return client;
  } catch (e) {
    return null;
  }
}

function cbCrowdMeterHtml(load) {
  if (!load) return "";
  const pct = load.loadPercentage ?? 0;
  const circumference = 2 * Math.PI * 26;
  const offset = circumference * (1 - pct / 100);
  const colorByLevel = { LOW: "var(--neon-cyan)", MODERATE: "var(--neon-amber)", BUSY: "var(--neon-amber)", VERY_BUSY: "var(--neon-magenta)" };
  const color = colorByLevel[load.level] || "var(--neon-violet)";
  return `
    <div class="crowd-meter">
      <div class="crowd-dial">
        <svg width="64" height="64" viewBox="0 0 64 64">
          <circle cx="32" cy="32" r="26" fill="none" stroke="var(--border)" stroke-width="7"/>
          <circle cx="32" cy="32" r="26" fill="none" stroke="${color}" stroke-width="7"
            stroke-dasharray="${circumference}" stroke-dashoffset="${offset}" stroke-linecap="round"/>
        </svg>
        <div class="crowd-dial-value">${pct}%</div>
      </div>
      <div>
        <div class="crowd-label">${load.level.replace("_", " ")} right now</div>
        <div class="crowd-sub">${cbEscapeHtml(load.message)}</div>
      </div>
    </div>`;
}

/* ---------------- Broadcast banner ----------------
   Any page that calls cbInitBroadcastBanner() gets a dismissible-for-session
   banner injected right below the topbar if admin has an active message. */
async function cbInitBroadcastBanner() {
  try {
    const res = await cbApi.get("/api/broadcast/active");
    if (!res.data || !res.data.active || !res.data.message) return;

    const dismissedId = sessionStorage.getItem("cb_dismissed_broadcast_id");
    if (dismissedId && Number(dismissedId) === res.data.id) return;

    let host = document.getElementById("cb-broadcast-banner");
    if (!host) {
      host = document.createElement("div");
      host.id = "cb-broadcast-banner";
      const topbar = document.getElementById("cb-topbar");
      if (topbar && topbar.parentNode) {
        topbar.parentNode.insertBefore(host, topbar.nextSibling);
      } else {
        document.body.prepend(host);
      }
    }

    host.innerHTML = `
      <div style="background:linear-gradient(90deg, var(--neon-amber), var(--neon-magenta)); color:#1a0f00; padding:10px 20px; display:flex; align-items:center; justify-content:center; gap:16px; font-weight:700; font-size:0.88rem;">
        <span>📢 ${cbEscapeHtml(res.data.message)}</span>
        <button onclick="cbDismissBroadcast(${res.data.id})" style="background:none; border:none; font-weight:700; cursor:pointer; color:#1a0f00; text-decoration:underline;">Dismiss</button>
      </div>`;
  } catch (e) { /* non-critical */ }
}

function cbDismissBroadcast(id) {
  sessionStorage.setItem("cb_dismissed_broadcast_id", String(id));
  const host = document.getElementById("cb-broadcast-banner");
  if (host) host.innerHTML = "";
}

/* ---------------- Single campus canteen status ---------------- */
async function cbInitCanteenStatus(hostId = "canteenStatusHost") {
  const host = document.getElementById(hostId);
  if (!host) return;
  try {
    const res = await cbApi.get("/api/canteen/status");
    const c = res.data;
    host.innerHTML = `
      <div class="cb-card cb-card-pad" style="margin-bottom:18px;">
        <div style="display:flex;justify-content:space-between;gap:12px;align-items:center;flex-wrap:wrap;">
          <div><div class="eyebrow">One campus canteen</div><strong>${cbEscapeHtml(c.canteenName)}</strong></div>
          <span class="badge-status ${c.orderingEnabled ? 'status-PICKED_UP' : 'status-CANCELLED'}">${c.orderingEnabled ? 'Online ordering open' : 'Ordering paused'}</span>
        </div>
        <div style="font-size:.82rem;color:var(--text-soft);margin-top:8px;">Daily operating hours: ${c.openingTime.slice(0,5)} – ${c.closingTime.slice(0,5)} · Pickup slots are restricted to these hours.</div>
      </div>`;
  } catch (e) {}
}
