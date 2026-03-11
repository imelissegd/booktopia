// ============================================================
//  book-modal.js — shared across catalog, cart, orders
// ============================================================

function viewBookModal(bookId, {
    modalContainerId = "modalContainer",
    closeFn = "closeBookModal",
    loggedInUser = null,
    onSuccessCart = null,
    onSuccessBuy = null,
    hidePurchase = false
} = {}) {
    const modalContainer = document.getElementById(modalContainerId);

    modalContainer.innerHTML = `
    <div class="modal-overlay" onclick="${closeFn}()">
      <div class="modal">
        <div class="modal-loading">Loading book details…</div>
      </div>
    </div>`;

    modalContainer.querySelector(".modal").addEventListener("click", e => e.stopPropagation());

    fetch(`http://localhost:8080/api/books/${bookId}`)
        .then(res => {
            if (!res.ok) throw new Error("Book not found");
            return res.json();
        })
        .then(book => {
            const categoryBadges = (book.categories || [])
                .map(cat => `<span class="category-badge">${formatCategory(cat)}</span>`)
                .join("");

            const canPurchase = !hidePurchase && loggedInUser && loggedInUser.role !== "ROLE_ADMIN";

            window._modalCallbacks = { onSuccessCart, onSuccessBuy, closeFn, modalContainerId };

            modalContainer.innerHTML = `
        <div class="modal-overlay" onclick="${closeFn}()">
          <div class="modal modal--book">
            <button class="modal-close" onclick="${closeFn}()" title="Close">✕</button>
            <div class="modal-book-layout">
              <div class="modal-book-cover">
                <img src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}">
              </div>
              <div class="modal-book-info">
                <h2 class="modal-book-title">${book.title}</h2>
                <p class="modal-book-author">by ${book.author || 'Unknown Author'}</p>
                <p class="modal-book-price">₱${book.price}</p>
                ${categoryBadges ? `<div class="category-badges">${categoryBadges}</div>` : ""}
                <p class="modal-book-desc">${book.description || 'No description available.'}</p>
              </div>
            </div>
            ${canPurchase ? `
              <div class="modal-book-actions" id="modalBookActions">
                <div class="modal-action-row">
                  <button class="modal-btn-teal" onclick="expandModalCart(${book.id}, ${book.price})">Add to Cart</button>
                  <button class="modal-btn-amber" onclick="expandModalBuy(${book.id}, ${book.price})">Buy Now</button>
                </div>
              </div>
            ` : ''}
          </div>
        </div>`;

            modalContainer.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
        })
        .catch(() => {
            modalContainer.innerHTML = `
        <div class="modal-overlay" onclick="${closeFn}()">
          <div class="modal">
            <button class="modal-close" onclick="${closeFn}()">✕</button>
            <p>Could not load book details.</p>
          </div>
        </div>`;

            modalContainer.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
        });
}

function expandModalCart(bookId, price) {
    document.getElementById("modalBookActions").innerHTML = `
    <div class="modal-expand">
      <div class="modal-expand-row">
        <label>Quantity</label>
        <input type="number" id="modalQty" value="1" min="1" oninput="updateModalTotal(${price})">
      </div>
      <div class="modal-expand-row">
        <label>Total</label>
        <strong id="modalTotal">₱${price}</strong>
      </div>
      <div class="modal-action-row">
        <button class="modal-btn-teal" onclick="confirmAddToCart(${bookId})">Confirm Add to Cart</button>
        <button class="modal-btn-ghost" onclick="collapseModalActions(${bookId}, ${price})">Cancel</button>
      </div>
    </div>`;
}

function expandModalBuy(bookId, price) {
    document.getElementById("modalBookActions").innerHTML = `
    <div class="modal-expand">
      <div class="modal-expand-row">
        <label>Quantity</label>
        <input type="number" id="modalQty" value="1" min="1" oninput="updateModalTotal(${price})">
      </div>
      <div class="modal-expand-row">
        <label>Total</label>
        <strong id="modalTotal">₱${price}</strong>
      </div>
      <div class="modal-action-row">
        <button class="modal-btn-amber" onclick="confirmBuyNow(${bookId})">Confirm Buy Now</button>
        <button class="modal-btn-ghost" onclick="collapseModalActions(${bookId}, ${price})">Cancel</button>
      </div>
    </div>`;
}

function collapseModalActions(bookId, price) {
    document.getElementById("modalBookActions").innerHTML = `
    <div class="modal-action-row">
      <button class="modal-btn-teal" onclick="expandModalCart(${bookId}, ${price})">Add to Cart</button>
      <button class="modal-btn-amber" onclick="expandModalBuy(${bookId}, ${price})">Buy Now</button>
    </div>`;
}

function updateModalTotal(unitPrice) {
    const qty = parseInt(document.getElementById("modalQty").value) || 1;
    document.getElementById("modalTotal").textContent = `₱${(unitPrice * qty).toFixed(2)}`;
}

function showSuccessModal(modalContainerId, { title, message, primaryLabel, primaryHref, secondaryLabel, secondaryHref }) {
    const modalContainer = document.getElementById(modalContainerId);
    modalContainer.innerHTML = `
    <div class="modal-overlay">
      <div class="modal modal--success">
        <div class="success-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="40" height="40">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="9 12 11 14 15 10"/>
          </svg>
        </div>
        <h2>${title}</h2>
        <p>${message}</p>
        <div class="modal-action-row">
          <button class="modal-btn-teal" onclick="window.location.href='${primaryHref}'">${primaryLabel}</button>
          <button class="modal-btn-ghost" onclick="window.location.href='${secondaryHref}'">${secondaryLabel}</button>
        </div>
      </div>
    </div>`;

    modalContainer.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
}

function confirmAddToCart(bookId) {
    const qty = parseInt(document.getElementById("modalQty").value) || 1;
    const loggedInUser = (() => {
        try { return JSON.parse(localStorage.getItem("currentUser")); }
        catch { return null; }
    })();

    const { closeFn, onSuccessCart, modalContainerId } = window._modalCallbacks || {};

    fetch(`http://localhost:8080/api/cart/${loggedInUser.username}/add`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bookId, quantity: qty })
    })
        .then(res => res.json())
        .then(() => {
            if (typeof onSuccessCart === "function") {
                onSuccessCart();
            } else {
                // Default: show success modal with navigation options
                showSuccessModal(modalContainerId, {
                    title: "Added to Cart!",
                    message: "The book has been added to your cart successfully.",
                    primaryLabel: "Go to Cart",
                    primaryHref: "cart.html",
                    secondaryLabel: "Continue Shopping",
                    secondaryHref: "catalog.html"
                });
            }
        })
        .catch(err => { console.error(err); alert("Error adding to cart"); });
}

function confirmBuyNow(bookId) {
    const qty = parseInt(document.getElementById("modalQty").value) || 1;
    const loggedInUser = (() => {
        try { return JSON.parse(localStorage.getItem("currentUser")); }
        catch { return null; }
    })();

    const { closeFn, onSuccessBuy, modalContainerId } = window._modalCallbacks || {};

    fetch(`http://localhost:8080/api/orders/${loggedInUser.username}/buy-now`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bookId, quantity: qty })
    })
        .then(res => res.json())
        .then(order => {
            const orderId = order?.orderId ?? order?.id;
            if (typeof onSuccessBuy === "function") {
                onSuccessBuy();
            } else {
                // Default: show success modal with navigation options
                showSuccessModal(modalContainerId, {
                    title: "Order Placed!",
                    message: `Order #${orderId} has been placed successfully.`,
                    primaryLabel: "View Orders",
                    primaryHref: "orders.html",
                    secondaryLabel: "Continue Shopping",
                    secondaryHref: "catalog.html"
                });
            }
        })
        .catch(err => { console.error(err); alert("Error during checkout"); });
}

function formatCategory(cat) {
    return cat.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}