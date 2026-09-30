/* ===================== Admin: menu management (dashboard.html) ===================== */

let cbAdminCategories = [];
let cbAdminItems = [];

async function cbInitAdminMenu() {
  await cbLoadAdminCategories();
  await cbLoadAdminItems();
  document.getElementById("itemForm").addEventListener("submit", cbSubmitItem);
}

async function cbLoadAdminCategories() {
  const res = await cbApi.get("/api/menu/categories");
  cbAdminCategories = res.data.sort((a, b) => a.displayOrder - b.displayOrder);
  document.getElementById("itemCategory").innerHTML = cbAdminCategories.map(c => `<option value="${c.id}">${cbEscapeHtml(c.name)}</option>`).join("");
  renderCategoryList();
}

function renderCategoryList() {
  const host = document.getElementById("categoryListHost");
  if (!host) return;
  host.innerHTML = cbAdminCategories.map(c => {
    const windowLabel = (c.availableFromMinuteOfDay != null && c.availableToMinuteOfDay != null)
      ? `${cbMinuteOfDayToLabel(c.availableFromMinuteOfDay)}–${cbMinuteOfDayToLabel(c.availableToMinuteOfDay)}`
      : "All day";
    return `
      <div class="cb-card cb-card-pad" style="display:flex; align-items:center; gap:10px; padding:10px 14px;">
        <div>
          <div style="font-weight:600; font-size:0.9rem;">${cbEscapeHtml(c.name)}</div>
          <div style="font-size:0.75rem; color:var(--text-soft);">${windowLabel}</div>
        </div>
        <button class="btn-cb btn-ghost btn-sm" onclick="cbEditCategory(${c.id})">Edit</button>
        <button class="btn-cb btn-danger btn-sm" onclick="cbDeleteCategory(${c.id})">Delete</button>
      </div>`;
  }).join("");
}

function cbMinuteOfDayToLabel(minutes) {
  const h = Math.floor(minutes / 60), m = minutes % 60;
  const period = h >= 12 ? "PM" : "AM";
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, "0")} ${period}`;
}

function cbTimeToMinuteOfDay(timeStr) {
  if (!timeStr) return null;
  const [h, m] = timeStr.split(":").map(Number);
  return h * 60 + m;
}

function cbMinuteOfDayToTimeInput(minutes) {
  if (minutes == null) return "";
  const h = Math.floor(minutes / 60), m = minutes % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}

function cbEditCategory(categoryId) {
  const cat = cbAdminCategories.find(c => c.id === categoryId);
  if (!cat) return;
  document.getElementById("editingCategoryId").value = categoryId;
  document.getElementById("newCategoryName").value = cat.name;
  document.getElementById("newCategoryFrom").value = cbMinuteOfDayToTimeInput(cat.availableFromMinuteOfDay);
  document.getElementById("newCategoryTo").value = cbMinuteOfDayToTimeInput(cat.availableToMinuteOfDay);
  document.getElementById("categorySaveBtn").textContent = "Save changes";
  document.getElementById("categoryCancelEditBtn").style.display = "inline-flex";
}

function cbCancelCategoryEdit() {
  document.getElementById("editingCategoryId").value = "";
  document.getElementById("newCategoryName").value = "";
  document.getElementById("newCategoryFrom").value = "";
  document.getElementById("newCategoryTo").value = "";
  document.getElementById("categorySaveBtn").textContent = "Add category";
  document.getElementById("categoryCancelEditBtn").style.display = "none";
}

async function cbCreateCategory() {
  const name = document.getElementById("newCategoryName").value.trim();
  if (!name) return;
  const editingId = document.getElementById("editingCategoryId").value;
  const availableFromMinuteOfDay = cbTimeToMinuteOfDay(document.getElementById("newCategoryFrom").value);
  const availableToMinuteOfDay = cbTimeToMinuteOfDay(document.getElementById("newCategoryTo").value);
  const payload = { name, displayOrder: editingId ? cbAdminCategories.find(c => c.id === Number(editingId))?.displayOrder || 1 : cbAdminCategories.length + 1, availableFromMinuteOfDay, availableToMinuteOfDay };
  try {
    if (editingId) {
      await cbApi.put(`/api/admin/menu/categories/${editingId}`, payload);
      cbToast("Category updated", "success");
    } else {
      await cbApi.post("/api/admin/menu/categories", payload);
      cbToast("Category added", "success");
    }
    cbCancelCategoryEdit();
    await cbLoadAdminCategories();
    await cbLoadAdminItems();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbDeleteCategory(categoryId) {
  if (!confirm("Delete this category? Items in it will need to be reassigned.")) return;
  try {
    await cbApi.delete(`/api/admin/menu/categories/${categoryId}`);
    cbToast("Category deleted", "success");
    await cbLoadAdminCategories();
    await cbLoadAdminItems();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbLoadAdminItems() {
  const res = await cbApi.get("/api/admin/menu/items");
  cbAdminItems = res.data;
  const host = document.getElementById("adminMenuHost");
  host.innerHTML = cbAdminCategories.map(cat => {
    const items = cbAdminItems.filter(i => i.categoryId === cat.id);
    if (!items.length) return "";
    return `
      <div class="col-12">
        <h3 style="font-size:1rem; margin:18px 0 12px;">${cbEscapeHtml(cat.name)}</h3>
        <div class="row g-3">
          ${items.map(item => `
            <div class="col-12 col-sm-6 col-lg-4">
              <div class="cb-card cb-card-pad">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                  <div>
                    <div style="font-weight:600;">${cbEscapeHtml(item.name)}</div>
                    <div class="mono" style="font-size:0.85rem; color:var(--text-soft);">${cbFormatMoney(item.price)} · ${item.baselinePrepTimeMinutes} min</div>
                  </div>
                  <span class="badge-status ${item.available ? 'status-READY' : 'status-CANCELLED'}">${item.available ? "In stock" : "Sold out"}</span>
                </div>
                <div style="display:flex; gap:8px; margin-top:14px;">
                  <button class="btn-cb btn-outline btn-sm" onclick="cbOpenItemModal(${item.id})">Edit</button>
                  <button class="btn-cb btn-danger btn-sm" onclick="cbDeleteItem(${item.id})">Delete</button>
                </div>
              </div>
            </div>`).join("")}
        </div>
      </div>`;
  }).join("");
}

function cbOpenItemModal(itemId) {
  const item = cbAdminItems.find(i => i.id === itemId);
  document.getElementById("itemModalTitle").textContent = item ? "Edit menu item" : "New menu item";
  document.getElementById("itemId").value = item ? item.id : "";
  document.getElementById("itemName").value = item ? item.name : "";
  document.getElementById("itemDescription").value = item ? item.description || "" : "";
  document.getElementById("itemImageUrl").value = item ? item.imageUrl || "" : "";
  document.getElementById("itemPrice").value = item ? item.price : "";
  document.getElementById("itemPrepTime").value = item ? item.baselinePrepTimeMinutes : 8;
  document.getElementById("itemCategory").value = item ? item.categoryId : (cbAdminCategories[0]?.id || "");
  document.getElementById("itemDietType").value = item ? (item.dietType || "VEG") : "VEG";
  document.getElementById("itemAllergens").value = item ? (item.allergens || []).join(", ") : "";
  document.getElementById("itemCustomizations").value = item ? (item.customizationOptions || []).join(", ") : "";
  document.getElementById("itemAvailable").checked = item ? item.available : true;
  document.getElementById("itemSpecial").checked = item ? item.dailySpecial : false;
  document.getElementById("itemModalBackdrop").style.display = "flex";
}

function cbCloseItemModal() {
  document.getElementById("itemModalBackdrop").style.display = "none";
}

async function cbSubmitItem(e) {
  e.preventDefault();
  const id = document.getElementById("itemId").value;
  const payload = {
    name: document.getElementById("itemName").value,
    description: document.getElementById("itemDescription").value,
    price: parseFloat(document.getElementById("itemPrice").value),
    categoryId: parseInt(document.getElementById("itemCategory").value, 10),
    imageUrl: document.getElementById("itemImageUrl").value.trim() || null,
    available: document.getElementById("itemAvailable").checked,
    dailySpecial: document.getElementById("itemSpecial").checked,
    baselinePrepTimeMinutes: parseInt(document.getElementById("itemPrepTime").value, 10),
    customizationOptions: document.getElementById("itemCustomizations").value.split(",").map(s => s.trim()).filter(Boolean),
    dietType: document.getElementById("itemDietType").value,
    allergens: document.getElementById("itemAllergens").value.split(",").map(s => s.trim()).filter(Boolean),
  };
  try {
    if (id) await cbApi.put(`/api/admin/menu/items/${id}`, payload);
    else await cbApi.post("/api/admin/menu/items", payload);
    cbToast("Menu item saved", "success");
    cbCloseItemModal();
    await cbLoadAdminItems();
  } catch (e2) { cbToast(cbErrorMessage(e2), "error"); }
}

async function cbDeleteItem(itemId) {
  if (!confirm("Delete this menu item?")) return;
  try {
    await cbApi.delete(`/api/admin/menu/items/${itemId}`);
    cbToast("Deleted", "success");
    await cbLoadAdminItems();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

/* ===================== Admin: analytics (analytics.html) ===================== */

async function cbInitAnalytics() {
  // Chart.js defaults to dark text/gridlines -- switch to light-on-dark to match the theme.
  Chart.defaults.color = "#92a0b5";
  Chart.defaults.borderColor = "#232c3d";
  Chart.defaults.font.family = "-apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";

  const [summary, bestSellers, peakHours, slotUtil] = await Promise.all([
    cbApi.get("/api/admin/analytics/summary"),
    cbApi.get("/api/admin/analytics/best-sellers?limit=8"),
    cbApi.get("/api/admin/analytics/peak-hours"),
    cbApi.get("/api/admin/analytics/slot-utilization"),
  ]);

  const s = summary.data;
  document.getElementById("summaryCards").innerHTML = [
    ["Orders today", s.totalOrdersToday],
    ["Revenue today", cbFormatMoney(s.revenueToday)],
    ["Prep-time accuracy", `${s.averagePrepAccuracy.toFixed(0)}%`],
    ["No-shows today", s.noShowsToday],
  ].map(([label, value]) => `
    <div class="col-6 col-lg-3">
      <div class="cb-card cb-card-pad">
        <div style="font-size:0.78rem; color:var(--text-soft); text-transform:uppercase; letter-spacing:0.03em;">${label}</div>
        <div style="font-family:var(--font-mono); font-size:1.6rem; font-weight:700; margin-top:6px;">${value}</div>
      </div>
    </div>`).join("");

  new Chart(document.getElementById("peakHoursChart"), {
    type: "bar",
    data: {
      labels: peakHours.data.map(p => `${p.hourOfDay}:00`),
      datasets: [{ label: "Avg load %", data: peakHours.data.map(p => p.averageLoadPercentage.toFixed(1)), backgroundColor: "#ffb020", borderRadius: 4 }]
    },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, max: 100 } } }
  });

  new Chart(document.getElementById("bestSellersChart"), {
    type: "doughnut",
    data: {
      labels: bestSellers.data.map(b => b.name),
      datasets: [{ data: bestSellers.data.map(b => b.totalQuantity), backgroundColor: ["#2fe6c9","#9d6bff","#ff3d81","#ffb020","#1c9c88","#6d49b8","#b12a5c","#b87c15"], borderColor: "#131924", borderWidth: 2 }]
    },
    options: { plugins: { legend: { position: "right", labels: { boxWidth: 12, font: { size: 11 } } } } }
  });

  new Chart(document.getElementById("slotUtilChart"), {
    type: "line",
    data: {
      labels: slotUtil.data.map(s2 => s2.slotLabel),
      datasets: [{ label: "Orders booked", data: slotUtil.data.map(s2 => s2.ordersBooked), borderColor: "#2fe6c9", backgroundColor: "rgba(47,230,201,.14)", pointBackgroundColor: "#2fe6c9", fill: true, tension: 0.3 }]
    },
    options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
  });
}

/* ===================== Admin: settings (settings.html) ===================== */

async function cbInitSettings() {
  const now = new Date();
  document.getElementById("billingYear").value = now.getFullYear();
  document.getElementById("billingMonth").value = now.getMonth() + 1;
  await Promise.all([cbLoadSlotCapacities(), cbLoadUsers(), cbLoadCurrentBroadcast(), cbLoadUnsettledStatements()]);
}

async function cbLoadCurrentBroadcast() {
  try {
    const res = await cbApi.get("/api/broadcast/active");
    const host = document.getElementById("currentBroadcastHost");
    host.innerHTML = res.data && res.data.active
      ? `<div class="cb-card cb-card-pad" style="border-color:var(--neon-amber);"><strong style="color:var(--neon-amber);">Currently live:</strong> ${cbEscapeHtml(res.data.message)}</div>`
      : `<div style="font-size:0.82rem; color:var(--text-faint);">No banner currently active.</div>`;
  } catch (e) { /* non-critical */ }
}

async function cbSetBroadcast() {
  const message = document.getElementById("broadcastMessage").value.trim();
  if (!message) { cbToast("Enter a message first", "error"); return; }
  try {
    await cbApi.post("/api/admin/broadcast", { message });
    document.getElementById("broadcastMessage").value = "";
    cbToast("Broadcast published", "success");
    cbLoadCurrentBroadcast();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbClearBroadcast() {
  try {
    await cbApi.post("/api/admin/broadcast/clear");
    cbToast("Broadcast cleared", "success");
    cbLoadCurrentBroadcast();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbLoadSlotCapacities() {
  const res = await cbApi.get("/api/admin/slots");
  document.getElementById("slotCapacityHost").innerHTML = res.data
    .sort((a, b) => a.windowStartMinuteOfDay - b.windowStartMinuteOfDay)
    .map(c => `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:9px 0; border-bottom:1px solid var(--border);">
        <span class="mono">${c.label}</span>
        <span style="display:flex; align-items:center; gap:10px;">
          <span class="mono">max ${c.maxOrders}</span>
          <button class="btn-cb btn-danger btn-sm" onclick="cbDeleteSlotCapacity(${c.id})">Remove</button>
        </span>
      </div>`).join("") || `<div class="empty-state" style="padding:20px 0;">No custom windows set — using the default capacity for every slot.</div>`;
}

async function cbSaveSlotCapacity() {
  const time = document.getElementById("slotTime").value;
  const capacity = parseInt(document.getElementById("slotCapacity").value, 10);
  if (!time) { cbToast("Pick a window start time", "error"); return; }
  const [h, m] = time.split(":").map(Number);
  try {
    await cbApi.post("/api/admin/slots", { windowStartMinuteOfDay: h * 60 + m, maxOrders: capacity });
    cbToast("Slot capacity saved", "success");
    cbLoadSlotCapacities();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbDeleteSlotCapacity(id) {
  try {
    await cbApi.delete(`/api/admin/slots/${id}`);
    cbLoadSlotCapacities();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbLoadUsers() {
  const res = await cbApi.get("/api/admin/users");
  document.getElementById("usersHost").innerHTML = res.data.map(u => `
    <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border);">
      <div>
        <div style="font-weight:600;">${cbEscapeHtml(u.fullName)}</div>
        <div style="font-size:0.8rem; color:var(--text-soft);">${cbEscapeHtml(u.loginId)} · ${cbEscapeHtml(u.email)} · ${u.role}</div>
      </div>
      <button class="btn-cb btn-outline btn-sm" onclick="cbToggleUser(${u.id}, '${u.active === false ? "activate" : "deactivate"}')">
        ${u.active === false ? "Activate" : "Deactivate"}
      </button>
    </div>`).join("");
}

async function cbToggleUser(userId, action) {
  try {
    await cbApi.patch(`/api/admin/users/${userId}/${action}`);
    cbToast(`User ${action}d`, "success");
    cbLoadUsers();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

/* ===================== Admin: order search (dashboard.html) ===================== */

async function cbSearchOrders() {
  const query = document.getElementById("orderSearchInput").value.trim();
  const host = document.getElementById("orderSearchResults");
  if (!query) { host.innerHTML = ""; return; }
  try {
    const res = await cbApi.get(`/api/admin/orders/search?query=${encodeURIComponent(query)}`);
    if (!res.data.length) {
      host.innerHTML = `<div class="empty-state" style="padding:16px 0;">No matching orders.</div>`;
      return;
    }
    host.innerHTML = res.data.map(o => `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border); flex-wrap:wrap; gap:8px;">
        <div>
          <div style="font-weight:600;">#${o.id} · ${cbEscapeHtml(o.studentName)} · ${cbFormatMoney(o.totalAmount)}</div>
          <div style="font-size:0.8rem; color:var(--text-soft);">${o.items.map(i => `${i.quantity}× ${cbEscapeHtml(i.menuItemName)}`).join(", ")}</div>
          <div class="mono" style="font-size:0.78rem; color:var(--text-faint);">Slot ${cbFormatSlot(o.pickupSlotStart, o.pickupSlotEnd)}</div>
        </div>
        <span class="badge-status status-${o.status}">${o.status.replace("_", " ")}</span>
      </div>`).join("");
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbGenerateStatements() {
  const year = parseInt(document.getElementById("billingYear").value, 10);
  const month = parseInt(document.getElementById("billingMonth").value, 10);
  if (!year || month < 1 || month > 12) { cbToast("Enter a valid year and month", "error"); return; }
  try {
    const res = await cbApi.post("/api/admin/billing/generate", { year, month });
    cbToast(`${res.data.length} statement(s) generated`, "success");
    cbLoadUnsettledStatements();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbLoadUnsettledStatements() {
  try {
    const res = await cbApi.get("/api/admin/billing/statements");
    const host = document.getElementById("unsettledStatementsHost");
    if (!host) return;
    host.innerHTML = res.data.length ? res.data.map(s => `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px solid var(--border);">
        <div style="font-size:0.85rem;">${cbEscapeHtml(s.userName)} (${cbEscapeHtml(s.loginId)}) · ${s.month}/${s.year} · ${s.orderCount} orders</div>
        <div style="display:flex; align-items:center; gap:10px;">
          <span class="mono">${cbFormatMoney(s.totalAmount)}</span>
          <button class="btn-cb btn-outline btn-sm" onclick="cbSettleStatement(${s.id})">Mark settled</button>
        </div>
      </div>`).join("") : `<div class="empty-state" style="padding:16px 0;">No unsettled statements.</div>`;
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbSettleStatement(id) {
  try {
    await cbApi.post(`/api/admin/billing/statements/${id}/settle`);
    cbToast("Marked settled", "success");
    cbLoadUnsettledStatements();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbCreatePrivilegedAccount() {
  const payload = {
    fullName: document.getElementById("newAccountName").value.trim(),
    email: document.getElementById("newAccountEmail").value.trim(),
    loginId: document.getElementById("newAccountLoginId").value.trim(),
    password: document.getElementById("newAccountPassword").value,
    role: document.getElementById("newAccountRole").value,
  };
  if (!payload.fullName || !payload.email || !payload.loginId || !payload.password) {
    cbToast("Fill in every field", "error");
    return;
  }
  try {
    await cbApi.post("/api/auth/admin/create-account", payload);
    cbToast(`${payload.role === "ADMIN" ? "Admin" : payload.role === "TEACHER" ? "Teacher" : "Staff"} account created`, "success");
    ["newAccountName","newAccountEmail","newAccountLoginId","newAccountPassword"].forEach(id => document.getElementById(id).value = "");
    cbLoadUsers();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}
