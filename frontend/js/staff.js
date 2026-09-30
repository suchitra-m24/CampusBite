const CB_NEXT_STATUS = { PLACED: "CONFIRMED", CONFIRMED: "PREPARING", PREPARING: "READY", READY: "PICKED_UP" };
const CB_ACTION_LABEL = { PLACED: "Confirm", CONFIRMED: "Start preparing", PREPARING: "Mark ready", READY: "Mark picked up" };
let cbSeenOrderIds = new Set();
let cbAudioContext = null;

/** Short synthesized ping -- no audio asset file needed. */
function cbPlayNewOrderChime() {
  try {
    if (!cbAudioContext) cbAudioContext = new (window.AudioContext || window.webkitAudioContext)();
    const ctx = cbAudioContext;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(880, ctx.currentTime);
    gain.gain.setValueAtTime(0.001, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.15, ctx.currentTime + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.4);
  } catch (e) { /* audio unsupported/blocked -- silent no-op */ }
}

async function cbInitStaffDashboard() {
  await Promise.all([cbLoadQueue(true), cbLoadCrowdMeterStaff(), cbLoadStock(), cbLoadStaffHistory()]);
  cbConnectSocket([
    { topic: "/topic/orders/staff", handler: () => cbLoadQueue() },
    { topic: "/topic/kitchen-load", handler: (payload) => { document.getElementById("crowdMeterHost").innerHTML = cbCrowdMeterHtml(payload); } },
    { topic: "/topic/menu", handler: () => cbLoadStock() },
  ]);
  setInterval(cbLoadQueue, 20000);
  document.getElementById("scanInput").addEventListener("keydown", (e) => { if (e.key === "Enter") cbScanToken(); });
  const historySearch = document.getElementById("staffHistorySearch");
  if (historySearch) historySearch.addEventListener("keydown", (e) => { if (e.key === "Enter") cbLoadStaffHistory(); });
}

async function cbLoadCrowdMeterStaff() {
  const res = await cbApi.get("/api/kitchen-load/current");
  document.getElementById("crowdMeterHost").innerHTML = cbCrowdMeterHtml(res.data);
}

async function cbLoadQueue(isInitialLoad) {
  const res = await cbApi.get("/api/staff/orders/queue");
  const queue = res.data; // already sorted by pickup slot server-side
  document.getElementById("queueEmpty").style.display = queue.length ? "none" : "block";

  if (!isInitialLoad) {
    const newPlaced = queue.filter(o => o.status === "PLACED" && !cbSeenOrderIds.has(o.id));
    if (newPlaced.length) cbPlayNewOrderChime();
  }
  queue.forEach(o => cbSeenOrderIds.add(o.id));

  document.getElementById("queueHost").innerHTML = queue.map(o => `
    <div class="cb-card cb-card-pad" style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; gap:16px; flex-wrap:wrap;">
      <div style="display:flex; gap:16px; align-items:center;">
        <div class="mono" style="font-size:1.2rem; font-weight:700; min-width:80px;">${cbFormatTime(o.pickupSlotStart)}</div>
        <div>
          <div style="font-weight:600;">#${o.id} · ${cbEscapeHtml(o.studentName)}</div>
          <div style="font-size:0.82rem; color:var(--text-soft);">${o.items.map(i => `${i.quantity}× ${cbEscapeHtml(i.menuItemName)}`).join(", ")}</div>
          ${o.specialInstructions ? `<div style="font-size:0.78rem; color:var(--neon-amber); margin-top:2px;">Note: ${cbEscapeHtml(o.specialInstructions)}</div>` : ""}
          <div class="mono" style="font-size:0.78rem; color:var(--text-soft); margin-top:2px;">${cbEscapeHtml(o.pickupToken)}</div>
        </div>
      </div>
      <div style="display:flex; align-items:center; gap:10px;">
        <span class="badge-status status-${o.status}">${o.overdue ? "OVERDUE · " : ""}${o.status.replace("_", " ")}</span>
        ${CB_NEXT_STATUS[o.status] ? `<button class="btn-cb btn-magenta btn-sm" onclick="cbAdvance(${o.id}, '${CB_NEXT_STATUS[o.status]}')">${CB_ACTION_LABEL[o.status]}</button>` : ""}
      </div>
    </div>`).join("");
}

async function cbAdvance(orderId, status) {
  try {
    await cbApi.patch(`/api/staff/orders/${orderId}/status`, { status });
    cbToast(`Order #${orderId} → ${status.replace("_", " ")}`, "success");
    cbLoadQueue();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbScanToken() {
  const input = document.getElementById("scanInput");
  const token = input.value.trim();
  if (!token) return;
  try {
    const res = await cbApi.post("/api/staff/orders/scan", { pickupToken: token });
    cbToast(`Order #${res.data.id} picked up. Enjoy!`, "success");
    input.value = "";
    cbLoadQueue();
  } catch (e) {
    cbToast(cbErrorMessage(e), "error");
  }
}

async function cbLoadStock() {
  const res = await cbApi.get("/api/staff/menu/items");
  document.getElementById("stockHost").innerHTML = res.data.map(item => `
    <div class="col-12 col-sm-6 col-lg-4">
      <div class="cb-card cb-card-pad" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <div style="font-weight:600;">${cbEscapeHtml(item.name)}</div>
          <div style="font-size:0.8rem; color:var(--text-soft);">${cbFormatMoney(item.price)}</div>
        </div>
        <button class="btn-cb btn-sm ${item.available ? "btn-danger" : "btn-cyan"}" onclick="cbToggleStock(${item.id}, ${!item.available})">
          ${item.available ? "Mark sold out" : "Mark in stock"}
        </button>
      </div>
    </div>`).join("");
}

async function cbToggleStock(itemId, available) {
  try {
    await cbApi.patch(`/api/staff/menu/items/${itemId}/availability`, { available });
    cbLoadStock();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbLoadStaffHistory() {
  const query = document.getElementById("staffHistorySearch")?.value.trim() || "";
  try {
    const res = await cbApi.get(`/api/staff/orders/history${query ? "?query=" + encodeURIComponent(query) : ""}`);
    const host = document.getElementById("staffHistoryHost");
    const page = res.data || {};
    const orders = Array.isArray(page) ? page : (page.content || []);
    if (!orders.length) {
      host.innerHTML = `<div class="empty-state" style="padding:16px 0;">No orders found.</div>`;
      return;
    }
    host.innerHTML = orders.map(o => `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border); flex-wrap:wrap; gap:8px;">
        <div>
          <div style="font-weight:600;">#${o.id} · ${cbEscapeHtml(o.studentName)}</div>
          <div style="font-size:0.8rem; color:var(--text-soft);">${o.items.map(i => `${i.quantity}× ${cbEscapeHtml(i.menuItemName)}`).join(", ")}</div>
          <div class="mono" style="font-size:0.78rem; color:var(--text-faint);">${cbEscapeHtml(o.pickupToken)} · Placed ${cbFormatTime(o.placedAt)}</div>
        </div>
        <span class="badge-status status-${o.status}">${o.status.replace("_", " ")}</span>
      </div>`).join("");
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}
