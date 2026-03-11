/* navbar.js — inject shared navbar on every page */

function renderNavbar() {
    const loggedInUser = (() => {
        try { return JSON.parse(localStorage.getItem("currentUser")); }
        catch { return null; }
    })();

    const isAdmin = loggedInUser?.role === "ROLE_ADMIN";

    const nav = document.createElement("nav");
    nav.className = "navbar";
    nav.innerHTML = `
    <a class="navbar-brand" href="catalog.html">
      <span class="navbar-logo">Book<span class="navbar-logo-accent">topia</span></span>
      <span class="navbar-tagline">Your modern bookshop</span>
    </a>

    <div class="navbar-actions">
      ${loggedInUser ? `
        <a class="nav-btn nav-btn--ghost" href="catalog.html">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
          Browse Books
        </a>
        ${isAdmin ? `
          <a class="nav-btn nav-btn--ghost" href="admin-dashboard.html">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
            Admin Dashboard
          </a>
        ` : `
          <a class="nav-btn nav-btn--ghost" href="cart.html">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
            View Cart
          </a>
          <a class="nav-btn nav-btn--ghost" href="orders.html">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            View Order History
          </a>
        `}
        <div class="navbar-user">
          <span class="navbar-user-info">
            <span class="navbar-username">Logged in as: ${loggedInUser.username}</span>
            <span class="navbar-role">Role: ${loggedInUser.role.replace("ROLE_", "")}</span>
          </span>
          <button class="nav-btn nav-btn--logout" id="navLogoutBtn">Logout</button>
        </div>
      ` : `
        <a class="nav-btn nav-btn--ghost" href="login.html">Login</a>
        <a class="nav-btn nav-btn--primary" href="signup.html">Register</a>
      `}
    </div>

    <button class="navbar-hamburger" id="navHamburger" aria-label="Toggle menu">
      <span></span><span></span><span></span>
    </button>
  `;

    document.body.prepend(nav);

    // Logout
    const logoutBtn = document.getElementById("navLogoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", async () => {
            await fetch("http://localhost:8080/logout", { method: "POST", credentials: "include" })
                .catch(() => {});
            localStorage.removeItem("currentUser");
            window.location.href = "catalog.html";
        });
    }

    // Hamburger toggle
    const hamburger = document.getElementById("navHamburger");
    const actions = nav.querySelector(".navbar-actions");
    hamburger.addEventListener("click", () => {
        actions.classList.toggle("navbar-actions--open");
        hamburger.classList.toggle("is-open");
    });
}

document.addEventListener("DOMContentLoaded", renderNavbar);