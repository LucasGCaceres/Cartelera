import { useEffect, useState } from "react";
import LoginPage from "./pages/LoginPage.jsx";
import AdminPage from "./pages/AdminPage.jsx";
import HistoryPage from "./pages/HistoryPage.jsx";
import UsersPage from "./pages/UsersPage.jsx";
import DisplayPage from "./pages/DisplayPage.jsx";
import { getCurrentUser, logout } from "./api/carteleraApi";
import "./App.css";


function App() {
  const [route, setRoute] = useState("admin");
  const [currentUser, setCurrentUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    const currentPath = window.location.pathname;

    if (currentPath.includes("display")) {
      setRoute("display");
    } else if (currentPath.includes("history")) {
      setRoute("history");
    } else if (currentPath.includes("users")) {
      setRoute("users");
    } else {
      setRoute("admin");
    }
  }, []);

  useEffect(() => {
    async function checkSession() {
      try {
        const user = await getCurrentUser();
        setCurrentUser(user);

        if (user.role === "LECTOR" && !window.location.pathname.includes("display")) {
          setRoute("display");
          window.history.replaceState({}, "", "/display");
        }
      } catch (err) {
        setCurrentUser(null);
      } finally {
        setAuthLoading(false);
      }
    }

    checkSession();
  }, []);

  function navigateTo(nextRoute) {
    if (currentUser?.role === "LECTOR" && nextRoute !== "display") {
      return;
    }

    if (
        (nextRoute === "history" || nextRoute === "users") &&
        currentUser?.role !== "ADMIN"
    ) {
      return;
    }

    const pathByRoute = {
      admin: "/",
      history: "/history",
      users: "/users",
      display: "/display",
    };

    setRoute(nextRoute);
    window.history.pushState({}, "", pathByRoute[nextRoute]);
  }

  async function handleLoginSuccess(user) {
    setCurrentUser(user);

    if (user.role === "LECTOR") {
      setRoute("display");
      window.history.pushState({}, "", "/display");
      return;
    }

    setRoute("admin");
    window.history.pushState({}, "", "/");
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (err) {
      console.error(err);
    } finally {
      setCurrentUser(null);
      setRoute("admin");
      window.history.pushState({}, "", "/");
    }
  }

  if (route === "display") {
    return <DisplayPage currentUser={currentUser} onLogout={handleLogout} />;
  }

  if (authLoading) {
    return (
        <main className="page">
          <section className="card">
            <p>Cargando sesión...</p>
          </section>
        </main>
    );
  }

  if (!currentUser) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  if (currentUser.role === "LECTOR") {
    return <DisplayPage currentUser={currentUser} onLogout={handleLogout} />;
  }

  if (route === "history") {
    if (currentUser.role !== "ADMIN") {
      return (
          <AdminPage
              activeRoute="admin"
              onNavigate={navigateTo}
              currentUser={currentUser}
              onLogout={handleLogout}
          />
      );
    }

    return (
        <HistoryPage
            activeRoute="history"
            onNavigate={navigateTo}
            currentUser={currentUser}
            onLogout={handleLogout}
        />
    );
  }

  if (route === "users") {
    if (currentUser.role !== "ADMIN") {
      return (
          <AdminPage
              activeRoute="admin"
              onNavigate={navigateTo}
              currentUser={currentUser}
              onLogout={handleLogout}
          />
      );
    }

    return (
        <UsersPage
            activeRoute="users"
            onNavigate={navigateTo}
            currentUser={currentUser}
            onLogout={handleLogout}
        />
    );
  }

  return (
      <AdminPage
          activeRoute="admin"
          onNavigate={navigateTo}
          currentUser={currentUser}
          onLogout={handleLogout}
      />
  );
}

export default App;