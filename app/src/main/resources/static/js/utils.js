// --- Admin: Populate user dropdown ---
async function populateUserDropdown() {
    try {
        const res = await fetch("http://localhost:8080/api/admin/users", {
            credentials: "include"
        });
        if (!res.ok) throw new Error("Failed to fetch users");
        const users = await res.json();
        const dropdown = document.getElementById("userDropdown");
        dropdown.innerHTML = "";

        // Add default option
        const defaultOpt = document.createElement("option");
        defaultOpt.value = "";
        defaultOpt.textContent = "-- Select User --";
        dropdown.appendChild(defaultOpt);

        users.forEach(user => {
            const opt = document.createElement("option");
            opt.value = user.username;
            opt.textContent = user.username;
            dropdown.appendChild(opt);
        });

        document.getElementById("viewCartBtn").onclick = () => {
            const selectedUser = dropdown.value;
            window.location.href = `cart.html?username=${selectedUser}`;
        };

        document.getElementById("viewOrdersBtn").onclick = () => {
            const selectedUser = dropdown.value;
            window.location.href = `orders.html?username=${selectedUser}`;
        };
    } catch (err) {
        console.error(err);
    }
}