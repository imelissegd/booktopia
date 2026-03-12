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
  const query    = document.getElementById("searchInput").value.trim().toLowerCase();
  const category = document.getElementById("categoryFilter").value;
  const isManageMode = new URLSearchParams(window.location.search).get("mode") === "manage";
  const statusEl = document.getElementById("statusFilter");
  const status   = statusEl ? statusEl.value : "all";

  filteredBooks = books.filter(book => {
    const matchesSearch =
        !query ||
        book.title?.toLowerCase().includes(query) ||
        book.author?.toLowerCase().includes(query);

    const matchesCategory =
        !category ||
        (book.categories && book.categories.includes(category));

    // Browse mode: always hide delisted books
    // Manage mode: filter by the status dropdown
    const matchesStatus = isManageMode
        ? (status === "active"   ? book.active !== false
            : status === "delisted" ? book.active === false
                : true)
        : book.active !== false;

    return matchesSearch && matchesCategory && matchesStatus;
  });

  const countEl = document.getElementById("resultsCount");
  if (query || category || (isManageMode && status !== "all")) {
    countEl.textContent = `${filteredBooks.length} result${filteredBooks.length !== 1 ? "s" : ""} found`;
  } else {
    countEl.textContent = "";
  }

  currentPage = 1;
  renderBooksPage(currentPage);
  setupPagination();
}

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

    div.innerHTML = `
      <img src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}">
      <h3>${book.title}</h3>
      <p>${book.author || ''}</p>
      <p>₱${book.price}</p>
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

    return `
      <tr style="${book.active === false ? 'opacity:0.5' : ''}">
        <td><img class="list-cover" src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}"></td>
        <td>
          <div class="list-title">${book.title}</div>
          <div class="list-author">${book.author || '—'}</div>
        </td>
        <td class="list-price">₱${book.price}</td>
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
    loggedInUser
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

function openCheckout(bookId) {
  if (loggedInUser?.role === "ROLE_ADMIN") return;

  const book = books.find(b => b.id === bookId);
  if (!book) return;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal">
        <h2>Buy Now</h2>
        <p class="modal-book-title">${book.title}</p>
        <p class="modal-book-author">by ${book.author || 'Unknown Author'}</p>
        <p class="modal-book-price">₱${book.price}</p>
        <label>Quantity</label>
        <input type="number" id="qty" value="1" min="1" oninput="updateModalTotal(${book.price})">
        <p>Total: <strong id="modalTotal">₱${book.price}</strong></p>
        <br>
        <button onclick="buyNow(${book.id})">Buy Now</button>
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
      .then(res => res.json())
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
      .catch(err => { console.error(err); alert("Error during checkout"); });
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