const container    = document.getElementById("orders");
const userDropdown = document.getElementById("userDropdown");

let username = null;

const currentUser = (() => {
    try { return JSON.parse(localStorage.getItem("currentUser")); }
    catch { return null; }
})();

const isAdmin = currentUser?.role === "ROLE_ADMIN";

if (isAdmin) {
    if (userDropdown) userDropdown.style.display = "";
    populateUserDropdown("userDropdown");

    userDropdown.addEventListener("change", () => {
        username = userDropdown.value;
        if (!username) {
            showEmpty("Select a user to view their order history.");
        } else {
            fetchOrders();
        }
    });
} else {
    if (userDropdown) {
        const selectWrap = userDropdown?.closest(".select-wrap");
        if (selectWrap) selectWrap.style.display = "none";
    }
    username = currentUser?.username;
    if (!username) {
        showEmpty("Please log in to view your order history.");
    } else {
        fetchOrders();
    }
}

// --- Fetch ---
function fetchOrders() {
    container.innerHTML = `<div class="table-loading">Loading orders…</div>`;
    fetch(`http://localhost:8080/api/orders/${username}/history`)
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch orders");
            return res.json();
        })
        .then(data => renderOrders(data))
        .catch(err => {
            console.error(err);
            showEmpty("Error loading order history. Please try again.");
        });
}

// --- Render ---
function renderOrders(data) {
    if (!data.length) {
        showEmpty("No orders found.");
        return;
    }

    container.innerHTML = "";

    // --- Order count summary ---
    const _totalItems = data.reduce((sum, o) => sum + o.items.reduce((s, i) => s + i.quantity, 0), 0);
    const _countBanner = document.createElement("p");
    _countBanner.style.cssText = "font-size:0.78rem;color:var(--muted);margin:0 0 0.75rem;padding:0.75rem 20px 0";
    _countBanner.textContent = `${data.length} order${data.length !== 1 ? "s" : ""} • ${_totalItems} item${_totalItems !== 1 ? "s" : ""} total`;
    container.appendChild(_countBanner);

    data.forEach(order => {
        const rows = order.items.map(item => `
      <tr>
        <td class="td-title">${item.bookTitle}</td>
        <td>${item.author || "—"}</td>
        <td class="td-center">${item.quantity}</td>
        <td class="td-price">₱${item.unitPrice}</td>
        <td class="td-price td-bold">₱${item.totalPrice}</td>
        <td class="td-actions">
          <button class="tbl-btn tbl-btn--ghost" onclick="openViewBook(${item.bookId})">View Book</button>
        </td>
      </tr>
    `).join("");

        const statusClass = {
            PENDING:   "status--pending",
            COMPLETED: "status--completed",
            CANCELLED: "status--cancelled",
        }[order.status] || "status--pending";

        const orderCard = document.createElement("div");
        orderCard.className = "order-card";
        orderCard.innerHTML = `
      <div class="order-card-header">
        <div class="order-card-meta">
          <span class="order-id">Order ${order.transactionId}</span>
          <span class="order-date">${new Date(order.orderDate).toLocaleDateString("en-PH", { year:"numeric", month:"long", day:"numeric" })}</span>
        </div>
        ${isAdmin ? `<span class="status-badge ${statusClass}">${order.status}</span>` : ``}
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Book Title</th>
              <th>Author</th>
              <th class="td-center">Qty</th>
              <th>Unit Price</th>
              <th>Total</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    `;
        container.appendChild(orderCard);
    });
}

// --- View Book Modal ---
function openViewBook(bookId) {
    viewBookModal(bookId, {
        modalContainerId: "ordersModalContainer", closeFn: "closeOrdersModal", loggedInUser: currentUser });
}
function closeOrdersModal() { document.getElementById("ordersModalContainer").innerHTML = ""; }
function formatCategory(cat) {
    return cat.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

function showEmpty(msg) {
    container.innerHTML = `
    <div class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
      <p>${msg}</p>
    </div>`;
}