const container    = document.getElementById("orders");
const userDropdown = document.getElementById("userDropdown");

let username     = null;
let allOrders    = [];
let currentPage  = 1;
let itemsPerPage = 5;

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
        .then(data => {
            allOrders   = data;
            currentPage = 1;
            renderOrders();
        })
        .catch(err => {
            console.error(err);
            showEmpty("Error loading order history. Please try again.");
        });
}

// --- Render ---
function renderOrders() {
    if (!allOrders.length) {
        showEmpty("No orders found.");
        setupPagination();
        return;
    }

    container.innerHTML = "";

    const start      = (currentPage - 1) * itemsPerPage;
    const end        = start + itemsPerPage;
    const pageOrders = allOrders.slice(start, end);

    const _totalItems  = allOrders.reduce((sum, o) => sum + o.items.reduce((s, i) => s + i.quantity, 0), 0);
    const _countBanner = document.createElement("p");
    _countBanner.style.cssText = "font-size:0.78rem;color:var(--muted);margin:0 0 0.75rem;padding:0.75rem 20px 0";
    _countBanner.textContent   = `${allOrders.length} order${allOrders.length !== 1 ? "s" : ""} • ${_totalItems} item${_totalItems !== 1 ? "s" : ""} total`;
    container.appendChild(_countBanner);

    pageOrders.forEach(order => {
        const rows = order.items.map(item => `
      <tr>
        <td class="td-title" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${item.bookTitle}</td>
        <td style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${item.author || "—"}</td>
        <td class="td-center">${item.quantity}</td>
        <td class="td-price">₱${item.unitPrice}</td>
        <td class="td-price td-bold">₱${item.totalPrice}</td>
        <td class="td-actions">
          <button class="tbl-btn tbl-btn--ghost" style="background:var(--offwhite)" onclick="openViewBook(${item.bookId})">View Book</button>
        </td>
      </tr>
    `).join("");

        const statusClass = {
            PENDING:   "status--pending",
            COMPLETED: "status--completed",
            CANCELLED: "status--cancelled",
        }[order.status] || "status--pending";

        const orderTotal = order.items.reduce((sum, i) => sum + parseFloat(i.totalPrice), 0);

        const orderCard = document.createElement("div");
        orderCard.className = "order-card";
        orderCard.innerHTML = `
      <div class="order-card-header">
        <div class="order-card-meta">
          <span class="order-id">Order ${order.transactionId}</span>
        </div>
        <span style="margin-left:auto;display:flex;align-items:center;gap:1.25rem">
          <span style="font-size:0.88rem;font-weight:600;color:var(--teal)">Total: ₱${orderTotal.toFixed(2)}</span>
          <span class="order-date" style="font-size:0.78rem;color:var(--muted)">Order Placed: ${new Date(order.orderDate).toLocaleDateString("en-PH", { year:"numeric", month:"long", day:"numeric" })}</span>
        </span>
      </div>
      <div class="table-wrap">
        <table class="data-table" style="table-layout:fixed;width:100%">
          <colgroup>
            <col style="width:30%">
            <col style="width:20%">
            <col style="width:8%">
            <col style="width:14%">
            <col style="width:14%">
            <col style="width:14%">
          </colgroup>
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

    setupPagination();
}

// --- Pagination ---
function setupPagination() {
    const pagination = document.getElementById("ordersPagination");
    if (!pagination) return;
    pagination.innerHTML = "";

    if (!allOrders.length) return;

    const totalPages = Math.ceil(allOrders.length / itemsPerPage);
    if (totalPages <= 1) return;

    const prevBtn = document.createElement("button");
    prevBtn.textContent = "← Prev";
    prevBtn.className   = "page-btn";
    prevBtn.disabled    = currentPage === 1;
    prevBtn.onclick     = () => { if (currentPage > 1) { currentPage--; renderOrders(); } };
    pagination.appendChild(prevBtn);

    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement("button");
        btn.textContent = i;
        btn.className   = i === currentPage ? "page-btn active-page" : "page-btn";
        btn.onclick     = () => { currentPage = i; renderOrders(); };
        pagination.appendChild(btn);
    }

    const nextBtn = document.createElement("button");
    nextBtn.textContent = "Next →";
    nextBtn.className   = "page-btn";
    nextBtn.disabled    = currentPage === totalPages;
    nextBtn.onclick     = () => { if (currentPage < totalPages) { currentPage++; renderOrders(); } };
    pagination.appendChild(nextBtn);
}

function changeItemsPerPage(val) {
    itemsPerPage = parseInt(val);
    currentPage  = 1;
    renderOrders();
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