// --- Handle registration ---
document.getElementById("registerForm")?.addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("regUsername").value;
  const email = document.getElementById("regEmail").value;
  const password = document.getElementById("regPassword").value;

  try {
    const res = await fetch("http://localhost:8080/api/register", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username, email, password })
    });

    const data = await res.json();

    if (data.success) {
      // Store registered user info locally (as basic USER role)
      localStorage.setItem("currentUser", JSON.stringify({ username, role: "ROLE_USER" }));
      alert("Registration successful!");
      window.location.href = "login.html"; // redirect to login
    } else {
      alert(data.message);
    }
  } catch (err) {
    console.error(err);
    alert("Registration failed. Check console.");
  }
});

// --- Handle login ---
document.getElementById("loginForm")?.addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  try {
    const res = await fetch("http://localhost:8080/api/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username, password }),
      credentials: 'include'   // include session cookie
    });

    const data = await res.json();

    if (data.success) {
      // Store logged-in user info (username + role)
      localStorage.setItem("currentUser", JSON.stringify({ username, role: data.role }));
      alert("Login successful!");
      window.location.href = "catalog.html"; // redirect to catalog
    } else {
      alert(data.message);
    }
  } catch (err) {
    console.error(err);
    alert("Login failed. Check console.");
  }
});