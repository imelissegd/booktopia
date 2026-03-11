console.log("catalog.js loaded");

const booksPerPage = 10;
let currentPage = 1;
let books = [];          // all books from API
let filteredBooks = [];  // books after search + category filter
let loggedInUser = null;

// --- On page load ---
document.addEventListener("DOMContentLoaded", async () => {
  loggedInUser = (() => {
    try { return JSON.parse(localStorage.getItem("currentUser")); }
    catch { return null; }
  })();

  await fetchBooks();
  applyFilters(); // initial render with no filters
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

  // Update results count
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
      ${loggedInUser?.role === "ROLE_ADMIN" ? '' : `
        <div class="book-actions">
          <button class="btn-view-book" onclick="viewBookModal(${book.id})">View Book</button>
          <div class="book-action-row">
            <button onclick="${loggedInUser ? `openAddToCart(${book.id})` : 'openLoginPrompt()'}">Add to Cart</button>
            <button onclick="${loggedInUser ? `openCheckout(${book.id})` : 'openLoginPrompt()'}">Buy Now</button>
          </div>
        </div>
      `}
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

// === Modals ===
function viewBookModal(bookId) {
  const modalContainer = document.getElementById("modalContainer");

  modalContainer.innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" onclick="event.stopPropagation()">
        <div class="modal-loading">Loading book details…</div>
      </div>
    </div>`;

  fetch(`http://localhost:8080/api/books/${bookId}`)
    .then(res => {
      if (!res.ok) throw new Error("Book not found");
      return res.json();
    })
    .then(book => {
      const categoryBadges = (book.categories || [])
        .map(cat => `<span class="category-badge">${formatCategory(cat)}</span>`)
        .join("");

      modalContainer.innerHTML = `
        <div class="modal-overlay" onclick="closeModal()">
          <div class="modal modal--book" onclick="event.stopPropagation()">
            <button class="modal-close" onclick="closeModal()" title="Close">✕</button>
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
          </div>
        </div>`;
    })
    .catch(() => {
      modalContainer.innerHTML = `
        <div class="modal-overlay" onclick="closeModal()">
          <div class="modal" onclick="event.stopPropagation()">
            <button class="modal-close" onclick="closeModal()">✕</button>
            <p>Could not load book details.</p>
          </div>
        </div>`;
    });
}

function closeBookModal() {
  document.getElementById("modalContainer").innerHTML = "";
}



function openAddToCart(bookId) {
  if (loggedInUser?.role === "ROLE_ADMIN") return;

  const book = books.find(b => b.id === bookId);
  if (!book) return;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" onclick="event.stopPropagation()">
        <h2>Add to Cart</h2>
        <img src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}">
        <h3>${book.title}</h3>
        <p>${book.description || ''}</p>
        <label>Quantity</label>
        <input type="number" id="qty" value="1" min="1">
        <br>
        <button onclick="addToCart(${book.id})">Add to Cart</button>
        <button onclick="closeModal()">Cancel</button>
      </div>
    </div>
  `;
}

function openCheckout(bookId) {
  if (loggedInUser?.role === "ROLE_ADMIN") return;

  const book = books.find(b => b.id === bookId);
  if (!book) return;

  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" onclick="event.stopPropagation()">
        <h2>Buy Now</h2>
        <img src="${book.image || './images/book-placeholder.svg'}" alt="${book.title}">
        <h3>${book.title}</h3>
        <p>${book.description || ''}</p>
        <label>Quantity</label>
        <input type="number" id="qty" value="1" min="1">
        <br>
        <button onclick="buyNow(${book.id})">Buy Now</button>
        <button onclick="closeModal()">Cancel</button>
      </div>
    </div>
  `;
}

function openLoginPrompt() {
  document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeModal()">
      <div class="modal" onclick="event.stopPropagation()">
        <h2>Login Required</h2>
        <p>You need to be logged in to add items to your cart or make a purchase.</p>
        <button onclick="window.location.href='login.html'">Go to Login</button>
        <button onclick="closeModal()">Cancel</button>
      </div>
    </div>
  `;
}

function closeModal() {
  document.getElementById("modalContainer").innerHTML = "";
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
      .then(() => { alert("Added to cart!"); closeModal(); })
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
      .then(order => { alert(`Order #${order.orderId} created!`); closeModal(); })
      .catch(err => { console.error(err); alert("Error during checkout"); });
}

// Exposed for navbar.js admin "Add Book" button
function openCreateBookModal() {
  if (typeof window._openCreateBookModal === "function") {
    window._openCreateBookModal();
  }
}