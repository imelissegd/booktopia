// js/create-book.js
console.log("create-book.js loaded");

// Categories enum values
const categories = ["FICTION","NON_FICTION","SCIENCE","TECHNOLOGY","HISTORY","ROMANCE","FANTASY"];

// Attach click listener to the "Add New Book" link in catalog
document.addEventListener("DOMContentLoaded", () => {
    const createBtn = document.getElementById("createBookBtn");
    if (createBtn) createBtn.addEventListener("click", openCreateBookModal);
});

// Function to open modal with the form
function openCreateBookModal() {
    document.getElementById("modalContainer").innerHTML = `
    <div class="modal-overlay" onclick="closeCreateBookModal()">
      <div class="modal" onclick="event.stopPropagation()">
        <h2>Create New Book</h2>

        <label>Title</label>
        <input type="text" id="newBookTitle" required>

        <label>Author</label>
        <input type="text" id="newBookAuthor" required>

        <label>Price</label>
        <input type="number" id="newBookPrice" step="0.01" required>

        <label>Description</label>
        <textarea id="newBookDescription" rows="4" required></textarea>

        <label>Category</label>
        <select id="newBookCategory" required>
          <option value="">Select category</option>
          ${categories.map(c => `<option value="${c}">${c.replace("_", " ")}</option>`).join('')}
        </select>

        <button id="submitNewBook">Create Book</button>
        <button type="button" onclick="closeCreateBookModal()">Cancel</button>

        <div id="createBookMessage"></div>
      </div>
    </div>
    `;

    // Add submit handler
    document.getElementById("submitNewBook").addEventListener("click", submitNewBook);
}

// Close the modal
function closeCreateBookModal() {
    document.getElementById("modalContainer").innerHTML = "";
}

// Handle form submission
async function submitNewBook() {
    const book = {
        title: document.getElementById("newBookTitle").value,
        author: document.getElementById("newBookAuthor").value,
        price: parseFloat(document.getElementById("newBookPrice").value),
        description: document.getElementById("newBookDescription").value,
        categories: [document.getElementById("newBookCategory").value]
    };

    try {
        const res = await fetch("http://localhost:8080/api/books", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(book)
        });

        if (!res.ok) throw new Error("Failed to create book");

        const data = await res.json();

        document.getElementById("createBookMessage").innerHTML = `<p style="color:green;">Book "${data.title}" created successfully!</p>`;

        // Optional: reset form fields
        document.getElementById("newBookTitle").value = "";
        document.getElementById("newBookAuthor").value = "";
        document.getElementById("newBookPrice").value = "";
        document.getElementById("newBookDescription").value = "";
        document.getElementById("newBookCategory").value = "";

        // Refresh catalog if fetchBooks is defined in catalog.js
        if (typeof fetchBooks === "function") {
            fetchBooks().then(() => {
                if (typeof renderBooksPage === "function") renderBooksPage(currentPage);
            });
        }

    } catch (err) {
        console.error(err);
        document.getElementById("createBookMessage").innerHTML = `<p style="color:red;">Error creating book.</p>`;
    }
}