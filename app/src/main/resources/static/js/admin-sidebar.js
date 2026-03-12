/* admin-sidebar.js — inject admin sidebar on all admin pages */

(function () {
    const currentPage = window.location.pathname.split("/").pop() || "admin-dashboard.html";
    const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";

    // On catalog.html, only show sidebar in manage mode
    if (currentPage === "catalog.html" && !isManageMode) return;

    // On any non-admin page that isn't in our list, bail out
    const adminPages = [
        "admin-dashboard.html", "manage-users.html", "add-user.html", "edit-user.html",
        "add-book.html", "edit-book.html", "cart.html", "orders.html", "catalog.html"
    ];
    if (!adminPages.includes(currentPage)) return;

    // Also guard: only admins should see the sidebar
    const currentUser = (() => { try { return JSON.parse(localStorage.getItem("currentUser")); } catch { return null; } })();
    if (currentUser?.role !== "ROLE_ADMIN") return;

    const navItems = [
        {
            section: "Books",
            icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="13" height="13"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>`,
            links: [
                { label: "Manage Books", href: "catalog.html?mode=manage", match: "catalog.html" },
                { label: "Add Book",     href: "add-book.html",            match: "add-book.html" },
            ]
        },
        {
            section: "Users",
            icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="13" height="13"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>`,
            links: [
                { label: "Manage Users", href: "manage-users.html", match: "manage-users.html" },
                { label: "Add User",     href: "add-user.html",     match: "add-user.html" },
            ]
        },
        {
            section: "Orders & Carts",
            icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="13" height="13"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>`,
            links: [
                { label: "View Carts",  href: "cart.html",   match: "cart.html" },
                { label: "View Orders", href: "orders.html", match: "orders.html" },
            ]
        }
    ];

    function isActive(match) {
        if (currentPage === match) return true;
        if (currentPage === "edit-user.html" && match === "manage-users.html") return true;
        if (currentPage === "edit-book.html" && match === "catalog.html") return true;
        return false;
    }

    const sectionsHTML = navItems.map(group => `
    <div class="sidebar-section">
      <div class="sidebar-section-header">
        ${group.icon}
        <span class="sidebar-section-label">${group.section}</span>
      </div>
      ${group.links.map(link => `
        <a href="${link.href}" class="sidebar-link${isActive(link.match) ? " sidebar-link--active" : ""}">
          ${link.label}
        </a>`).join("")}
    </div>`).join("");

    // Inject CSS immediately (before DOMContentLoaded) to avoid flash
    const style = document.createElement("style");
    style.textContent = `
    body { display: flex; flex-direction: column; min-height: 100vh; }

    .admin-layout {
      display: flex;
      flex: 1;
      min-height: 0;
    }

    .admin-sidebar {
      width: 220px;
      flex-shrink: 0;
      background: var(--surface);
      border-right: 1.5px solid var(--border);
      position: sticky;
      top: 64px;
      height: calc(100vh - 64px);
      overflow-y: auto;
      z-index: 50;
    }

    .sidebar-inner {
      padding: 1.25rem 0.75rem;
      display: flex;
      flex-direction: column;
    }

    .sidebar-home {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      padding: 0.55rem 0.75rem;
      border-radius: 7px;
      font-size: 0.82rem;
      font-weight: 600;
      color: var(--charcoal);
      text-decoration: none;
      transition: background 0.15s, color 0.15s;
      margin-bottom: 0.25rem;
    }
    .sidebar-home:hover { background: var(--offwhite); color: var(--teal); }
    .sidebar-home--active { background: var(--teal-glow); color: var(--teal); }

    .sidebar-divider {
      height: 1.5px;
      background: var(--border);
      margin: 0.5rem 0.25rem 0.75rem;
    }

    .sidebar-section { margin-bottom: 1rem; }

    .sidebar-section-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0 0.75rem 0.4rem;
    }
    .sidebar-section-header svg { stroke: var(--muted); flex-shrink: 0; }

    .sidebar-section-label {
      font-size: 0.62rem;
      font-weight: 700;
      letter-spacing: 0.1em;
      text-transform: uppercase;
      color: var(--muted);
    }

    .sidebar-link {
      display: block;
      padding: 0.48rem 0.75rem 0.48rem 1.85rem;
      border-radius: 7px;
      font-size: 0.82rem;
      font-weight: 500;
      color: var(--charcoal);
      text-decoration: none;
      transition: background 0.15s, color 0.15s;
    }
    .sidebar-link:hover { background: var(--offwhite); color: var(--teal); }
    .sidebar-link--active { background: var(--teal-glow); color: var(--teal); font-weight: 600; }

    .admin-content {
      flex: 1;
      min-width: 0;
      overflow-x: hidden;
    }

    .sidebar-overlay {
      display: none;
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.35);
      z-index: 199;
    }
    .sidebar-overlay.visible { display: block; }

    .sidebar-mobile-btn {
      display: none;
      position: fixed;
      bottom: 1.5rem;
      left: 1rem;
      width: 44px;
      height: 44px;
      background: var(--teal);
      color: #fff;
      border: none;
      border-radius: 50%;
      box-shadow: 0 4px 16px rgba(0,137,123,0.35);
      cursor: pointer;
      z-index: 300;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;
    }
    .sidebar-mobile-btn:hover { background: var(--teal-dark); }

    @media (max-width: 768px) {
      .admin-sidebar {
        position: fixed;
        top: 64px;
        left: 0;
        height: calc(100vh - 64px);
        z-index: 200;
        transform: translateX(-100%);
        transition: transform 0.25s cubic-bezier(.22,1,.36,1);
        box-shadow: 4px 0 20px rgba(0,0,0,0.1);
      }
      .admin-sidebar.sidebar--open { transform: translateX(0); }
      .sidebar-mobile-btn { display: flex; }
    }
  `;
    document.head.appendChild(style);

    document.addEventListener("DOMContentLoaded", () => {
        // Build sidebar element
        const sidebar = document.createElement("aside");
        sidebar.className = "admin-sidebar";
        sidebar.innerHTML = `
      <div class="sidebar-inner">
        <a href="admin-dashboard.html" class="sidebar-home${currentPage === "admin-dashboard.html" ? " sidebar-home--active" : ""}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="15" height="15">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
          </svg>
          Dashboard
        </a>
        <div class="sidebar-divider"></div>
        ${sectionsHTML}
      </div>`;

        // Find the navbar (inserted by navbar.js)
        const navbar = document.querySelector("nav.navbar");

        // Create layout wrapper
        const layout = document.createElement("div");
        layout.className = "admin-layout";

        const content = document.createElement("div");
        content.className = "admin-content";

        // Move everything that isn't the navbar into .admin-content
        Array.from(document.body.children).forEach(child => {
            if (child === navbar) return;
            content.appendChild(child);
        });

        layout.appendChild(sidebar);
        layout.appendChild(content);
        document.body.appendChild(layout);

        // Mobile overlay
        const overlay = document.createElement("div");
        overlay.className = "sidebar-overlay";
        document.body.appendChild(overlay);

        // Mobile toggle button
        const mobileBtn = document.createElement("button");
        mobileBtn.className = "sidebar-mobile-btn";
        mobileBtn.setAttribute("aria-label", "Toggle sidebar");
        mobileBtn.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>`;
        document.body.appendChild(mobileBtn);

        mobileBtn.addEventListener("click", () => {
            sidebar.classList.toggle("sidebar--open");
            overlay.classList.toggle("visible");
        });

        overlay.addEventListener("click", () => {
            sidebar.classList.remove("sidebar--open");
            overlay.classList.remove("visible");
        });
    });
})();