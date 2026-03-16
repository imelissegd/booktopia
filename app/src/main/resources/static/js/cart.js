const container   = document.getElementById("cart");
const checkoutBtn = document.getElementById("checkoutBtn");
const userDropdown = document.getElementById("userDropdown");

let cartItems    = [];
let username     = null;
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
            showEmpty("Select a user to view their cart");
            checkoutBtn.style.display = "none";
        } else {
            checkoutBtn.style.display = "none";
            fetchCart();
        }
    });
} else {
    if (userDropdown) {
        const selectWrap = userDropdown?.closest(".select-wrap");
        if (selectWrap) selectWrap.style.display = "none";
    }
    username = currentUser?.username;
    if (!username) {
        showEmpty("Please log in to view your cart.");
        checkoutBtn.style.display = "none";
    } else {
        checkoutBtn.style.display = "inline-flex";
        fetchCart();
    }
}

function lockQtyInput(el) {
    el.addEventListener("keydown", e => {
        if (["-", ".", "+", "e", "E"].includes(e.key)) e.preventDefault();
    });
    el.addEventListener("paste", e => e.preventDefault());
    el.addEventListener("input", () => {
        el.value = el.value.replace(/[^0-9]/g, "");
    });
    el.addEventListener("blur", () => {
        if (el.value === "" || parseInt(el.value) < 0) el.value = 0;
    });
}

// --- Fetch ---
function fetchCart() {
    container.innerHTML = `<div class="table-loading">Loading cart…</div>`;
    fetch(`http://localhost:8080/api/cart/${username}`)
        .then(res => res.json())
        .then(data => {
            cartItems = data.items;
            renderCart();
        })
        .catch(err => {
            console.error(err);
            showEmpty("Error loading cart. Please try again.");
        });
}

// --- Render ---
function renderCart() {
    if (!cartItems.length) {
        showEmpty("This cart is empty.");
        if (!isAdmin) checkoutBtn.style.display = "none";
        setupPagination();
        return;
    }

    if (!isAdmin) checkoutBtn.style.display = "inline-flex";

    const start     = (currentPage - 1) * itemsPerPage;
    const end       = start + itemsPerPage;
    const pageItems = cartItems.slice(start, end);

    const rows = pageItems.map(item => `
    <tr data-item-id="${item.cartItemId}">
      ${!isAdmin ? `
      <td class="td-check">
        <input type="checkbox" class="table-checkbox selectItem" data-id="${item.cartItemId}">
      </td>` : ``}
      <td class="td-title">${item.bookTitle}</td>
      <td class="td-center">
        ${isAdmin ? `${item.quantity}` : `
        <div class="qty-control">
          <button class="qty-btn" onclick="changeQty(${item.cartItemId}, ${item.quantity - 1})" title="Decrease">−</button>
          <input
            type="number"
            class="qty-input"
            value="${item.quantity}"
            min="0"
            data-id="${item.cartItemId}"
            onchange="changeQty(${item.cartItemId}, parseInt(this.value))"
          >
          <button class="qty-btn" onclick="changeQty(${item.cartItemId}, ${item.quantity + 1})" title="Increase">+</button>
        </div>`}
      </td>
      <td class="td-price">₱${item.unitPrice}</td>
      <td class="td-price td-bold">₱${item.totalPrice}</td>
      <td class="td-actions">
        <button class="tbl-btn tbl-btn--ghost" style="background:var(--offwhite)" onclick="openViewBook(${item.bookId})">View Book</button>
        ${!isAdmin ? `<button class="tbl-btn tbl-btn--danger" onclick="removeItem(${item.cartItemId})">Remove</button>` : ``}
      </td>
    </tr>
  `).join("");

    const _cartTotal = cartItems.reduce((sum, i) => sum + i.quantity, 0);
    const _cartUnique = cartItems.length;
    const _cartSummaryEl = document.createElement("p");
    _cartSummaryEl.style.cssText = "font-size:0.78rem;color:var(--muted);margin:0 0 0.6rem;padding:0.75rem 0 0 20px";
    _cartSummaryEl.textContent = `${_cartUnique} item${_cartUnique !== 1 ? "s" : ""} • ${_cartTotal} book${_cartTotal !== 1 ? "s" : ""} total`;

    container.innerHTML = `
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            ${!isAdmin ? `
            <th class="td-check">
              <input type="checkbox" class="table-checkbox" id="selectAll" title="Select all">
            </th>` : ``}
            <th>Book Title</th>
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

    container.prepend(_cartSummaryEl);

    if (!isAdmin) {
        document.getElementById("selectAll").addEventListener("change", e => {
            document.querySelectorAll(".selectItem").forEach(cb => cb.checked = e.target.checked);
        });

        document.querySelectorAll(".qty-input").forEach(lockQtyInput);
    }

    setupPagination();
}

// --- Pagination ---
function setupPagination() {
    const pagination = document.getElementById("cartPagination");
    if (!pagination) return;
    pagination.innerHTML = "";

    if (!cartItems.length) return;

    const totalPages = Math.ceil(cartItems.length / itemsPerPage);
    if (totalPages <= 1) return;

    const prevBtn = document.createElement("button");
    prevBtn.textContent = "← Prev";
    prevBtn.className   = "page-btn";
    prevBtn.disabled    = currentPage === 1;
    prevBtn.onclick     = () => { if (currentPage > 1) { currentPage--; renderCart(); } };
    pagination.appendChild(prevBtn);

    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement("button");
        btn.textContent = i;
        btn.className   = i === currentPage ? "page-btn active-page" : "page-btn";
        btn.onclick     = () => { currentPage = i; renderCart(); };
        pagination.appendChild(btn);
    }

    const nextBtn = document.createElement("button");
    nextBtn.textContent = "Next →";
    nextBtn.className   = "page-btn";
    nextBtn.disabled    = currentPage === totalPages;
    nextBtn.onclick     = () => { if (currentPage < totalPages) { currentPage++; renderCart(); } };
    pagination.appendChild(nextBtn);
}

function changeItemsPerPage(val) {
    itemsPerPage = parseInt(val);
    currentPage  = 1;
    renderCart();
}

// --- Quantity editing ---
function changeQty(cartItemId, newQty) {
    // If qty drops to 0 or below, confirm removal
    if (newQty <= 0) {
        if (!confirm("Remove this item from your cart?")) {
            // Revert input display
            renderCart();
            return;
        }
        newQty = 0; // backend will treat 0 as removal
    }

    // Optimistically update UI
    const inputEl = document.querySelector(`.qty-input[data-id="${cartItemId}"]`);
    if (inputEl) inputEl.value = newQty === 0 ? 0 : newQty;

    const endpoint = newQty === 0
        ? `http://localhost:8080/api/cart/${username}/remove/${cartItemId}`
        : `http://localhost:8080/api/cart/${username}/update/${cartItemId}`;

    const fetchOptions = newQty === 0
        ? { method: "DELETE" }
        : {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ quantity: newQty })
        };

    fetch(endpoint, fetchOptions)
        .then(res => {
            if (!res.ok) throw new Error("Update failed");
            return res.json();
        })
        .then(data => {
            cartItems = data.items;
            renderCart();
            if (newQty === 0) showToast("Item removed from cart.", "success");
        })
        .catch(err => {
            console.error(err);
            showToast("Failed to update quantity. Please try again.", "error");
            fetchCart(); // re-sync with server
        });
}

// --- Checkout ---
checkoutBtn.addEventListener("click", () => {
    const selectedIds = Array.from(document.querySelectorAll(".selectItem:checked"))
        .map(cb => Number(cb.dataset.id));

    if (!selectedIds.length) {
        alert("Select at least one item to checkout.");
        return;
    }

    checkoutBtn.disabled = true;
    checkoutBtn.textContent = "Processing…";

    fetch(`http://localhost:8080/api/orders/${username}/checkout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ cartItemIds: selectedIds })
    })
        .then(res => {
            if (res.status === 400) return res.text().then(msg => { throw { stock: true, message: msg }; });
            if (!res.ok) throw { stock: false, message: "Checkout failed" };
            return res.json();
        })
        .then(data => {
            const txnId = data?.transactionId ?? "";
            showSuccessModal("cartModalContainer", {
                title: "Order Placed!",
                message: `Order ${txnId} has been placed successfully.`,
                primaryLabel: "View Orders",
                primaryHref: "orders.html",
                secondaryLabel: "Back to Cart",
                secondaryHref: "cart.html"
            });
        })
        .catch(err => {
            if (err.stock) {
                showCartStockError(err.message);
            } else {
                console.error(err);
                showToast(err.message || "Checkout failed. Please try again.", "error");
            }
        })
        .finally(() => {
            checkoutBtn.disabled = false;
            checkoutBtn.innerHTML = `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="20 12 20 22 4 22 4 12"/><rect x="2" y="7" width="20" height="5"/></svg>
                Checkout
            `;
        });
});
// --- Remove item ---
function removeItem(cartItemId) {
    if (!confirm("Remove this item from your cart?")) return;

    fetch(`http://localhost:8080/api/cart/${username}/remove/${cartItemId}`, { method: "DELETE" })
        .then(res => {
            if (!res.ok) throw new Error("Failed to remove item");
            return res.json();
        })
        .then(data => {
            cartItems = data.items;
            renderCart();
            showToast("Item removed from cart.", "success");
        })
        .catch(err => {
            console.error(err);
            showToast("Failed to remove item. Please try again.", "error");
        });
}

// --- View book modal (same style as catalog) ---
function openViewBook(bookId) {
    viewBookModal(bookId, {
        modalContainerId: "cartModalContainer",
        closeFn: "closeCartModal",
        loggedInUser: currentUser,
        hidePurchase: true  // if inside cart no need to show add to cart and buy now
    });
}
function closeCartModal() { document.getElementById("cartModalContainer").innerHTML = ""; }

// Reused from catalog
function formatCategory(cat) {
    return cat.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

// --- Helpers ---
function showEmpty(msg) {
    container.innerHTML = `
    <div class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
      <p>${msg}</p>
    </div>`;
}

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

function showCartStockError(serverMessage) {
    const match = serverMessage.match(/Available:\s*(\d+)/i);
    const available = match ? parseInt(match[1]) : null;
    const modalContainerId = "cartModalContainer";
    const container = document.getElementById(modalContainerId);

    // Stock is completely gone — out-of-stock modal, one button
    if (available === 0) {
        container.innerHTML = `
        <div class="modal-overlay">
          <div class="modal" style="text-align:center;max-width:380px">
            <div style="display:flex;justify-content:center;margin-bottom:0.75rem;color:var(--error)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="40" height="40" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
            </div>
            <h2 style="font-family:'DM Serif Display',serif;margin-bottom:0.25rem">Item Out of Stock</h2>
            <p style="font-size:0.88rem;color:var(--muted);margin-bottom:1.25rem">This item is currently unavailable. Browse other books in the catalog.</p>
            <div class="modal-action-row" style="justify-content:center">
              <button class="modal-btn-teal" onclick="window.location.href='catalog.html'">Back to Catalog</button>
            </div>
          </div>
        </div>`;
        container.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
        return;
    }

    // Some stock remains but not enough — show available count + adjust option
    const stockLine = available !== null
        ? `<p style="font-size:1.1rem;font-weight:700;color:var(--teal);margin:0.25rem 0 1rem">${available} remaining in stock</p>`
        : `<p style="font-size:0.88rem;color:var(--muted);margin-bottom:1rem">${serverMessage}</p>`;

    container.innerHTML = `
    <div class="modal-overlay">
      <div class="modal" style="text-align:center;max-width:380px">
        <div style="display:flex;justify-content:center;margin-bottom:0.75rem;color:var(--error)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="40" height="40" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <h2 style="font-family:'DM Serif Display',serif;margin-bottom:0.25rem">Not Enough Stock</h2>
        ${stockLine}
        <div class="modal-action-row">
          <button class="modal-btn-ghost" onclick="window.location.href='catalog.html'">Browse Books</button>
          <button class="modal-btn-teal" onclick="document.getElementById('cartModalContainer').innerHTML='';fetchCart()">Adjust Quantity</button>
        </div>
      </div>
    </div>`;
    container.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
}