const container   = document.getElementById("cart");
const checkoutBtn = document.getElementById("checkoutBtn");
const userDropdown = document.getElementById("userDropdown");

let cartItems = [];
let username  = null;

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
        return;
    }

    if (!isAdmin) checkoutBtn.style.display = "inline-flex";

    const rows = cartItems.map(item => `
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
        <button class="tbl-btn tbl-btn--ghost" onclick="openViewBook(${item.bookId})">View Book</button>
        ${!isAdmin ? `<button class="tbl-btn tbl-btn--danger" onclick="removeItem(${item.cartItemId})">Remove</button>` : ``}
      </td>
    </tr>
  `).join("");

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

    if (!isAdmin) {
        document.getElementById("selectAll").addEventListener("change", e => {
            document.querySelectorAll(".selectItem").forEach(cb => cb.checked = e.target.checked);
        });
    }
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
            if (!res.ok) throw new Error("Checkout failed");
            return res.json();
        })
        .then(data => {
            const orderId = data?.orderId ?? data?.id;
            showSuccessModal("cartModalContainer", {
                title: "Order Placed!",
                message: `Order #${orderId} has been placed successfully.`,
                primaryLabel: "View Orders",
                primaryHref: "orders.html",
                secondaryLabel: "Back to Cart",
                secondaryHref: "cart.html"
            });
        })
        .catch(err => {
            console.error(err);
            showToast("Checkout failed. Please try again.", "error");
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