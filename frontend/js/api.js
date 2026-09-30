/* =====================================================================
   CampusBite API client
   - Access token lives only in JavaScript memory (not localStorage)
   - Rotating refresh token is an httpOnly cookie
   - Transparently refreshes the short-lived access token when needed
   ===================================================================== */

const CB_API_BASE = window.CB_CONFIG.apiBase;
const CB_WS_BASE = window.CB_CONFIG.wsBase;

let cbAccessToken = null;
let cbRefreshingPromise = null;

// Remove the old pre-v4 access-token persistence if a browser still has it.
try { localStorage.removeItem("cb_access_token"); } catch (_) {}

const CBAuth = {
  getAccessToken() { return cbAccessToken; },
  getUser() {
    try {
      const raw = sessionStorage.getItem("cb_user") || localStorage.getItem("cb_user");
      return raw ? JSON.parse(raw) : null;
    } catch (_) { return null; }
  },
  setSession(authResponse) {
    cbAccessToken = authResponse.accessToken || null;
    if (authResponse.user) {
      sessionStorage.setItem("cb_user", JSON.stringify(authResponse.user));
      localStorage.removeItem("cb_user");
    }
    // The refresh token is never stored here -- it arrives as an httpOnly
    // cookie the browser manages automatically, invisible to this script.
  },
  clearSession() {
    cbAccessToken = null;
    sessionStorage.removeItem("cb_user");
    localStorage.removeItem("cb_access_token");
    localStorage.removeItem("cb_user");
  },
  isLoggedIn() { return !!this.getUser(); },
  requireRole(roles) {
    const user = this.getUser();
    if (!user || !roles.includes(user.role)) {
      window.location.href = "/index.html";
      return null;
    }
    return user;
  },
  async ensureAccessToken() {
    if (cbAccessToken) return cbAccessToken;
    if (!this.getUser()) return null;
    if (!cbRefreshingPromise) {
      cbRefreshingPromise = axios.post(`${CB_API_BASE}/api/auth/refresh`, {}, { withCredentials: true })
        .then((res) => {
          this.setSession(res.data);
          return res.data;
        })
        .catch((e) => {
          this.clearSession();
          window.location.href = "/index.html";
          throw e;
        })
        .finally(() => { cbRefreshingPromise = null; });
    }
    const res = await cbRefreshingPromise;
    return res.accessToken;
  },
  logoutRedirect() {
    this.clearSession();
    // No body needed -- the browser sends the httpOnly cookie automatically.
    axios.post(`${CB_API_BASE}/api/auth/logout`, {}, { withCredentials: true }).catch(() => {});
    window.location.href = "/index.html";
  }
};

const cbApi = axios.create({ baseURL: CB_API_BASE, withCredentials: true });

cbApi.interceptors.request.use(async (config) => {
  const isAuthEndpoint = config.url?.includes("/api/auth/");
  if (!isAuthEndpoint) {
    const token = await CBAuth.ensureAccessToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

cbApi.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const isAuthEndpoint = original?.url?.includes("/api/auth/");
    if (error.response && error.response.status === 401 && !original._retry && !isAuthEndpoint) {
      original._retry = true;
      try {
        const token = await CBAuth.ensureAccessToken();
        if (!token) return Promise.reject(error);
        original.headers.Authorization = `Bearer ${token}`;
        return cbApi(original);
      } catch (e) {
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  }
);

function cbErrorMessage(error) {
  return error?.response?.data?.message || "Something went wrong. Please try again.";
}

function cbToast(message, type = "default") {
  let el = document.getElementById("cb-toast");
  if (!el) {
    el = document.createElement("div");
    el.id = "cb-toast";
    el.className = "toast-cb";
    el.setAttribute("role", "status");
    el.setAttribute("aria-live", "polite");
    document.body.appendChild(el);
  }
  el.textContent = message;
  el.className = `toast-cb show ${type === "error" ? "error" : type === "success" ? "success" : ""}`;
  clearTimeout(window._cbToastTimer);
  window._cbToastTimer = setTimeout(() => { el.classList.remove("show"); }, 3200);
}

function cbFormatMoney(amount) {
  return `₹${Number(amount).toFixed(2)}`;
}

function cbFormatTime(isoString) {
  if (!isoString) return "--";
  const d = new Date(isoString);
  return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
}

function cbFormatSlot(startIso, endIso) {
  const s = new Date(startIso), e = new Date(endIso);
  const opts = { hour: "2-digit", minute: "2-digit" };
  return `${s.toLocaleTimeString([], opts)} – ${e.toLocaleTimeString([], opts)}`;
}
