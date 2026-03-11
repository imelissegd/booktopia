console.log("catalog.js loaded");

const booksPerPage = 10;
let currentPage = 1;
let books = [];          // all books from API
let filteredBooks = [];  // books after search + category filter
let booksMap = {};       // books indexed by id for quick lookup
let loggedInUser = null;

// --- On page load ---
document.addEventListener("DOMContentLoaded", async () => {
  loggedInUser = (() => {
    try { return JSON.parse(localStorage.getItem("currentUser")); }
    catch { return null; }
  })();

  await fetchBooks();
  applyFilters();
});

// --- Fetch all books ---
async function fetchBooks() {
  try {
    const res = await fetch("http://localhost:8080/api/books");
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
  const category = document.getElementById("categoryFilter").value;

  filteredBooks = books.filter(book => {
    const matchesSearch =
        !query ||
        book.title?.toLowerCase().includes(query) ||
        book.author?.toLowerCase().includes(query);

    const matchesCategory =
        !category ||
        (book.categories && book.categories.includes(category));

    return matchesSearch && matchesCategory;
  });

  const countEl = document.getElementById("resultsCount");
  if (query || category) {
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
    bookList.innerHTML = "<p>No books found.</p>";
    return;
  }

  const start = (page - 1) * booksPerPage;
  const end = start + booksPerPage;
  const pageBooks = filteredBooks.slice(start, end);

  pageBooks.forEach(book => {
    booksMap[book.id] = book; // cache for viewBookModal

    const div = document.createElement("div");
    div.className = "book";

    const categoryBadges = (book.categories || [])
        .map(cat => `<span class="category-badge">${formatCategory(cat)}</span>`)
        .join("");

    div.innerHTML = `
      <img src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}">
      <h3>${book.title}</h3>
      <p>${book.author || ''}</p>
      <p>₱${book.price}</p>
      ${categoryBadges ? `<div class="category-badges">${categoryBadges}</div>` : ""}
      <div class="book-actions">
        <button class="btn-view-book" onclick="openViewBook(${book.id})">View Book</button>
        ${loggedInUser?.role !== "ROLE_ADMIN" ? `
          <div class="book-action-row">
            <button onclick="${loggedInUser ? `openAddToCart(${book.id})` : 'openLoginPrompt()'}">Add to Cart</button>
            <button onclick="${loggedInUser ? `openCheckout(${book.id})` : 'openLoginPrompt()'}">Buy Now</button>
          </div>
        ` : ''}
      </div>
    `;

    bookList.appendChild(div);
  });
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
  if (typeof window._openCreateBookModal === "function") {
    window._openCreateBookModal();
  }
}