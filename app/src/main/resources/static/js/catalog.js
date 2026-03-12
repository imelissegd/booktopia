console.log("catalog.js loaded");

const booksPerPage = 10;
let currentPage = 1;
let currentView = "grid"; // "grid" | "list"
let books = [];
let filteredBooks = [];
let booksMap = {};
let loggedInUser = null;

// --- On page load ---
document.addEventListener("DOMContentLoaded", async () => {
  loggedInUser = (() => {
    try { return JSON.parse(localStorage.getItem("currentUser")); }
    catch { return null; }
  })();

  const isAdmin = loggedInUser?.role === "ROLE_ADMIN";
  const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";

  if (isAdmin && isManageMode) {
    document.querySelector(".catalog-main h1").textContent = "Manage Books";
    document.getElementById("addBookBtn").style.display = "inline-flex";
    document.getElementById("statusFilterWrap").style.display = "";
    setView("list", false);
  }

  await fetchBooks();
  applyFilters();
});

// --- View toggle ---
function setView(view, rerender = true) {
  currentView = view;
  document.getElementById("gridViewBtn").classList.toggle("active", view === "grid");
  document.getElementById("listViewBtn").classList.toggle("active", view === "list");
  if (rerender) renderBooksPage(currentPage);
}

// --- Fetch all books ---
async function fetchBooks() {
  try {
    const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";
    const url = isManageMode
        ? "http://localhost:8080/api/books/admin/all"
        : "http://localhost:8080/api/books";
    const res = await fetch(url, { credentials: "include" });
    if (!res.ok) throw new Error("Failed to fetch books");
    books = await res.json();
    filteredBooks = [...books];
  } catch (err) {
    console.error(err);
    alert("Could not load books. See console.");
  }
}

// --- Apply search + category filters ---
function applyFilters() {
  const query = document.getElementById("searchInput").value.trim().toLowerCase();
  const allCbs = [...document.querySelectorAll(".cat-cb")];
  const selectedCategories = allCbs.filter(cb => cb.checked).map(cb => cb.value);
  const allSelected = selectedCategories.length === allCbs.length;
  const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";
  const statusEl = document.getElementById("statusFilter");
  const status   = statusEl ? statusEl.value : "all";

  filteredBooks = books.filter(book => {
    const matchesSearch =
        !query ||
        book.title?.toLowerCase().includes(query) ||
        book.author?.toLowerCase().includes(query);

    const matchesCategory =
        allSelected || selectedCategories.length === 0 ||
        (book.categories && book.categories.some(c => selectedCategories.includes(c)));

    const matchesStatus = isManageMode
        ? (status === "active"   ? book.active !== false
            : status === "delisted" ? book.active === false
                : true)
        : book.active !== false;

    return matchesSearch && matchesCategory && matchesStatus;
  });

  const countEl = document.getElementById("resultsCount");
  const _total = books.length;
  const _shown = filteredBooks.length;
  const _filtered = query || (!allSelected && selectedCategories.length > 0) || (isManageMode && status !== "all");
  if (_filtered) {
    countEl.textContent = `Showing ${_shown} of ${_total} book${_total !== 1 ? "s" : ""}`;
  } else {
    countEl.textContent = `${_total} book${_total !== 1 ? "s" : ""} total`;
  }

  currentPage = 1;
  renderBooksPage(currentPage);
  setupPagination();
}

// --- Category checkbox dropdown ---
function toggleCategoryDropdown(e) {
  e.stopPropagation();
  const dd = document.getElementById("categoryDropdown");
  dd.style.display = dd.style.display === "none" ? "block" : "none";
}

function handleCatAll(cb) {
  // Ticking All → check all individuals. Unticking All → uncheck all individuals.
  document.querySelectorAll(".cat-cb").forEach(c => c.checked = cb.checked);
  updateCategoryBtnLabel();
  applyFilters();
}

function handleCatCheck() {
  const allBoxes = [...document.querySelectorAll(".cat-cb")];
  const checked  = allBoxes.filter(c => c.checked);
  // All checkbox mirrors whether every individual is checked
  document.getElementById("catAll").checked = checked.length === allBoxes.length;
  updateCategoryBtnLabel();
  applyFilters();
}

function updateCategoryBtnLabel() {
  const allBoxes = [...document.querySelectorAll(".cat-cb")];
  const checked  = allBoxes.filter(c => c.checked);
  const label    = document.getElementById("categoryBtnLabel");
  const hint     = document.getElementById("categoryHint");

  if (checked.length === 0 || checked.length === allBoxes.length) {
    label.textContent = "All Categories";
  } else if (checked.length === 1) {
    label.textContent = checked[0].closest("label").textContent.trim();
  } else {
    label.textContent = `${checked.length} Categories`;
  }
  if (hint) hint.style.display = checked.length >= 2 && checked.length < allBoxes.length ? "block" : "none";
}

document.addEventListener("click", (e) => {
  const dd  = document.getElementById("categoryDropdown");
  const btn = document.getElementById("categoryDropdownBtn");
  if (dd && !dd.contains(e.target) && e.target !== btn && !btn?.contains(e.target)) {
    dd.style.display = "none";
  }
});

// --- Render books for current page ---
function renderBooksPage(page) {
  const bookList = document.getElementById("bookList");
  bookList.innerHTML = "";

  if (!filteredBooks.length) {
    bookList.className = "";
    bookList.innerHTML = "<p style='text-align:center;color:var(--muted);padding:2rem'>No books found.</p>";
    return;
  }

  const start = (page - 1) * booksPerPage;
  const end = start + booksPerPage;
  const pageBooks = filteredBooks.slice(start, end);

  pageBooks.forEach(book => { booksMap[book.id] = book; });

  if (currentView === "list") {
    renderListView(bookList, pageBooks);
  } else {
    renderGridView(bookList, pageBooks);
  }
}

// --- Grid view ---
function renderGridView(bookList, pageBooks) {
  bookList.className = "";
  const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";
  const isAdmin = loggedInUser?.role === "ROLE_ADMIN";

  pageBooks.forEach(book => {
    const div = document.createElement("div");
    div.className = "book";
    if (book.active === false) div.style.opacity = "0.5";

    const categoryBadges = (book.categories || [])
        .map(cat => `<span class="category-badge">${formatCategory(cat)}</span>`)
        .join("");

    let actions = "";
    if (isManageMode) {
      const delistRelist = book.active !== false
          ? `<button onclick="delistBook(${book.id})" style="background:var(--error);color:#fff">Delist</button>`
          : `<button onclick="relistBook(${book.id})" style="background:var(--teal);color:#fff">Relist</button>`;
      actions = `
        <button class="btn-view-book" onclick="openViewBook(${book.id})">View Book</button>
        <div class="book-action-row">
          <button onclick="editBook(${book.id})" style="background:var(--teal);color:#fff">Edit</button>
          ${delistRelist}
        </div>`;
    } else if (isAdmin) {
      actions = `<button class="btn-view-book" onclick="openViewBook(${book.id})">View Book</button>`;
    } else {
      actions = `
        <button class="btn-view-book" onclick="openViewBook(${book.id})">View Book</button>
        <div class="book-action-row">
          <button onclick="${loggedInUser ? `openAddToCart(${book.id})` : 'openLoginPrompt()'}">Add to Cart</button>
          <button onclick="${loggedInUser ? `openCheckout(${book.id})` : 'openLoginPrompt()'}">Buy Now</button>
        </div>`;
    }

    const stockBadge = book.stock !== null && book.stock !== undefined
        ? book.stock <= 0
            ? `<span style="font-size:0.68rem;font-weight:600;padding:2px 8px;border-radius:20px;background:rgba(192,57,43,0.1);color:var(--error)">Out of Stock</span>`
            : `<span style="font-size:0.68rem;font-weight:600;padding:2px 8px;border-radius:20px;background:rgba(0,137,123,0.1);color:var(--teal)">Stock: ${book.stock}</span>`
        : "";

    div.innerHTML = `
      <img src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}">
      <h3>${book.title}</h3>
      <p>${book.author || ''}</p>
      <p>₱${book.price}</p>
      ${stockBadge}
      ${categoryBadges ? `<div class="category-badges">${categoryBadges}</div>` : ""}
      <div class="book-actions">${actions}</div>
    `;

    bookList.appendChild(div);
  });
}

// --- List view ---
function renderListView(bookList, pageBooks) {
  bookList.className = "list-view";
  const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";
  const isAdmin = loggedInUser?.role === "ROLE_ADMIN";

  const rows = pageBooks.map(book => {
    const badges = (book.categories || [])
        .map(cat => `<span class="list-badge">${formatCategory(cat)}</span>`)
        .join("");

    let actions = "";
    if (isManageMode) {
      const delistRelist = book.active !== false
          ? `<button class="tbl-btn tbl-btn--danger" onclick="delistBook(${book.id})">Delist</button>`
          : `<button class="tbl-btn tbl-btn--ghost" style="border-color:var(--teal);color:var(--teal)" onclick="relistBook(${book.id})">Relist</button>`;
      actions = `
        <button class="tbl-btn tbl-btn--ghost" onclick="openViewBook(${book.id})">View</button>
        <button class="tbl-btn tbl-btn--ghost" onclick="editBook(${book.id})">Edit</button>
        ${delistRelist}`;
    } else if (isAdmin) {
      actions = `<button class="tbl-btn tbl-btn--ghost" onclick="openViewBook(${book.id})">View</button>`;
    } else {
      actions = loggedInUser
          ? `<button class="tbl-btn tbl-btn--ghost" onclick="openViewBook(${book.id})">View</button>
           <button class="tbl-btn tbl-btn--ghost" onclick="openAddToCart(${book.id})">Add to Cart</button>
           <button class="tbl-btn tbl-btn--ghost" style="border-color:var(--amber);color:var(--amber-dark)" onclick="openCheckout(${book.id})">Buy Now</button>`
          : `<button class="tbl-btn tbl-btn--ghost" onclick="openViewBook(${book.id})">View</button>
           <button class="tbl-btn tbl-btn--ghost" onclick="openLoginPrompt()">Add to Cart</button>
           <button class="tbl-btn tbl-btn--ghost" onclick="openLoginPrompt()">Buy Now</button>`;
    }

    const statusCell = isManageMode
        ? `<td>${book.active !== false
            ? `<span class="list-badge" style="border-color:var(--teal);color:var(--teal)">Active</span>`
            : `<span class="list-badge" style="border-color:var(--error);color:var(--error)">Delisted</span>`
        }</td>`
        : "";

    const stockCell = book.stock !== null && book.stock !== undefined
        ? book.stock <= 0
            ? `<span class="list-badge" style="border-color:var(--error);color:var(--error)">Out of Stock</span>`
            : `<span class="list-badge" style="border-color:var(--teal);color:var(--teal)">Stock: ${book.stock}</span>`
        : `<span class="list-badge">—</span>`;

    return `
      <tr style="${book.active === false ? 'opacity:0.5' : ''}">
        <td><img class="list-cover" src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}"></td>
        <td>
          <div class="list-title">${book.title}</div>
          <div class="list-author">${book.author || '—'}</div>
        </td>
        <td class="list-price">₱${book.price}</td>
        <td>${stockCell}</td>
        <td><div class="list-badges">${badges || '—'}</div></td>
        ${statusCell}
        <td><div class="list-actions">${actions}</div></td>
      </tr>`;
  }).join("");

  bookList.innerHTML = `
    <table class="list-table">
      <thead>
        <tr>
          <th></th>
          <th>Title / Author</th>
          <th>Price</th>
          <th>Stock</th>
          <th>Categories</th>
          ${isManageMode ? '<th>Status</th>' : ''}
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>`;
}

// --- Format category enum to readable label ---
function formatCategory(cat) {
  return cat
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, c => c.toUpperCase());
}

// --- Pagination ---
function setupPagination() {
  const pagination = document.getElementById("pagination");
  pagination.innerHTML = "";

  if (!filteredBooks.length) return;

  const totalPages = Math.ceil(filteredBooks.length / booksPerPage);

  for (let i = 1; i <= totalPages; i++) {
    const btn = document.createElement("button");
    btn.textContent = i;
    btn.className = i === currentPage ? "active-page" : "";
    btn.onclick = () => {
      currentPage = i;
      renderBooksPage(currentPage);
      setupPagination();
    };
    pagination.appendChild(btn);
  }
}

// --- Modals ---

function openViewBook(bookId) {
  viewBookModal(bookId, {
    modalContainerId: "modalContainer",
    closeFn: "closeModal",
    loggedInUser,
    onBrowse: closeModal
  });
}

function closeModal() {
  document.getElementById("modalContainer").innerHTML = "";
}

function updateModalTotal(unitPrice) {
  const qty = parseInt(document.getElementById("qty").value) || 1;
  document.getElementById("modalTotal").textContent = `₱${(unitPrice * qty).toFixed(2)}`;
}

function openAddToCart(bookId) {
  if (loggedInUser?.role === "ROLE_ADMIN") return;

  const book = books.find(b => b.id === bookId);
  if (!book) return;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal">
        <h2>Add to Cart</h2>
        <p class="modal-book-title">${book.title}</p>
        <p class="modal-book-author">by ${book.author || 'Unknown Author'}</p>
        <p class="modal-book-price">₱${book.price}</p>
        <label>Quantity</label>
        <input type="number" id="qty" value="1" min="1" oninput="updateModalTotal(${book.price})">
        <p>Total: <strong id="modalTotal">₱${book.price}</strong></p>
        <br>
        <button onclick="addToCart(${book.id})">Add to Cart</button>
        <button onclick="closeModal()">Cancel</button>
      </div>
    </div>`;
  document.querySelector("#modalContainer .modal").addEventListener("click", e => e.stopPropagation());
}

function openCheckout(bookId, maxQty = null) {
  if (loggedInUser?.role === "ROLE_ADMIN") return;

  const book = books.find(b => b.id === bookId);
  if (!book) return;

  const maxAttr = maxQty !== null ? `max="${maxQty}"` : "";
  const initialQty = maxQty !== null ? Math.min(1, maxQty) : 1;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal">
        <h2>Buy Now</h2>
        <p class="modal-book-title">${book.title}</p>
        <p class="modal-book-author">by ${book.author || 'Unknown Author'}</p>
        <p class="modal-book-price">₱${book.price}</p>
        <label>Quantity</label>
        <input type="number" id="qty" value="${initialQty}" min="1" ${maxAttr} oninput="updateModalTotal(${book.price})">
        <p>Total: <strong id="modalTotal">₱${(book.price * initialQty).toFixed(2)}</strong></p>
        <br>
        <button class="modal-btn-amber" onclick="buyNow(${book.id})">Buy Now</button>
        <button onclick="closeModal()">Cancel</button>
      </div>
    </div>`;
  document.querySelector("#modalContainer .modal").addEventListener("click", e => e.stopPropagation());
}

function openLoginPrompt() {
  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal">
        <div class="modal-lock-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" width="32" height="32">
            <rect x="3" y="11" width="18" height="11" rx="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
        </div>
        <h2>Login Required</h2>
        <p>You need to be logged in to add items to your cart or make a purchase.</p>
        <button onclick="window.location.href='login.html'">Go to Login</button>
        <button onclick="closeModal()">Maybe Later</button>
      </div>
    </div>`;
  document.querySelector("#modalContainer .modal").addEventListener("click", e => e.stopPropagation());
}

// --- User actions ---
function addToCart(bookId) {
  const qty = parseInt(document.getElementById("qty").value);
  fetch(`http://localhost:8080/api/cart/${loggedInUser.username}/add`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ bookId, quantity: qty })
  })
      .then(res => res.json())
      .then(() => {
        showSuccessModal("modalContainer", {
          title: "Added to Cart!",
          message: "The book has been added to your cart successfully.",
          primaryLabel: "Go to Cart",
          primaryHref: "cart.html",
          secondaryLabel: "Continue Shopping",
          secondaryHref: "catalog.html"
        });
      })
      .catch(err => { console.error(err); alert("Error adding to cart"); });
}

function buyNow(bookId) {
  const qty = parseInt(document.getElementById("qty").value);
  fetch(`http://localhost:8080/api/orders/${loggedInUser.username}/buy-now`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ bookId, quantity: qty })
  })
      .then(res => {
        if (res.status === 400) {
          return res.text().then(msg => {
            throw { stock: true, message: msg };
          });
        }
        if (!res.ok) {
          throw { stock:false, message:"Checkout failed" };
        }
        return res.json();
      })
      .then(order => {
        const txnId = order?.transactionId ?? "";

        showSuccessModal("modalContainer", {
          title: "Order Placed!",
          message: `Order ${txnId} has been placed successfully.`,
          primaryLabel: "View Orders",
          primaryHref: "orders.html",
          secondaryLabel: "Continue Shopping",
          secondaryHref: "catalog.html"
        });
      })
      .catch(err => {
        if (err.stock) {
          const match = err.message.match(/Available:\s*(\d+)/i);
          const available = match ? parseInt(match[1]) : null;
          if (available === 0) {
            showOutOfStockModal("modalContainer");
          } else {
            showCatalogStockErrorModal("modalContainer", err.message, bookId, available);
          }
          return;
        }
        console.error(err);
        alert(err.message || "Error during checkout");
      });
}

// Exposed for navbar.js admin "Add Book" button
function openCreateBookModal() {
  window.location.href = "add-book.html";
}

// --- Admin: Delist book ---
function delistBook(bookId) {
  fetch(`http://localhost:8080/api/books/${bookId}/delist`, { method: "PATCH", credentials: "include" })
      .then(res => { if (!res.ok) throw new Error(); return res.json(); })
      .then(updated => {
        const idx = books.findIndex(b => b.id === bookId);
        if (idx !== -1) books[idx] = updated;
        booksMap[bookId] = updated;
        applyFilters();
        showCatalogToast(`"${updated.title}" has been delisted.`, "success");
      })
      .catch(() => showCatalogToast("Failed to delist book.", "error"));
}

// --- Admin: Relist book ---
function relistBook(bookId) {
  fetch(`http://localhost:8080/api/books/${bookId}/relist`, { method: "PATCH", credentials: "include" })
      .then(res => { if (!res.ok) throw new Error(); return res.json(); })
      .then(updated => {
        const idx = books.findIndex(b => b.id === bookId);
        if (idx !== -1) books[idx] = updated;
        booksMap[bookId] = updated;
        applyFilters();
        showCatalogToast(`"${updated.title}" has been relisted.`, "success");
      })
      .catch(() => showCatalogToast("Failed to relist book.", "error"));
}

// --- Admin: Edit book ---
function editBook(bookId) {
  window.location.href = `edit-book.html?id=${bookId}`;
}

// --- Admin: Delete book ---
function deleteBook(bookId) {
  const book = booksMap[bookId];
  const title = book?.title ?? `Book #${bookId}`;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" style="text-align:center">
        <div style="display:flex;justify-content:center;margin-bottom:0.75rem;color:var(--error)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="36" height="36" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <h2 style="font-family:'DM Serif Display',serif;margin-bottom:0.5rem">Delete Book?</h2>
        <p style="color:var(--muted);font-size:0.88rem;margin-bottom:1.2rem">
          "<strong>${title}</strong>" will be permanently removed from the catalog.
        </p>
        <div class="modal-action-row">
          <button class="modal-btn-ghost" onclick="closeModal()">Cancel</button>
          <button style="flex:1;padding:8px 12px;background:var(--error);color:#fff;border:none;border-radius:5px;font-weight:600;font-size:0.82rem;cursor:pointer"
            onclick="confirmDeleteBook(${bookId})">Yes, Delete</button>
        </div>
      </div>
    </div>`;
  document.querySelector("#modalContainer .modal").addEventListener("click", e => e.stopPropagation());
}

function confirmDeleteBook(bookId) {
  fetch(`http://localhost:8080/api/books/${bookId}`, { method: "DELETE", credentials: "include" })
      .then(res => {
        if (!res.ok) throw new Error("Delete failed");
        books = books.filter(b => b.id !== bookId);
        filteredBooks = filteredBooks.filter(b => b.id !== bookId);
        delete booksMap[bookId];
        closeModal();
        renderBooksPage(currentPage);
        setupPagination();
        showCatalogToast("Book deleted successfully.", "success");
      })
      .catch(err => {
        console.error(err);
        closeModal();
        alert("Failed to delete book. Please try again.");
      });
}

function showCatalogStockErrorModal(modalContainerId, serverMessage, bookId, available) {
  const stockLine = available !== null
      ? `<p style="font-size:1.1rem;font-weight:700;color:var(--teal);margin:0.25rem 0 1rem">${available} remaining in stock</p>`
      : `<p style="font-size:0.88rem;color:var(--muted);margin-bottom:1rem">${serverMessage}</p>`;

  const container = document.getElementById(modalContainerId);
  container.innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" style="text-align:center;max-width:380px">
        <div style="display:flex;justify-content:center;margin-bottom:0.75rem;color:var(--error)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" width="40" height="40" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
        </div>
        <h2 style="font-family:'DM Serif Display',serif;margin-bottom:0.25rem">Not Enough Stock</h2>
        ${stockLine}
        <div class="modal-action-row">
          <button class="modal-btn-ghost" onclick="closeModal()">Browse Books</button>
          <button class="modal-btn-teal" onclick="openCheckout(${bookId}, ${available ?? 1})">Adjust Quantity</button>
        </div>
      </div>
    </div>`;
  container.querySelector(".modal").addEventListener("click", e => e.stopPropagation());
}

function showCatalogToast(msg, type = "success") {
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