/* ===================== Student: menu + cart (dashboard.html) ===================== */

let cbMenuItems = [];
let cbCategories = [];
let cbActiveCategory = "all";
let cbActiveDiet = "all";
let cbCart = { items: [], total: 0 };
let cbPendingCustomizeItem = null;

async function cbInitDashboard() {
  renderMenuSkeleton();
  await Promise.all([cbLoadMenu(), cbLoadCategories(), cbLoadCart(), cbLoadCrowdMeter(), cbLoadSpecials()]);
  cbConnectSocket([
    { topic: "/topic/menu", handler: () => cbLoadMenu() },
    { topic: "/topic/kitchen-load", handler: (payload) => renderCrowdMeter(payload) },
  ]);
  setInterval(cbLoadCrowdMeter, 60000); // fallback poll in case the socket drops
}

async function cbLoadSpecials() {
  try {
    const res = await cbApi.get("/api/menu/items/specials");
    const host = document.getElementById("specialsHost");
    if (!res.data.length) { host.innerHTML = ""; return; }
    host.innerHTML = `
      <div class="cb-card cb-card-pad" style="background:linear-gradient(120deg, rgba(157,107,255,.16), rgba(47,230,201,.10)); border-color: var(--border-strong); margin-bottom:6px;">
        <div class="eyebrow" style="color:var(--neon-amber);">Today's specials</div>
        <div style="display:flex; gap:10px; margin-top:8px; flex-wrap:wrap;">
          ${res.data.map(i => `<span class="pill" style="background:rgba(9,12,17,.4); color:var(--text); border-color:var(--border-strong);">${cbEscapeHtml(i.name)} · ${cbFormatMoney(i.price)}</span>`).join("")}
        </div>
      </div>`;
  } catch (e) { /* non-critical */ }
}

async function cbLoadCrowdMeter() {
  try {
    const res = await cbApi.get("/api/kitchen-load/current");
    renderCrowdMeter(res.data);
  } catch (e) { /* non-critical */ }
}

function renderCrowdMeter(load) {
  const host = document.getElementById("crowdMeterHost");
  if (host) host.innerHTML = cbCrowdMeterHtml(load);
}

async function cbLoadCategories() {
  const res = await cbApi.get("/api/menu/categories");
  cbCategories = res.data.sort((a, b) => a.displayOrder - b.displayOrder);
  const host = document.getElementById("categoryPills");
  host.innerHTML = `<button class="pill active" onclick="cbFilterCategory('all', this)">All</button>` +
    cbCategories.map(c => `<button class="pill" onclick="cbFilterCategory(${c.id}, this)">${cbEscapeHtml(c.name)}</button>`).join("");
}

function cbFilterCategory(catId, btn) {
  cbActiveCategory = catId;
  document.querySelectorAll("#categoryPills .pill").forEach(p => p.classList.remove("active"));
  btn.classList.add("active");
  renderMenuGrid();
}

function cbFilterDiet(diet, btn) {
  cbActiveDiet = diet;
  document.querySelectorAll("#dietFilterAll, #dietFilterVeg, #dietFilterNonVeg").forEach(p => p.classList.remove("active"));
  btn.classList.add("active");
  renderMenuGrid();
}
function renderMenuSkeleton() {
  const grid = document.getElementById("menuGrid");
  if (!grid) return;
  grid.innerHTML = Array.from({ length: 6 }).map(() => `
    <div class="col-12 col-sm-6 col-lg-4">
      <div class="menu-card">
        <div class="skeleton" style="height:132px;"></div>
        <div class="menu-card-body">
          <div class="skeleton" style="height:18px; width:70%; margin-bottom:8px;"></div>
          <div class="skeleton" style="height:14px; width:90%; margin-bottom:6px;"></div>
          <div class="skeleton" style="height:14px; width:50%;"></div>
        </div>
      </div>
    </div>`).join("");
}

async function cbLoadMenu() {
  try {
    const res = await cbApi.get("/api/menu/items");
    cbMenuItems = res.data;
    renderMenuGrid();
  } catch (e) {
    document.getElementById("menuGrid").innerHTML = "";
    document.getElementById("menuEmpty").style.display = "block";
    document.getElementById("menuEmpty").innerHTML = `
      <h3>Couldn't load the menu</h3>
      <p>Check your connection and try again.</p>
      <button class="btn-cb btn-outline" style="margin-top:10px;" onclick="cbLoadMenu()">Retry</button>`;
  }
}

function renderMenuGrid() {
  const grid = document.getElementById("menuGrid");
  const searchEl = document.getElementById("menuSearch");
  const search = searchEl ? searchEl.value.trim().toLowerCase() : "";

  let items = cbActiveCategory === "all" ? cbMenuItems : cbMenuItems.filter(i => i.categoryId === cbActiveCategory);
  if (cbActiveDiet !== "all") items = items.filter(i => i.dietType === cbActiveDiet);
  if (search) {
    items = items.filter(i =>
      (i.name || "").toLowerCase().includes(search) || (i.description || "").toLowerCase().includes(search));
  }

  document.getElementById("menuEmpty").style.display = items.length ? "none" : "block";

  grid.innerHTML = items.map(item => `
    <div class="col-12 col-sm-6 col-lg-4">
      <div class="menu-card">
        <div class="menu-card-image" ${item.imageUrl ? `style="background-image:url('${cbSafeImageUrl(item.imageUrl)}'); background-size:cover; background-position:center;"` : ""}>
          ${item.dailySpecial ? `<span class="menu-card-badge">Special</span>` : ""}
          <span class="menu-card-avail ${item.available ? "avail-yes" : "avail-no"}">${item.available ? "In stock" : "Sold out"}</span>
          ${!item.imageUrl ? cbEscapeHtml(item.name.charAt(0)) : ""}
        </div>
        <div class="menu-card-body">
          <div style="display:flex; align-items:center; gap:6px;">
            <span title="${item.dietType === 'NON_VEG' ? 'Non-vegetarian' : item.dietType === 'EGG' ? 'Contains egg' : 'Vegetarian'}" style="display:inline-block; width:12px; height:12px; border:2px solid ${item.dietType === 'NON_VEG' ? 'var(--danger)' : item.dietType === 'EGG' ? 'var(--neon-amber)' : 'var(--success)'}; border-radius:2px; position:relative; flex-shrink:0;">
              <span style="position:absolute; inset:2px; border-radius:50%; background:${item.dietType === 'NON_VEG' ? 'var(--danger)' : item.dietType === 'EGG' ? 'var(--neon-amber)' : 'var(--success)'};"></span>
            </span>
            <div class="menu-card-title">${cbEscapeHtml(item.name)}</div>
          </div>
          <div class="menu-card-desc">${cbEscapeHtml(item.description || "")}</div>
          ${item.allergens && item.allergens.length ? `<div style="font-size:0.72rem; color:var(--text-faint);">Contains: ${cbEscapeHtml(item.allergens.join(", "))}</div>` : ""}
          <div class="menu-card-footer">
            <span class="menu-card-price">${cbFormatMoney(item.price)}</span>
            <button class="btn-cb btn-sm ${item.available ? "btn-magenta" : "btn-ghost"}"
              ${item.available ? "" : "disabled"} onclick="cbStartAddToCart(${item.id})">
              ${item.available ? "Add" : "Sold out"}
            </button>
          </div>
        </div>
      </div>
    </div>`).join("");
}
function cbStartAddToCart(menuItemId) {
  const item = cbMenuItems.find(i => i.id === menuItemId);
  if (!item) return;

  if (!item.customizationOptions || item.customizationOptions.length === 0) {
    cbAddToCart(menuItemId, []);
    return;
  }

  cbPendingCustomizeItem = item;
  document.getElementById("customizeModalTitle").textContent = "Customize " + item.name;
  document.getElementById("customizeOptionsHost").innerHTML = item.customizationOptions.map((opt, idx) => `
    <label style="display:flex; align-items:center; gap:10px; padding:8px 0; font-size:0.92rem; cursor:pointer;">
      <input type="radio" name="cb-customize-opt" class="cb-customize-opt" value="${cbEscapeHtml(opt)}" ${idx === 0 ? "checked" : ""}> ${cbEscapeHtml(opt)}
    </label>`).join("");
  document.getElementById("customizeModalBackdrop").style.display = "flex";
}

function cbCloseCustomizeModal() {
  document.getElementById("customizeModalBackdrop").style.display = "none";
  cbPendingCustomizeItem = null;
}

function cbConfirmCustomizedAdd() {
  if (!cbPendingCustomizeItem) return;
  const selected = document.querySelector(".cb-customize-opt:checked");
  cbAddToCart(cbPendingCustomizeItem.id, selected ? [selected.value] : []);
  cbCloseCustomizeModal();
}

async function cbAddToCart(menuItemId, selectedCustomizations) {
  try {
    const res = await cbApi.post("/api/student/cart/items", { menuItemId, quantity: 1, selectedCustomizations: selectedCustomizations || [] });
    cbCart = res.data;
    renderCartUI();
    cbToast("Added to cart", "success");
  } catch (e) {
    cbToast(cbErrorMessage(e), "error");
  }
}

async function cbLoadCart() {
  const res = await cbApi.get("/api/student/cart");
  cbCart = res.data;
  renderCartUI();
}

async function cbUpdateCartQty(cartItemId, qty) {
  try {
    const res = await cbApi.patch(`/api/student/cart/items/${cartItemId}`, { quantity: qty });
    cbCart = res.data;
    renderCartUI();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

function renderCartUI() {
  const count = cbCart.items.reduce((sum, i) => sum + i.quantity, 0);
  document.getElementById("cartFab").style.display = count > 0 ? "flex" : "none";
  document.getElementById("cartCount").textContent = count;
  document.getElementById("cartTotal").textContent = cbFormatMoney(cbCart.total);

  const host = document.getElementById("cartItemsHost");
  if (!host) return;

  if (!cbCart.items.length) {
    host.innerHTML = `<div class="empty-state">Your cart is empty.</div>`;
  } else {
    host.innerHTML = cbCart.items.map(i => `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:12px 0; border-bottom:1px solid var(--border);">
        <div>
          <div style="font-weight:600;">${cbEscapeHtml(i.menuItemName)}</div>
          <div style="font-size:0.82rem; color:var(--text-soft);">${cbFormatMoney(i.unitPrice)} each</div>
          ${!i.available ? `<div style="font-size:0.78rem; color:var(--neon-pink); margin-top:2px;">No longer available</div>` : ""}
        </div>
        <div style="display:flex; align-items:center; gap:8px;">
          <button class="btn-cb btn-ghost btn-sm" onclick="cbUpdateCartQty(${i.id}, ${i.quantity - 1})">−</button>
          <span class="mono">${i.quantity}</span>
          <button class="btn-cb btn-ghost btn-sm" onclick="cbUpdateCartQty(${i.id}, ${i.quantity + 1})">+</button>
        </div>
      </div>`).join("");
  }
  const totalEl = document.getElementById("cartDrawerTotal");
  if (totalEl) totalEl.textContent = cbFormatMoney(cbCart.total);
}

function cbOpenCart() {
  document.getElementById("cartDrawer").classList.add("open");
  document.getElementById("cartBackdrop").classList.add("open");
}
function cbCloseCart() {
  document.getElementById("cartDrawer").classList.remove("open");
  document.getElementById("cartBackdrop").classList.remove("open");
}

async function cbPlaceOrder() {
  const btn = document.getElementById("placeOrderBtn");
  btn.disabled = true; btn.textContent = "Placing order…";
  try {
    const specialInstructions = document.getElementById("specialInstructions").value;
    const storageKey = "cb_pending_order_idempotency";
    let pending = null;
    try { pending = JSON.parse(sessionStorage.getItem(storageKey) || "null"); } catch (_) {}

    // Reuse the same key if the browser lost the previous response. This makes a
    // second click/retry replay the original order instead of creating a duplicate.
    if (!pending || pending.notes !== specialInstructions) {
      pending = { key: crypto.randomUUID(), notes: specialInstructions };
      sessionStorage.setItem(storageKey, JSON.stringify(pending));
    }

    const res = await cbApi.post("/api/student/orders", { specialInstructions }, {
      headers: { "Idempotency-Key": pending.key }
    });
    sessionStorage.removeItem(storageKey);
    cbToast(`Order placed! Slot ${cbFormatSlot(res.data.pickupSlotStart, res.data.pickupSlotEnd)}`, "success");
    window.location.href = "/student/orders.html";
  } catch (e) {
    cbToast(cbErrorMessage(e), "error");
    btn.disabled = false; btn.textContent = "Place order — auto-assign my slot";
  }
}

/* ===================== Student: orders + live ticket (orders.html) ===================== */

const CB_STAGES = ["PLACED", "CONFIRMED", "PREPARING", "READY", "PICKED_UP"];
const CB_STAGE_LABELS = { PLACED: "Placed", CONFIRMED: "Confirmed", PREPARING: "Preparing", READY: "Ready", PICKED_UP: "Picked up" };

let cbOrders = [];

async function cbInitOrdersPage() {
  await Promise.all([cbLoadOrders(), cbLoadTrustScore()]);
  const user = CBAuth.getUser();
  cbConnectSocket([
    { topic: `/topic/orders/user/${user.id}`, handler: () => cbLoadOrders() },
  ]);
  setInterval(cbLoadOrders, 30000);
  setInterval(cbTickCountdowns, 1000);
}

function cbTickCountdowns() {
  document.querySelectorAll("[data-countdown-target]").forEach(el => {
    const target = new Date(el.getAttribute("data-countdown-target")).getTime();
    const now = Date.now();
    const diffMin = Math.round((target - now) / 60000);
    el.textContent = diffMin > 0 ? `~${diffMin} min until ready` : "Ready any moment now";
  });
}

async function cbLoadTrustScore() {
  try {
    const res = await cbApi.get("/api/student/trust-score");
    const host = document.getElementById("trustScoreHost");
    const color = res.data.restricted ? "var(--neon-pink)" : "var(--neon-cyan)";
    host.innerHTML = `
      <div class="cb-card cb-card-pad" style="text-align:right;">
        <div style="font-size:0.75rem; color:var(--text-soft); text-transform:uppercase; letter-spacing:0.04em;">Trust score</div>
        <div style="font-family:var(--font-mono); font-size:1.6rem; font-weight:700; color:${color};">${res.data.score}</div>
        ${res.data.restricted ? `<div style="font-size:0.78rem; color:var(--neon-pink);">Limited to 1 active slot</div>` : ""}
      </div>`;
  } catch (e) { /* ok if not yet initialized */ }
}

async function cbLoadOrders() {
  const res = await cbApi.get("/api/student/orders");
  cbOrders = res.data;
  renderActiveOrder();
  renderHistory();
}

function renderActiveOrder() {
  const activeOrders = cbOrders.filter(o => ["PLACED", "CONFIRMED", "PREPARING", "READY"].includes(o.status));
  const host = document.getElementById("activeOrderHost");
  if (!activeOrders.length) { host.innerHTML = ""; return; }

  host.innerHTML = activeOrders.map(active => {
    const stageIndex = CB_STAGES.indexOf(active.status);
    const fillPct = stageIndex <= 0 ? 0 : (stageIndex / (CB_STAGES.length - 1)) * 84;

    const prepStartedAt = active.preparingAt || active.confirmedAt || active.placedAt;
    const countdownTarget = new Date(new Date(prepStartedAt).getTime() + active.estimatedPrepMinutes * 60000).toISOString();
    const showCountdown = ["PLACED", "CONFIRMED", "PREPARING"].includes(active.status);

    return `
    <div class="ticket" style="margin-bottom:20px;">
      <div class="ticket-head">
        <div>
          <div class="eyebrow">Order #${active.id}</div>
          <div class="ticket-token mono">${cbEscapeHtml(active.pickupToken)}</div>
          <div class="ticket-slot">Pickup slot: ${cbFormatSlot(active.pickupSlotStart, active.pickupSlotEnd)}</div>
          ${showCountdown ? `<div class="ticket-slot" data-countdown-target="${countdownTarget}" style="color:var(--neon-cyan); font-weight:600; margin-top:2px;">Calculating…</div>` : ""}
        </div>
        <div style="text-align:right;">
          <span class="badge-status status-${active.status}">${active.status.replace("_", " ")}</span>
          ${active.delayNotified ? `<div style="margin-top:8px; font-size:0.78rem; color:var(--neon-amber); font-weight:700;">⏱ Running a bit behind schedule</div>` : ""}
          <div style="margin-top:10px; display:flex; gap:8px; justify-content:flex-end;">
            ${active.status === "PLACED" ? `<button class="btn-cb btn-outline btn-sm" onclick="cbOpenEditOrder(${active.id})">Edit</button>` : ""}
            ${["PLACED", "CONFIRMED"].includes(active.status) ? `<button class="btn-cb btn-danger btn-sm" onclick="cbCancelOrder(${active.id})">Cancel order</button>` : ""}
          </div>
        </div>
      </div>
      <div class="ticket-perf"></div>
      <div class="ticket-track">
        <div class="ticket-track-fill" style="width:${fillPct}%;"></div>
        ${CB_STAGES.map((s, i) => `
          <div class="ticket-stage ${i < stageIndex ? "done" : i === stageIndex ? "current" : ""}">
            <div class="ticket-punch"></div>
            <div class="ticket-stage-label">${CB_STAGE_LABELS[s]}</div>
          </div>`).join("")}
      </div>
      ${active.status === "READY" ? `
        <div class="ticket-qr">
          <img src="${cbSafeImageUrl(active.qrCodeBase64)}" alt="Pickup QR code">
          <div>
            <div style="font-weight:600;">Ready for pickup!</div>
            <div style="font-size:0.85rem; color:var(--text-soft);">Show this QR code or read out ${cbEscapeHtml(active.pickupToken)} at the counter.</div>
          </div>
        </div>` : ""}
    </div>`;
  }).join("");
  cbTickCountdowns();
}

function renderHistory() {
  const drafts = {};
  document.querySelectorAll('[id^="ratingComment-"]').forEach(input => {
    drafts[input.id] = input.value;
  });
  const historical = cbOrders.filter(o => !["PLACED", "CONFIRMED", "PREPARING", "READY"].includes(o.status));
  document.getElementById("historyEmpty").style.display = historical.length ? "none" : "block";
  document.getElementById("historyHost").innerHTML = historical.map(o => `
    <div class="cb-card cb-card-pad" style="margin-bottom:12px;">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
        <div>
          <div style="font-weight:600;">Order #${o.id} · ${cbFormatMoney(o.totalAmount)}</div>
          <div style="font-size:0.82rem; color:var(--text-soft);">
            ${o.items.map(i => `${i.quantity}× ${cbEscapeHtml(i.menuItemName)}`).join(", ")}
          </div>
          <div style="font-size:0.78rem; color:var(--text-soft); margin-top:4px;">Placed ${cbFormatTime(o.placedAt)} · Slot ${cbFormatSlot(o.pickupSlotStart, o.pickupSlotEnd)}</div>
          ${o.cancelReason ? `<div style="font-size:0.78rem; color:var(--text-soft); margin-top:4px;">Reason: ${cbEscapeHtml(o.cancelReason)}</div>` : ""}
        </div>
        <div style="display:flex; align-items:center; gap:10px;">
          <span class="badge-status status-${o.status}">${o.status.replace("_", " ")}</span>
          <button class="btn-cb btn-outline btn-sm" onclick="cbReorder(${o.id})">Reorder</button>
        </div>
      </div>
      ${o.status === "PICKED_UP" ? `<div id="ratingHost-${o.id}" style="margin-top:14px; padding-top:14px; border-top:1px solid var(--border);"></div>` : ""}
    </div>`).join("");

  historical.filter(o => o.status === "PICKED_UP").forEach(o => cbLoadRatingForOrder(o.id).then(() => {
    const input = document.getElementById(`ratingComment-${o.id}`);
    if (input && drafts[input.id]) input.value = drafts[input.id];
  }));
}

async function cbLoadRatingForOrder(orderId) {
  const host = document.getElementById(`ratingHost-${orderId}`);
  if (!host) return;
  try {
    const res = await cbApi.get(`/api/student/orders/${orderId}/rating`);
    if (res.data) {
      host.innerHTML = `<div style="font-size:0.85rem;">${"⭐".repeat(res.data.stars)}${"☆".repeat(5 - res.data.stars)} ${res.data.comment ? `— "${cbEscapeHtml(res.data.comment)}"` : ""}</div>`;
    } else {
      renderRatingForm(host, orderId);
    }
  } catch (e) {
    renderRatingForm(host, orderId);
  }
}

function renderRatingForm(host, orderId) {
  host.innerHTML = `
    <div style="font-size:0.85rem; color:var(--text-soft); margin-bottom:8px;">How was this order?</div>
    <div style="display:flex; gap:6px; margin-bottom:10px;" id="starPicker-${orderId}">
      ${[1,2,3,4,5].map(n => `<button type="button" class="cb-rating-star" onclick="cbSetRatingStars(${orderId}, ${n})" data-star="${n}" aria-label="${n} star${n > 1 ? "s" : ""}" style="cursor:pointer; font-size:1.3rem; opacity:0.35; background:none; border:0; padding:2px;">⭐</button>`).join("")}
    </div>
    <input class="form-control" id="ratingComment-${orderId}" placeholder="Optional comment" style="margin-bottom:10px;">
    <button class="btn-cb btn-outline btn-sm" onclick="cbSubmitRating(${orderId})">Submit rating</button>`;
}

let cbRatingSelections = {};

function cbSetRatingStars(orderId, stars) {
  cbRatingSelections[orderId] = stars;
  document.querySelectorAll(`#starPicker-${orderId} button`).forEach(el => {
    el.style.opacity = Number(el.getAttribute("data-star")) <= stars ? "1" : "0.35";
  });
}

async function cbSubmitRating(orderId) {
  const stars = cbRatingSelections[orderId];
  if (!stars) { cbToast("Pick a star rating first", "error"); return; }
  const comment = document.getElementById(`ratingComment-${orderId}`).value.trim();
  try {
    await cbApi.post(`/api/student/orders/${orderId}/rating`, { stars, comment: comment || null });
    cbToast("Thanks for the feedback!", "success");
    cbLoadRatingForOrder(orderId);
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbCancelOrder(orderId) {
  if (!confirm("Cancel this order? Cancelling close to your slot may affect your trust score.")) return;
  const reason = prompt("Optional cancellation reason:", "");
  if (reason === null) return;
  try {
    await cbApi.post(`/api/student/orders/${orderId}/cancel`, { reason: reason.trim() || null });
    cbToast("Order cancelled successfully. Any applicable refund has been processed.", "success");
    cbLoadOrders();
    if (typeof cbRefreshWalletChip === "function") cbRefreshWalletChip();
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

async function cbReorder(orderId) {
  const order = cbOrders.find(o => o.id === orderId);
  if (!order) return;
  try {
    const menuItems = (await cbApi.get("/api/menu/items")).data;
    for (const item of order.items) {
      const menuItem = menuItems.find(m => m.id === item.menuItemId || m.name === item.menuItemName);
      if (menuItem && menuItem.available) {
        await cbApi.post("/api/student/cart/items", { menuItemId: menuItem.id, quantity: item.quantity, selectedCustomizations: item.selectedCustomizations || [] });
      }
    }
    cbToast("Added previous order to your cart", "success");
    window.location.href = "/student/dashboard.html";
  } catch (e) { cbToast(cbErrorMessage(e), "error"); }
}

/* ===================== Edit order before it's confirmed ===================== */

function cbOpenEditOrder(orderId) {
  const order = cbOrders.find(o => o.id === orderId);
  if (!order) return;
  cbEnsureEditOrderModal();
  document.getElementById("editOrderTitle").textContent = `Edit order #${order.id}`;
  document.getElementById("editOrderHost").innerHTML = order.items.map(item => `
    <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid var(--border);">
      <div>
        <div style="font-weight:600;">${cbEscapeHtml(item.menuItemName)}</div>
        <div style="font-size:0.8rem; color:var(--text-soft);">${cbFormatMoney(item.unitPrice)} each</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px;">
        <button class="btn-cb btn-ghost btn-sm" onclick="cbEditOrderQty(this, -1)">−</button>
        <span class="mono cb-edit-qty" data-item-id="${item.orderItemId || ''}">${item.quantity}</span>
        <button class="btn-cb btn-ghost btn-sm" onclick="cbEditOrderQty(this, 1)">+</button>
      </div>
    </div>`).join("");
  document.getElementById("editOrderModalBackdrop").dataset.orderId = orderId;
  document.getElementById("editOrderModalBackdrop").style.display = "flex";
}

function cbEnsureEditOrderModal() {
  if (document.getElementById("editOrderModalBackdrop")) return;
  const div = document.createElement("div");
  div.id = "editOrderModalBackdrop";
  div.style.cssText = "display:none; position:fixed; inset:0; background:rgba(0,0,0,.55); z-index:180; align-items:center; justify-content:center;";
  div.innerHTML = `
    <div class="cb-card cb-card-pad" style="width:420px; max-width:90vw;">
      <h3 id="editOrderTitle" style="margin-bottom:6px;">Edit order</h3>
      <p style="font-size:0.82rem; color:var(--text-soft); margin-bottom:14px;">Adjust quantities, or set to 0 to remove an item. Your wallet is charged or refunded the difference.</p>
      <div id="editOrderHost" style="margin-bottom:16px;"></div>
      <div style="display:flex; gap:10px;">
        <button class="btn-cb btn-cyan" style="flex:1;" onclick="cbSaveOrderEdit()">Save changes</button>
        <button class="btn-cb btn-ghost" onclick="cbCloseEditOrder()">Cancel</button>
      </div>
    </div>`;
  document.body.appendChild(div);
}

function cbEditOrderQty(btn, delta) {
  const span = btn.parentElement.querySelector(".cb-edit-qty");
  const newVal = Math.max(0, parseInt(span.textContent, 10) + delta);
  span.textContent = newVal;
}

function cbCloseEditOrder() {
  document.getElementById("editOrderModalBackdrop").style.display = "none";
}

async function cbSaveOrderEdit() {
  const backdrop = document.getElementById("editOrderModalBackdrop");
  const orderId = backdrop.dataset.orderId;
  const order = cbOrders.find(o => String(o.id) === String(orderId));
  if (!order) return;

  const rows = document.querySelectorAll("#editOrderHost .cb-edit-qty");
  const items = Array.from(rows).map((span, idx) => ({
    orderItemId: order.items[idx].orderItemId,
    quantity: parseInt(span.textContent, 10),
  })).filter(u => u.orderItemId != null);

  if (!items.length) { cbToast("Couldn't read item IDs -- try reloading the page", "error"); return; }

  try {
    await cbApi.patch(`/api/student/orders/${orderId}/items`, { items });
    cbToast("Order updated", "success");
    cbCloseEditOrder();
    cbLoadOrders();
    if (typeof cbRefreshWalletChip === "function") cbRefreshWalletChip();
  } catch (e) {
    cbToast(cbErrorMessage(e), "error");
  }
}
