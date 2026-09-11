import { useEffect, useMemo, useState } from "react";
import LoginPage from "./pages/LoginPage.jsx";
import AdminPage from "./pages/AdminPage.jsx";
import HistoryPage from "./pages/HistoryPage.jsx";
import UsersPage from "./pages/UsersPage.jsx";
import DisplayPage from "./pages/DisplayPage.jsx";
import { getCurrentUser, logout } from "./api/carteleraApi.js";
import "./App.css";

const DEFAULT_PLANT_CODE = "ezeiza";

function getRouteFromPath(path = window.location.pathname) {
  if (path.startsWith("/display/")) {
    return "display";
  }

  if (path.startsWith("/app/")) {
    return "app";
  }

  if (path.startsWith("/history")) {
    return "history";
  }

  if (path.startsWith("/users")) {
    return "users";
  }

  return "admin";
}

function getPlantCodeFromPath(path = window.location.pathname) {
  const parts = path.split("/").filter(Boolean);

  if ((parts[0] === "display" || parts[0] === "app") && parts[1]) {
    return parts[1];
  }

  return null;
}

function getDefaultPlantCode(user) {
  if (!user?.plants || user.plants.length === 0) {
    return DEFAULT_PLANT_CODE;
  }

  return user.plants[0].code || DEFAULT_PLANT_CODE;
}

function userHasRoleInPlant(user, plantCode) {
    if (!user) {
        return false;
    }

    if (user.platformAdmin) {
        return true;
    }

    return Boolean(user.plants?.some((plant) => plant.code === plantCode));
}

function buildPathForRoute(route, plantCode) {
  const safePlantCode = plantCode || DEFAULT_PLANT_CODE;

  const pathByRoute = {
    admin: "/",
    history: "/history",
    users: "/users",
    display: `/display/${safePlantCode}`,
    app: `/app/${safePlantCode}`,
  };

  return pathByRoute[route] || "/";
}

function App() {
  const [route, setRoute] = useState(() => getRouteFromPath());
  const [selectedPlantCode, setSelectedPlantCode] = useState(
      () => getPlantCodeFromPath() || DEFAULT_PLANT_CODE
  );
  const [currentUser, setCurrentUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);

  const canUseAdminArea = useMemo(() => {
    if (!currentUser) {
      return false;
    }

    return Boolean(currentUser.platformAdmin || currentUser.plants?.length > 0);
  }, [currentUser]);

  const canViewUsers = useMemo(() => {
    if (!currentUser) {
      return false;
    }

    if (currentUser.platformAdmin) {
      return true;
    }

    return currentUser.plants?.some((plant) => plant.role === "ADMIN") || false;
  }, [currentUser]);

  const canViewHistory = canViewUsers;

  useEffect(() => {
    async function checkSession() {
      const initialRoute = getRouteFromPath();
      const pathPlantCode = getPlantCodeFromPath();

      if (pathPlantCode) {
        setSelectedPlantCode(pathPlantCode);
      }

      if (initialRoute === "display") {
        try {
          const user = await getCurrentUser();
          setCurrentUser(user);
        } catch {
          setCurrentUser(null);
        } finally {
          setAuthLoading(false);
        }

        return;
      }

      try {
        const user = await getCurrentUser();

        setCurrentUser(user);
        setSelectedPlantCode(pathPlantCode || getDefaultPlantCode(user));
      } catch {
        setCurrentUser(null);
        setSelectedPlantCode(pathPlantCode || DEFAULT_PLANT_CODE);
      } finally {
        setAuthLoading(false);
      }
    }

    checkSession();
  }, []);

  useEffect(() => {
    function handlePopState() {
      const nextRoute = getRouteFromPath();
      const pathPlantCode = getPlantCodeFromPath();

      setRoute(nextRoute);

      if (pathPlantCode) {
        setSelectedPlantCode(pathPlantCode);
      }
    }

    window.addEventListener("popstate", handlePopState);

    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, []);

  function navigateTo(nextRoute, plantCode = selectedPlantCode) {
    if (!currentUser && nextRoute !== "display") {
      return;
    }

    if (nextRoute === "history" && !canViewHistory) {
      return;
    }

    if (nextRoute === "users" && !canViewUsers) {
      return;
    }

    const safePlantCode =
        plantCode || getDefaultPlantCode(currentUser) || DEFAULT_PLANT_CODE;

    const nextPath = buildPathForRoute(nextRoute, safePlantCode);

    setRoute(nextRoute);
    setSelectedPlantCode(safePlantCode);
    window.history.pushState({}, "", nextPath);
  }

  function handlePlantChange(plantCode) {
    setSelectedPlantCode(plantCode);

    if (route === "display" || route === "app") {
      const nextPath = buildPathForRoute(route, plantCode);
      window.history.pushState({}, "", nextPath);
    }
  }

  function handleOpenPanel(plantCode) {
    const safePlantCode =
        plantCode || selectedPlantCode || getDefaultPlantCode(currentUser);

    setSelectedPlantCode(safePlantCode);
    setRoute("admin");
    window.history.pushState({}, "", "/");
  }

  async function handleLoginSuccess(user) {
    const pathRoute = getRouteFromPath();
    const pathPlantCode = getPlantCodeFromPath();

    let fullUser = user;

    try {
      fullUser = await getCurrentUser();
    } catch (err) {
      console.warn("Login succeeded but failed to refresh user profile:", err);
    }

    const defaultPlantCode = pathPlantCode || getDefaultPlantCode(fullUser);

    setCurrentUser(fullUser);
    setSelectedPlantCode(defaultPlantCode);

    if (pathRoute === "app") {
      setRoute("app");
      window.history.pushState({}, "", `/app/${defaultPlantCode}`);
      return;
    }

    if (!fullUser.platformAdmin && (!fullUser.plants || fullUser.plants.length === 0)) {
      setRoute("display");
      window.history.pushState({}, "", `/display/${defaultPlantCode}`);
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
      setSelectedPlantCode(DEFAULT_PLANT_CODE);
      window.history.pushState({}, "", "/");
    }
  }

  if (route === "display") {
    return (
        <DisplayPage
            plantCode={selectedPlantCode || getPlantCodeFromPath() || DEFAULT_PLANT_CODE}
            currentUser={currentUser}
            onLogout={handleLogout}
            interactive={false}
            canOpenPanel={false}
            onOpenPanel={handleOpenPanel}
        />
    );
  }

  if (authLoading) {
    return (
        <div className="app-shell">
          <div className="card">
            <h2>Cargando sesión...</h2>
            <p>Verificando si ya hay un usuario autenticado.</p>
          </div>
        </div>
    );
  }

  if (!currentUser) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  if (route === "app") {
    const appPlantCode =
        selectedPlantCode || getPlantCodeFromPath() || getDefaultPlantCode(currentUser);

    return (
        <DisplayPage
            plantCode={appPlantCode}
            currentUser={currentUser}
            onLogout={handleLogout}
            interactive
            canOpenPanel={userHasRoleInPlant(currentUser, appPlantCode)}
            onOpenPanel={handleOpenPanel}
            onPlantChange={handlePlantChange}
        />
    );
  }

  if (!canUseAdminArea) {
    return (
        <div className="app-shell">
          <div className="card">
            <h2>Sin permisos administrativos</h2>
            <p>
              Tu usuario está autenticado, pero todavía no tiene permisos asignados
              en ninguna planta.
            </p>

            <div className="action-row">
              <button
                  type="button"
                  onClick={() => navigateTo("display", selectedPlantCode || DEFAULT_PLANT_CODE)}
              >
                Abrir cartelera pública
              </button>

              <button type="button" onClick={handleLogout}>
                Salir
              </button>
            </div>
          </div>
        </div>
    );
  }

  if (route === "history") {
    if (!canViewHistory) {
      return (
          <div className="app-shell">
            <div className="card">
              <h2>Acceso no permitido</h2>
              <p>No tenés permisos para ver el historial.</p>

              <div className="action-row">
                <button type="button" onClick={() => navigateTo("admin")}>
                  Volver al panel
                </button>
              </div>
            </div>
          </div>
      );
    }

    return (
        <HistoryPage
            activeRoute="history"
            onNavigate={navigateTo}
            currentUser={currentUser}
            onLogout={handleLogout}
            selectedPlantCode={selectedPlantCode || getDefaultPlantCode(currentUser)}
            onPlantChange={handlePlantChange}
        />
    );
  }

  if (route === "users") {
    if (!canViewUsers) {
      return (
          <div className="app-shell">
            <div className="card">
              <h2>Acceso no permitido</h2>
              <p>No tenés permisos para administrar usuarios.</p>

              <div className="action-row">
                <button type="button" onClick={() => navigateTo("admin")}>
                  Volver al panel
                </button>
              </div>
            </div>
          </div>
      );
    }

    return (
        <UsersPage
            activeRoute="users"
            onNavigate={navigateTo}
            currentUser={currentUser}
            onLogout={handleLogout}
            selectedPlantCode={selectedPlantCode || getDefaultPlantCode(currentUser)}
            onPlantChange={handlePlantChange}
        />
    );
  }

  return (
      <AdminPage
          activeRoute="admin"
          onNavigate={navigateTo}
          currentUser={currentUser}
          onLogout={handleLogout}
          selectedPlantCode={selectedPlantCode || getDefaultPlantCode(currentUser)}
          onPlantChange={handlePlantChange}
      />
  );
}

export default App;