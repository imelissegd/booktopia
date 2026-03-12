console.log("manage-users.js loaded");

const usersPerPage = 12;
let currentPage = 1;
let currentView = "list";
let users = [];
let filteredUsers = [];
let usersMap = {};
let loggedInUser = null;

// --- On page load ---
document.addEventListener("DOMContentLoaded", async () => {
  loggedInUser = (() => {
    try { return JSON.parse(localStorage.getItem("currentUser")); }
    catch { return null; }
  })();

  // Redirect if not admin
  if (loggedInUser?.role !== "ROLE_ADMIN") {
    window.location.href = "catalog.html";
    return;
  }

  setView("list", false);
  await fetchUsers();
  applyFilters();
});

// --- View toggle ---
function setView(view, rerender = true) {
  currentView = view;
  document.getElementById("gridViewBtn").classList.toggle("active", view === "grid");
  document.getElementById("listViewBtn").classList.toggle("active", view === "list");
  if (rerender) renderUsersPage(currentPage);
}

// --- Fetch all users ---
async function fetchUsers() {
  try {
    const res = await fetch("http://localhost:8080/api/users", { credentials: "include" });
    if (!res.ok) throw new Error("Failed to fetch users");
    users = await res.json();
    filteredUsers = [...users];
  } catch (err) {
    console.error(err);
    alert("Could not load users. See console.");
  }
}

// --- Apply filters ---
function applyFilters() {
  const query  = document.getElementById("searchInput").value.trim().toLowerCase();
  const role   = document.getElementById("roleFilter").value;
  const status = document.getElementById("statusFilter").value;

  filteredUsers = users.filter(user => {
    const matchesSearch =
        !query ||
        user.username?.toLowerCase().includes(query) ||
        user.email?.toLowerCase().includes(query);

    const matchesRole = !role || user.role === role;

    const matchesStatus =
        status === "all"         ? true
            : status === "active"      ? user.enabled !== false
                : status === "deactivated" ? user.enabled === false
                    : true;

    return matchesSearch && matchesRole && matchesStatus;
  });

  const countEl = document.getElementById("resultsCount");
  if (query || role || status !== "all") {
    countEl.textContent = `${filteredUsers.length} result${filteredUsers.length !== 1 ? "s" : ""} found`;
  } else {
    countEl.textContent = "";
  }

  currentPage = 1;
  renderUsersPage(currentPage);
  setupPagination();
}

// --- Render ---
function renderUsersPage(page) {
  const userList = document.getElementById("userList");
  userList.innerHTML = "";

  if (!filteredUsers.length) {
    userList.className = "";
    userList.innerHTML = "<p style='text-align:center;color:var(--muted);padding:2rem'>No users found.</p>";
    return;
  }

  const start = (page - 1) * usersPerPage;
  const end   = start + usersPerPage;
  const pageUsers = filteredUsers.slice(start, end);

  pageUsers.forEach(user => { usersMap[user.id] = user; });

  if (currentView === "list") {
    renderListView(userList, pageUsers);
  } else {
    renderGridView(userList, pageUsers);
  }
}

// --- Grid view ---
function renderGridView(userList, pageUsers) {
  userList.className = "";

  pageUsers.forEach(user => {
    const div = document.createElement("div");
    div.className = "user-card";
    if (user.enabled === false) div.style.opacity = "0.5";

    const roleBadgeClass = user.role === "ROLE_ADMIN" ? "role--admin" : "role--user";
    const roleLabel = user.role === "ROLE_ADMIN" ? "Admin" : "User";

    const deactivateRelist = user.enabled !== false
        ? `<button onclick="deactivateUser(${user.id})" style="flex:1;padding:7px 6px;border:none;border-radius:5px;font-family:inherit;font-weight:bold;font-size:0.72rem;cursor:pointer;background:var(--error);color:#fff">Deactivate</button>`
        : `<button onclick="reactivateUser(${user.id})" style="flex:1;padding:7px 6px;border:none;border-radius:5px;font-family:inherit;font-weight:bold;font-size:0.72rem;cursor:pointer;background:var(--teal);color:#fff">Reactivate</button>`;

    const promoteBtn = user.role === "ROLE_ADMIN"
        ? `<button onclick="changeRole(${user.id}, 'ROLE_USER')" style="width:100%;padding:7px 6px;border:1.5px solid var(--teal);border-radius:5px;font-family:inherit;font-weight:bold;font-size:0.72rem;cursor:pointer;background:transparent;color:var(--teal)">Demote to User</button>`
        : `<button onclick="changeRole(${user.id}, 'ROLE_ADMIN')" style="width:100%;padding:7px 6px;border:1.5px solid var(--teal);border-radius:5px;font-family:inherit;font-weight:bold;font-size:0.72rem;cursor:pointer;background:transparent;color:var(--teal)">Promote to Admin</button>`;

    div.innerHTML = `
      <div class="user-avatar">
        <svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
      </div>
      <div class="user-name">${user.username}</div>
      <div class="user-email">${user.email}</div>
      <span class="user-role-badge ${roleBadgeClass}">${roleLabel}</span>
      <div class="user-card-actions">
        <button class="btn-view-user" onclick="editUser(${user.id})">Edit</button>
        ${promoteBtn}
        <div class="book-action-row">${deactivateRelist}</div>
      </div>
    `;

    userList.appendChild(div);
  });
}

// --- List view ---
function renderListView(userList, pageUsers) {
  userList.className = "list-view";

  const rows = pageUsers.map(user => {
    const roleBadgeClass = user.role === "ROLE_ADMIN" ? "role--admin" : "role--user";
    const roleLabel = user.role === "ROLE_ADMIN" ? "Admin" : "User";

    const statusBadge = user.enabled !== false
        ? `<span class="list-badge" style="border-color:var(--teal);color:var(--teal)">Active</span>`
        : `<span class="list-badge" style="border-color:var(--error);color:var(--error)">Deactivated</span>`;

    const promoteBtn = user.role === "ROLE_ADMIN"
        ? `<button class="tbl-btn tbl-btn--ghost" style="border-color:var(--teal);color:var(--teal)" onclick="changeRole(${user.id}, 'ROLE_USER')">Demote</button>`
        : `<button class="tbl-btn tbl-btn--ghost" style="border-color:var(--teal);color:var(--teal)" onclick="changeRole(${user.id}, 'ROLE_ADMIN')">Promote</button>`;

    const deactivateRelist = user.enabled !== false
        ? `<button class="tbl-btn tbl-btn--danger" onclick="deactivateUser(${user.id})">Deactivate</button>`
        : `<button class="tbl-btn tbl-btn--ghost" style="border-color:var(--teal);color:var(--teal)" onclick="reactivateUser(${user.id})">Reactivate</button>`;

    return `
      <tr style="${user.enabled === false ? 'opacity:0.55' : ''}">
        <td>
          <div style="display:flex;align-items:center;justify-content:center;width:38px;height:38px;border-radius:50%;background:var(--offwhite);border:1.5px solid var(--border);flex-shrink:0">
            <svg viewBox="0 0 24 24" fill="none" stroke="var(--muted)" stroke-width="1.6" width="18" height="18"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
        </td>
        <td>
          <div class="list-title">${user.username}</div>
          <div class="list-author">${user.email}</div>
        </td>
        <td><span class="user-role-badge ${roleBadgeClass}">${roleLabel}</span></td>
        <td>${statusBadge}</td>
        <td><div class="list-actions">
          <button class="tbl-btn tbl-btn--ghost" onclick="editUser(${user.id})">Edit</button>
          ${promoteBtn}
          ${deactivateRelist}
        </div></td>
      </tr>`;
  }).join("");

  userList.innerHTML = `
    <table class="list-table">
      <thead>
        <tr>
          <th></th>
          <th>Username / Email</th>
          <th>Role</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>`;
}

// --- Pagination ---
function setupPagination() {
  const pagination = document.getElementById("pagination");
  pagination.innerHTML = "";
  if (!filteredUsers.length) return;

  const totalPages = Math.ceil(filteredUsers.length / usersPerPage);
  for (let i = 1; i <= totalPages; i++) {
    const btn = document.createElement("button");
    btn.textContent = i;
    btn.className = i === currentPage ? "active-page" : "";
    btn.onclick = () => {
      currentPage = i;
      renderUsersPage(currentPage);
      setupPagination();
    };
    pagination.appendChild(btn);
  }
}

// --- Edit user ---
function editUser(userId) {
  window.location.href = `edit-user.html?id=${userId}`;
}

// --- Deactivate user ---
function deactivateUser(userId) {
  const user = usersMap[userId];
  const name = user?.username ?? `User #${userId}`;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" style="text-align:center">
        <div style="display:flex;justify-content:center;margin-bottom:0.75rem;color:var(--error)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="36" height="36" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <h2 style="font-family:'DM Serif Display',serif;margin-bottom:0.5rem">Deactivate Account?</h2>
        <p style="color:var(--muted);font-size:0.88rem;margin-bottom:1.2rem">
          <strong>${name}</strong> will lose access to their account.
        </p>
        <div class="modal-action-row">
          <button class="modal-btn-ghost" onclick="closeModal()">Cancel</button>
          <button style="flex:1;padding:8px 12px;background:var(--error);color:#fff;border:none;border-radius:5px;font-weight:600;font-size:0.82rem;cursor:pointer"
            onclick="confirmDeactivate(${userId})">Yes, Deactivate</button>
        </div>
      </div>
    </div>`;
  document.querySelector("#modalContainer .modal").addEventListener("click", e => e.stopPropagation());
}

function confirmDeactivate(userId) {
  fetch(`http://localhost:8080/api/users/${userId}/deactivate`, { method: "PATCH", credentials: "include" })
      .then(res => { if (!res.ok) throw new Error(); return res.json(); })
      .then(updated => {
        const idx = users.findIndex(u => u.id === userId);
        if (idx !== -1) users[idx] = updated;
        usersMap[userId] = updated;
        closeModal();
        applyFilters();
        showToast(`"${updated.username}" has been deactivated.`, "success");
      })
      .catch(() => { closeModal(); showToast("Failed to deactivate user.", "error"); });
}

// --- Reactivate user ---
function reactivateUser(userId) {
  fetch(`http://localhost:8080/api/users/${userId}/reactivate`, { method: "PATCH", credentials: "include" })
      .then(res => { if (!res.ok) throw new Error(); return res.json(); })
      .then(updated => {
        const idx = users.findIndex(u => u.id === userId);
        if (idx !== -1) users[idx] = updated;
        usersMap[userId] = updated;
        applyFilters();
        showToast(`"${updated.username}" has been reactivated.`, "success");
      })
      .catch(() => showToast("Failed to reactivate user.", "error"));
}

// --- Change role ---
function changeRole(userId, newRole) {
  const user = usersMap[userId];
  const name = user?.username ?? `User #${userId}`;
  const action = newRole === "ROLE_ADMIN" ? "Promote" : "Demote";
  const desc = newRole === "ROLE_ADMIN"
      ? `<strong>${name}</strong> will become an Admin.`
      : `<strong>${name}</strong> will be demoted to a regular User.`;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" style="text-align:center">
        <h2 style="font-family:'DM Serif Display',serif;margin-bottom:0.5rem">${action} User?</h2>
        <p style="color:var(--muted);font-size:0.88rem;margin-bottom:1.2rem">${desc}</p>
        <div class="modal-action-row">
          <button class="modal-btn-ghost" onclick="closeModal()">Cancel</button>
          <button style="flex:1;padding:8px 12px;background:var(--teal);color:#fff;border:none;border-radius:5px;font-weight:600;font-size:0.82rem;cursor:pointer"
            onclick="confirmChangeRole(${userId}, '${newRole}')">Yes, ${action}</button>
        </div>
      </div>
    </div>`;
  document.querySelector("#modalContainer .modal").addEventListener("click", e => e.stopPropagation());
}

function confirmChangeRole(userId, newRole) {
  fetch(`http://localhost:8080/api/users/${userId}/role`, {
    method: "PATCH",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ role: newRole })
  })
      .then(res => {
        if (res.status === 400) return res.text().then(msg => { throw new Error(msg); });
        if (!res.ok) throw new Error("Failed to update role.");
        return res.json();
      })
      .then(updated => {
        const idx = users.findIndex(u => u.id === userId);
        if (idx !== -1) users[idx] = updated;
        usersMap[userId] = updated;
        closeModal();
        applyFilters();
        const label = newRole === "ROLE_ADMIN" ? "promoted to Admin" : "demoted to User";
        showToast(`"${updated.username}" has been ${label}.`, "success");
      })
      .catch(err => {
        closeModal();
        showToast(err.message || "Failed to update role.", "error");
      });
}
// --- Modal ---
function closeModal() {
  document.getElementById("modalContainer").innerHTML = "";
}

// --- Toast ---
function showToast(msg, type = "success") {
  const toast = document.createElement("div");
  toast.className = `toast toast--${type}`;
  toast.textContent = msg;
  document.body.appendChild(toast);
  setTimeout(() => toast.classList.add("toast--visible"), 10);
  setTimeout(() => {
    toast.classList.remove("toast--visible");
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}