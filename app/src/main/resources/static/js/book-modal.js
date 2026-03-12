// ============================================================
//  book-modal.js — shared across catalog, cart, orders
// ============================================================

function viewBookModal(bookId, {
    modalContainerId = "modalContainer",
    closeFn = "closeBookModal",
    loggedInUser = null,
    onSuccessCart = null,
    onSuccessBuy = null,
    onBrowse = null,
    hidePurchase = false,
    _autoExpandBuy = false,
    _maxQty = null
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

            window._modalCallbacks = { onSuccessCart, onSuccessBuy, onBrowse, closeFn, modalContainerId };
            window._modalBook = book;

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
                ${book.stock !== null && book.stock !== undefined
                ? book.stock <= 0
                    ? `<p class="modal-book-stock" style="font-size:0.8rem;font-weight:600;color:var(--error)">Out of Stock</p>`
                    : `<p class="modal-book-stock" style="font-size:0.8rem;font-weight:600;color:var(--teal)">Stock: ${book.stock}</p>`
                : ""}
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

            if (_autoExpandBuy && canPurchase) {
                try {
                    expandModalBuy(book.id, book.price);
                    if (_maxQty !== null) {
                        const qtyInput = document.getElementById("modalQty");
                        if (qtyInput) {
                            qtyInput.max = _maxQty;
                            qtyInput.value = Math.min(parseInt(qtyInput.value) || 1, _maxQty);
                            updateModalTotal(book.price);
                        }
                    }
                } catch (e) {
                    console.error("expandModalBuy error:", e);
                }
            }
        })
        .catch(err => {
            console.error("viewBookModal fetch error:", err);
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
    lockQtyInput(document.getElementById("modalQty"));
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
    lockQtyInput(document.getElementById("modalQty"));
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

    const { onSuccessCart, modalContainerId } = window._modalCallbacks || {};

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

    const { onSuccessBuy, modalContainerId } = window._modalCallbacks || {};

    fetch(`http://localhost:8080/api/orders/${loggedInUser.username}/buy-now`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bookId, quantity: qty })
    })
        .then(res => {
            if (res.status === 400) return res.text().then(msg => { throw { stock: true, message: msg }; });
            if (!res.ok) throw { stock: false, message: "Error during checkout" };
            return res.json();
        })
        .then(order => {
            const txnId = order?.transactionId ?? "";
            if (typeof onSuccessBuy === "function") {
                onSuccessBuy();
            } else {
                showSuccessModal(modalContainerId, {
                    title: "Order Placed!",
                    message: `Order ${txnId} has been placed successfully.`,
                    primaryLabel: "View Orders",
                    primaryHref: "orders.html",
                    secondaryLabel: "Continue Shopping",
                    secondaryHref: "catalog.html"
                });
            }
        })
        .catch(err => {
            if (err.stock) {
                showStockErrorModal(modalContainerId, err.message, bookId);
            } else {
                console.error(err);
                alert(err.message || "Error during checkout");
            }
        });
}

function showStockErrorModal(modalContainerId, serverMessage, bookId) {
    const match = serverMessage.match(/Available:\s*(\d+)/i);
    const available = match ? parseInt(match[1]) : null;

    // Stock is completely gone — show out-of-stock modal
    if (available === 0) {
        showOutOfStockModal(modalContainerId);
        return;
    }

    const { onBrowse } = window._modalCallbacks || {};
    const browseAction = typeof onBrowse === "function"
        ? `(${onBrowse.toString()})()`
        : `window.location.href='catalog.html'`;

    // Some stock remains but not enough for the requested qty
    const stockLine = available !== null
        ? `<p style="font-size:1.1rem;font-weight:700;color:var(--teal);margin:0.25rem 0 1rem">${available} remaining in stock</p>`
        : `<p style="font-size:0.88rem;color:var(--muted);margin-bottom:1rem">${serverMessage}</p>`;

    const container = document.getElementById(modalContainerId);
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
          <button class="modal-btn-ghost" id="_browseBtn">Browse Books</button>
          <button class="modal-btn-teal" onclick="adjustQty(${bookId}, ${available ?? 1})">Adjust Quantity</button>
        </div>
      </div>
    </div>`;
    container.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
    container.querySelector("#_browseBtn").addEventListener("click", () => {
        if (typeof onBrowse === "function") onBrowse();
        else window.location.href = "catalog.html";
    });
}

function showOutOfStockModal(modalContainerId) {
    const { onBrowse } = window._modalCallbacks || {};
    const container = document.getElementById(modalContainerId);
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
          <button class="modal-btn-teal" id="_browseBtn">Back to Catalog</button>
        </div>
      </div>
    </div>`;
    container.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
    container.querySelector("#_browseBtn").addEventListener("click", () => {
        if (typeof onBrowse === "function") onBrowse();
        else window.location.href = "catalog.html";
    });
}

function adjustQty(bookId, maxQty) {
    const { modalContainerId, closeFn, onSuccessCart, onSuccessBuy } = window._modalCallbacks || {};
    const loggedInUser = (() => { try { return JSON.parse(localStorage.getItem("currentUser")); } catch { return null; } })();

    // Re-open the book modal, then automatically expand the buy panel
    viewBookModal(bookId, {
        modalContainerId,
        closeFn,
        loggedInUser,
        onSuccessCart,
        onSuccessBuy,
        _autoExpandBuy: true,
        _maxQty: maxQty
    });
}

function formatCategory(cat) {
    return cat.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

function lockQtyInput(el) {
    el.addEventListener("keydown", e => {
        if (["-", ".", "+", "e", "E"].includes(e.key)) e.preventDefault();
    });
    el.addEventListener("paste", e => e.preventDefault());
    el.addEventListener("input", () => {
        el.value = el.value.replace(/[^0-9]/g, "");
        if (el.value === "" || parseInt(el.value) < 1) el.value = 1;
    });
    el.addEventListener("blur", () => {
        if (el.value === "" || parseInt(el.value) < 1) el.value = 1;
    });
}