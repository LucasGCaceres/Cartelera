import { useEffect, useState } from "react";
import "./App.css";

import {
  createPerson,
  createUser,
  deletePerson,
  getAdminState,
  getAuditLogs,
  getCurrentUser,
  getPublishedDisplay,
  getUsers,
  login,
  logout,
  movePersonDown,
  movePersonUp,
  publishDisplay,
  resetUserPassword,
  updateAvailability,
  updateUserRole,
  updateUserStatus,
  registerUser,
} from "./api/carteleraApi";

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
      if (window.location.pathname.includes("display")) {
        setAuthLoading(false);
        return;
      }

      try {
        const user = await getCurrentUser();
        setCurrentUser(user);
      } catch (err) {
        setCurrentUser(null);
      } finally {
        setAuthLoading(false);
      }
    }

    checkSession();
  }, []);

  function navigateTo(nextRoute) {
    if ((nextRoute === "history" || nextRoute === "users") && currentUser?.role !== "ADMIN") {
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
    return <DisplayPage />;
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

function LoginPage({ onLoginSuccess }) {
  const [mode, setMode] = useState("login");

  const [formData, setFormData] = useState({
    username: "",
    password: "",
    fullName: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  function handleChange(event) {
    const { name, value } = event.target;

    setFormData((current) => ({
      ...current,
      [name]: value,
    }));
  }

  function switchMode(nextMode) {
    setMode(nextMode);
    setError("");
    setSuccessMessage("");

    setFormData({
      username: "",
      password: "",
      fullName: "",
    });
  }

  async function handleLoginSubmit(event) {
    event.preventDefault();

    if (!formData.username.trim() || !formData.password.trim()) {
      setError("Usuario y contraseña son obligatorios.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setSuccessMessage("");

      const user = await login(formData.username.trim(), formData.password);

      onLoginSuccess(user);
    } catch (err) {
      if (err.status === 423) {
        setError("El usuario está deshabilitado. Contactá a un administrador.");
      } else {
        setError("Usuario o contraseña incorrectos.");
      }

      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleRegisterSubmit(event) {
    event.preventDefault();

    if (
        !formData.username.trim() ||
        !formData.password.trim() ||
        !formData.fullName.trim()
    ) {
      setError("Usuario, nombre completo y contraseña son obligatorios.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setSuccessMessage("");

      await registerUser({
        username: formData.username.trim(),
        password: formData.password,
        fullName: formData.fullName.trim(),
      });

      setSuccessMessage(
          "Cuenta creada como LECTOR. Ahora podés iniciar sesión."
      );

      setMode("login");

      setFormData({
        username: formData.username.trim(),
        password: "",
        fullName: "",
      });
    } catch (err) {
      if (err.status === 400) {
        setError("No se pudo crear la cuenta. Revisá los datos ingresados.");
      } else if (err.status === 409) {
        setError("Ya existe un usuario con ese nombre.");
      } else {
        setError("No se pudo crear la cuenta. Verificá que el usuario no exista.");
      }

      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const isRegisterMode = mode === "register";

  return (
      <main className="login-page">
        <section className="login-card">
          <span className="eyebrow">Cartelera responsable de planta</span>

          <h1>{isRegisterMode ? "Crear cuenta" : "Iniciar sesión"}</h1>

          <p>
            {isRegisterMode
                ? "La cuenta se creará inicialmente como LECTOR."
                : "Ingresá con tu usuario para administrar la cartelera."}
          </p>

          {error && <div className="error-box">{error}</div>}

          {successMessage && (
              <div className="pending-box">
                <strong>{successMessage}</strong>
              </div>
          )}

          <form
              className="person-form"
              onSubmit={isRegisterMode ? handleRegisterSubmit : handleLoginSubmit}
          >
            {isRegisterMode && (
                <label>
                  Nombre completo
                  <input
                      type="text"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleChange}
                      placeholder="Ej: Usuario Recepción"
                      required
                  />
                </label>
            )}

            <label>
              Usuario
              <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  placeholder="Ej: recepcion1"
                  autoComplete="username"
                  required
              />
            </label>

            <label>
              Contraseña
              <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Contraseña"
                  autoComplete={isRegisterMode ? "new-password" : "current-password"}
                  required
              />
            </label>

            <button type="submit" disabled={loading}>
              {loading
                  ? isRegisterMode
                      ? "Creando..."
                      : "Ingresando..."
                  : isRegisterMode
                      ? "Crear cuenta"
                      : "Ingresar"}
            </button>
          </form>

          <div className="login-help">
            {isRegisterMode ? (
                <>
                  <strong>¿Ya tenés cuenta?</strong>

                  <button
                      type="button"
                      className="link-button"
                      onClick={() => switchMode("login")}
                  >
                    Volver al inicio de sesión
                  </button>
                </>
            ) : (
                <>
                  <strong>¿No tenés cuenta?</strong>

                  <button
                      type="button"
                      className="link-button"
                      onClick={() => switchMode("register")}
                  >
                    Crear cuenta nueva
                  </button>
                </>
            )}
          </div>
        </section>
      </main>
  );
}

function Topbar({ subtitle, activeRoute, onNavigate, currentUser, onLogout }) {
  const isAdmin = currentUser?.role === "ADMIN";

  function openDisplay() {
    window.open("/display", "_blank", "noopener,noreferrer");
  }

  return (
      <header className="topbar">
        <div>
          <h1>Cartelera responsable de planta</h1>
          <p>{subtitle}</p>
        </div>

        <nav className="topbar-actions" aria-label="Navegación principal">
          <button
              type="button"
              className={activeRoute === "admin" ? "nav-button active" : "nav-button"}
              onClick={() => onNavigate("admin")}
          >
            Panel
          </button>

          {isAdmin && (
              <button
                  type="button"
                  className={activeRoute === "history" ? "nav-button active" : "nav-button"}
                  onClick={() => onNavigate("history")}
              >
                Historial
              </button>
          )}

          {isAdmin && (
              <button
                  type="button"
                  className={activeRoute === "users" ? "nav-button active" : "nav-button"}
                  onClick={() => onNavigate("users")}
              >
                Usuarios
              </button>
          )}

          <button
              type="button"
              className="nav-button display-nav-button"
              onClick={openDisplay}
          >
            Abrir cartelera
          </button>

          <div className="user-pill">
            <span>{currentUser?.fullName}</span>
            <strong>{currentUser?.role}</strong>
          </div>

          <button type="button" className="nav-button logout-button" onClick={onLogout}>
            Salir
          </button>
        </nav>
      </header>
  );
}

function AdminPage({ activeRoute, onNavigate, currentUser, onLogout }) {
  const [adminState, setAdminState] = useState({
    currentResponsible: null,
    successionList: [],
    publishedDisplay: null,
  });

  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    position: "",
  });

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  async function loadAdminState() {
    try {
      setError("");
      const data = await getAdminState();
      setAdminState(data);
    } catch (err) {
      setError("No se pudo cargar el estado del panel.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAdminState();
  }, []);

  function handleInputChange(event) {
    const { name, value } = event.target;

    setFormData((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleCreatePerson(event) {
    event.preventDefault();

    if (!formData.firstName.trim() || !formData.lastName.trim()) {
      setError("Nombre y apellido son obligatorios.");
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const data = await createPerson({
        firstName: formData.firstName.trim(),
        lastName: formData.lastName.trim(),
        position: formData.position.trim(),
      });

      setAdminState(data);

      setFormData({
        firstName: "",
        lastName: "",
        position: "",
      });
    } catch (err) {
      setError("No se pudo crear la persona.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleAvailabilityChange(personId, available) {
    try {
      setActionLoading(true);
      setError("");

      const data = await updateAvailability(personId, available);
      setAdminState(data);
    } catch (err) {
      setError("No se pudo actualizar la disponibilidad.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleMoveUp(personId) {
    try {
      setActionLoading(true);
      setError("");

      const data = await movePersonUp(personId);
      setAdminState(data);
    } catch (err) {
      setError("No se pudo subir la persona en la lista.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleMoveDown(personId) {
    try {
      setActionLoading(true);
      setError("");

      const data = await movePersonDown(personId);
      setAdminState(data);
    } catch (err) {
      setError("No se pudo bajar la persona en la lista.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleDeletePerson(person) {
    const confirmed = window.confirm(
        `¿Seguro que querés eliminar a ${person.fullName} de la lista?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const data = await deletePerson(person.id);
      setAdminState(data);
    } catch (err) {
      setError("No se pudo eliminar la persona.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handlePublishDisplay() {
    const confirmed = window.confirm(
        "¿Confirmás que querés sincronizar la cartelera con el responsable resultante actual?"
    );

    if (!confirmed) {
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const data = await publishDisplay();
      setAdminState(data);
    } catch (err) {
      setError("No se pudo sincronizar la cartelera.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return (
        <main className="page">
          <section className="card">
            <p>Cargando panel...</p>
          </section>
        </main>
    );
  }

  const currentResponsible = adminState.currentResponsible;
  const successionList = adminState.successionList || [];
  const publishedDisplay = adminState.publishedDisplay;

  const isAdmin = currentUser?.role === "ADMIN";
  const isOperator = currentUser?.role === "OPERADOR";

  const canCreatePersons = isAdmin;
  const canDeletePersons = isAdmin;
  const canOperateDisplay = isAdmin || isOperator;

  const previewPersonId = currentResponsible?.id ?? null;
  const publishedPersonId = publishedDisplay?.personId ?? null;

  const hasInitialPublication = Boolean(publishedDisplay);
  const hasPendingChanges =
      hasInitialPublication && previewPersonId !== publishedPersonId;

  const canPublish = !hasInitialPublication || hasPendingChanges;

  return (
      <main className="page">
        <Topbar
            subtitle="Planta Ezeiza"
            activeRoute={activeRoute}
            onNavigate={onNavigate}
            currentUser={currentUser}
            onLogout={onLogout}
        />

        {error && <div className="error-box">{error}</div>}

        {!hasInitialPublication && (
            <div className="pending-box">
              <strong>Cartelera sin publicación inicial.</strong>
              <span>
            Todavía no se sincronizó la cartelera. Presioná “Sincronizar
            cartelera” para publicar el responsable res ultante.
          </span>
            </div>
        )}

        {hasPendingChanges && (
            <div className="pending-box warning-pending">
              <strong>Hay cambios pendientes sin publicar.</strong>
              <span>
            La vista previa actual no coincide con la cartelera publicada. La TV
            no se actualizará hasta presionar “Sincronizar cartelera”.
          </span>
            </div>
        )}

        <section className={canCreatePersons ? "grid" : "grid single-card-grid"}>
          <section className="card responsible-card">
            <span className="eyebrow">Vista previa</span>
            <h2>Responsable resultante</h2>

            {currentResponsible ? (
                <>
                  <p className="preview-name">{currentResponsible.fullName}</p>
                  <p>{currentResponsible.position || "Sin cargo informado"}</p>
                </>
            ) : (
                <>
                  <p className="preview-name">No hay responsable disponible</p>
                  <p>Marcá al menos una persona como disponible.</p>
                </>
            )}

            <div className="publish-box">
              <span className="eyebrow">Cartelera publicada</span>

              {publishedDisplay ? (
                  publishedDisplay.responsibleName ? (
                      <>
                        <strong>{publishedDisplay.responsibleName}</strong>
                        <span>
              {publishedDisplay.responsiblePosition || "Sin cargo informado"}
            </span>
                      </>
                  ) : (
                      <>
                        <strong>No hay responsable publicado</strong>
                        <span>La última publicación no tenía responsable disponible.</span>
                      </>
                  )
              ) : (
                  <>
                    <strong>Sin publicación inicial</strong>
                    <span>Presioná sincronizar para publicar por primera vez.</span>
                  </>
              )}
            </div>

            <button
                type="button"
                className="publish-button"
                onClick={handlePublishDisplay}
                disabled={actionLoading || !canPublish || !canOperateDisplay}
            >
              {actionLoading
                  ? "Sincronizando..."
                  : !hasInitialPublication
                      ? "Publicar cartelera"
                      : hasPendingChanges
                          ? "Sincronizar cartelera"
                          : "Cartelera actualizada"}
            </button>
          </section>

          {canCreatePersons && (
              <section className="card">
                <span className="eyebrow">Nueva persona</span>
                <h2>Cargar persona</h2>

                <form className="person-form" onSubmit={handleCreatePerson}>
                  <label>
                    Nombre
                    <input
                        type="text"
                        name="firstName"
                        value={formData.firstName}
                        onChange={handleInputChange}
                        placeholder="Ej: Ana"
                        required
                    />
                  </label>

                  <label>
                    Apellido
                    <input
                        type="text"
                        name="lastName"
                        value={formData.lastName}
                        onChange={handleInputChange}
                        placeholder="Ej: Torres"
                        required
                    />
                  </label>

                  <label>
                    Cargo
                    <input
                        type="text"
                        name="position"
                        value={formData.position}
                        onChange={handleInputChange}
                        placeholder="Ej: Coordinadora"
                    />
                  </label>

                  <button type="submit" disabled={actionLoading}>
                    {actionLoading ? "Guardando..." : "Guardar persona"}
                  </button>
                </form>
              </section>
          )}
        </section>

        <section className="card">
          <div className="section-header">
            <div>
              <span className="eyebrow">Sucesión</span>
              <h2>Lista de sucesión y disponibilidad</h2>
            </div>
          </div>

          {successionList.length === 0 ? (
              <p className="empty-message">Todavía no hay personas cargadas.</p>
          ) : (
              <div className="table-wrapper">
                <table>
                  <thead>
                  <tr>
                    <th>Orden</th>
                    <th>Persona</th>
                    <th>Cargo</th>
                    <th>Disponible</th>
                    <th>Responsable</th>
                    <th>Acciones</th>
                  </tr>
                  </thead>

                  <tbody>
                  {successionList.map((item) => (
                      <tr key={item.id}>
                        <td>{item.orderNumber}</td>

                        <td>
                          <strong>{item.person.fullName}</strong>
                        </td>

                        <td>{item.person.position || "-"}</td>

                        <td>
                          <label className="availability-toggle">
                            <input
                                type="checkbox"
                                checked={item.person.available}
                                onChange={(event) =>
                                    handleAvailabilityChange(
                                        item.person.id,
                                        event.target.checked
                                    )
                                }
                                disabled={actionLoading || !canOperateDisplay}
                            />
                            <span>{item.person.available ? "Sí" : "No"}</span>
                          </label>
                        </td>

                        <td>
                          {currentResponsible?.id === item.person.id ? (
                              <span className="responsible-badge">Sí</span>
                          ) : (
                              <span className="not-responsible-badge">No</span>
                          )}
                        </td>

                        <td>
                          <div className="actions">
                            <button
                                type="button"
                                onClick={() => handleMoveUp(item.person.id)}
                                disabled={actionLoading || item.orderNumber === 1 || !canOperateDisplay}                            >
                              Subir
                            </button>

                            <button
                                type="button"
                                onClick={() => handleMoveDown(item.person.id)}
                                disabled={
                                    actionLoading ||
                                    item.orderNumber === successionList.length ||
                                    !canOperateDisplay
                                }
                            >
                              Bajar
                            </button>

                            {canDeletePersons && (
                                <button
                                    type="button"
                                    className="danger-button"
                                    onClick={() => handleDeletePerson(item.person)}
                                    disabled={actionLoading}
                                >
                                  Eliminar
                                </button>
                            )}
                          </div>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          )}
        </section>
      </main>
  );
}

function HistoryPage({ activeRoute, onNavigate, currentUser, onLogout }) {
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadAuditLogs() {
    try {
      setError("");
      const data = await getAuditLogs();
      setAuditLogs(data);
    } catch (err) {
      setError("No se pudo cargar el historial.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAuditLogs();
  }, []);

  return (
      <main className="page">
        <Topbar
            subtitle="Historial de movimientos"
            activeRoute={activeRoute}
            onNavigate={onNavigate}
            currentUser={currentUser}
            onLogout={onLogout}
        />

        {error && <div className="error-box">{error}</div>}

        <section className="card">
          <div className="section-header">
            <div>
              <span className="eyebrow">Auditoría</span>
              <h2>Historial de movimientos</h2>
            </div>

            <button type="button" className="ghost-button" onClick={loadAuditLogs}>
              Actualizar
            </button>
          </div>

          {loading ? (
              <p className="empty-message">Cargando historial...</p>
          ) : auditLogs.length === 0 ? (
              <p className="empty-message">Todavía no hay movimientos registrados.</p>
          ) : (
              <div className="table-wrapper">
                <table>
                  <thead>
                  <tr>
                    <th>Fecha y hora</th>
                    <th>Usuario</th>
                    <th>Acción</th>
                    <th>Entidad</th>
                    <th>Detalle</th>
                  </tr>
                  </thead>

                  <tbody>
                  {auditLogs.map((log) => (
                      <tr key={log.id}>
                        <td>{formatDateTime(log.createdAt)}</td>
                        <td>{log.username}</td>
                        <td>
                          <span className="action-badge">{translateAction(log.action)}</span>
                        </td>
                        <td>{log.entityName}</td>
                        <td>{log.description}</td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          )}
        </section>
      </main>
  );
}

function UsersPage({ activeRoute, onNavigate, currentUser, onLogout }) {
  const [users, setUsers] = useState([]);
  const [formData, setFormData] = useState({
    username: "",
    fullName: "",
    password: "",
    role: "OPERADOR",
  });

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  async function loadUsers() {
    try {
      setError("");
      const data = await getUsers();
      setUsers(data);
    } catch (err) {
      setError("No se pudieron cargar los usuarios.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  function handleInputChange(event) {
    const { name, value } = event.target;

    setFormData((current) => ({
      ...current,
      [name]: value,
    }));
  }

  async function handleCreateUser(event) {
    event.preventDefault();

    if (
        !formData.username.trim() ||
        !formData.fullName.trim() ||
        !formData.password.trim()
    ) {
      setError("Usuario, nombre completo y contraseña son obligatorios.");
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const data = await createUser({
        username: formData.username.trim(),
        fullName: formData.fullName.trim(),
        password: formData.password,
        role: formData.role,
      });

      setUsers(data);

      setFormData({
        username: "",
        fullName: "",
        password: "",
        role: "OPERADOR",
      });
    } catch (err) {
      setError("No se pudo crear el usuario. Verificá que no exista ya.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleChangeRole(userId, role) {
    try {
      setActionLoading(true);
      setError("");

      const data = await updateUserRole(userId, role);
      setUsers(data);
    } catch (err) {
      setError("No se pudo cambiar el rol del usuario.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleToggleStatus(user) {
    if (user.username === currentUser.username) {
      setError("No podés desactivar tu propio usuario.");
      return;
    }

    const confirmed = window.confirm(
        user.active
            ? `¿Seguro que querés desactivar a ${user.username}?`
            : `¿Seguro que querés activar a ${user.username}?`
    );

    if (!confirmed) {
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const data = await updateUserStatus(user.id, !user.active);
      setUsers(data);
    } catch (err) {
      setError("No se pudo cambiar el estado del usuario.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  async function handleResetPassword(user) {
    const newPassword = window.prompt(
        `Ingresá la nueva contraseña para ${user.username}`
    );

    if (!newPassword || !newPassword.trim()) {
      return;
    }

    try {
      setActionLoading(true);
      setError("");

      const data = await resetUserPassword(user.id, newPassword.trim());
      setUsers(data);
    } catch (err) {
      setError("No se pudo restablecer la contraseña.");
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  return (
      <main className="page">
        <Topbar
            subtitle="Administración de usuarios"
            activeRoute={activeRoute}
            onNavigate={onNavigate}
            currentUser={currentUser}
            onLogout={onLogout}
        />

        {error && <div className="error-box">{error}</div>}

        <section className="grid">
          <section className="card">
            <span className="eyebrow">Nuevo usuario</span>
            <h2>Crear usuario</h2>

            <form className="person-form" onSubmit={handleCreateUser}>
              <label>
                Usuario
                <input
                    type="text"
                    name="username"
                    value={formData.username}
                    onChange={handleInputChange}
                    placeholder="Ej: recepcion1"
                    required
                />
              </label>

              <label>
                Nombre completo
                <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleInputChange}
                    placeholder="Ej: Usuario Recepción"
                    required
                />
              </label>

              <label>
                Contraseña inicial
                <input
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleInputChange}
                    placeholder="Contraseña"
                    required
                />
              </label>

              <label>
                Rol
                <select
                    name="role"
                    value={formData.role}
                    onChange={handleInputChange}
                >
                  <option value="OPERADOR">Operador</option>
                  <option value="ADMIN">Administrador</option>
                  <option value="LECTOR">Lector</option>
                </select>
              </label>

              <button type="submit" disabled={actionLoading}>
                {actionLoading ? "Guardando..." : "Crear usuario"}
              </button>
            </form>
          </section>

          <section className="card">
            <span className="eyebrow">Roles</span>
            <h2>Permisos definidos</h2>

            <div className="role-help">
              <p>
                <strong>ADMIN:</strong> acceso completo, historial, usuarios,
                carga y eliminación de personas.
              </p>
              <p>
                <strong>OPERADOR:</strong> disponibilidad, sucesión y publicación
                de cartelera.
              </p>
              <p>
                <strong>LECTOR:</strong> reservado para solo lectura en una etapa
                posterior.
              </p>
            </div>
          </section>
        </section>

        <section className="card">
          <div className="section-header">
            <div>
              <span className="eyebrow">Usuarios</span>
              <h2>Usuarios registrados</h2>
            </div>

            <button type="button" className="ghost-button" onClick={loadUsers}>
              Actualizar
            </button>
          </div>

          {loading ? (
              <p className="empty-message">Cargando usuarios...</p>
          ) : users.length === 0 ? (
              <p className="empty-message">No hay usuarios registrados.</p>
          ) : (
              <div className="table-wrapper">
                <table>
                  <thead>
                  <tr>
                    <th>Usuario</th>
                    <th>Nombre completo</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                  </thead>

                  <tbody>
                  {users.map((user) => (
                      <tr key={user.id}>
                        <td>
                          <strong>{user.username}</strong>
                        </td>

                        <td>{user.fullName}</td>

                        <td>
                          <select
                              value={user.role}
                              onChange={(event) =>
                                  handleChangeRole(user.id, event.target.value)
                              }
                              disabled={
                                  actionLoading || user.username === currentUser.username
                              }
                          >
                            <option value="ADMIN">Administrador</option>
                            <option value="OPERADOR">Operador</option>
                            <option value="LECTOR">Lector</option>
                          </select>
                        </td>

                        <td>
                          {user.active ? (
                              <span className="responsible-badge">Activo</span>
                          ) : (
                              <span className="not-responsible-badge">Inactivo</span>
                          )}
                        </td>

                        <td>
                          <div className="actions">
                            <button
                                type="button"
                                onClick={() => handleResetPassword(user)}
                                disabled={actionLoading}
                            >
                              Reset clave
                            </button>

                            <button
                                type="button"
                                className={user.active ? "danger-button" : ""}
                                onClick={() => handleToggleStatus(user)}
                                disabled={
                                    actionLoading || user.username === currentUser.username
                                }
                            >
                              {user.active ? "Desactivar" : "Activar"}
                            </button>
                          </div>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          )}
        </section>
      </main>
  );
}

function DisplayPage() {
  const [publishedDisplay, setPublishedDisplay] = useState(null);
  const [loading, setLoading] = useState(true);
  const [connectionError, setConnectionError] = useState(false);

  async function loadPublishedDisplay() {
    try {
      const data = await getPublishedDisplay();
      setPublishedDisplay(data);
      setConnectionError(false);
    } catch (err) {
      console.error(err);
      setConnectionError(true);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPublishedDisplay();

    const intervalId = window.setInterval(() => {
      loadPublishedDisplay();
    }, 2000);

    return () => window.clearInterval(intervalId);
  }, []);

  return (
      <main className="display-page">
        <section className="display-content">
          <h1>{publishedDisplay?.plantName || "PLANTA EZEIZA"}</h1>

          <p className="display-title">
            {publishedDisplay?.mainTitle || "Responsable de planta"}
          </p>

          {connectionError && (
              <p className="display-connection-warning">
                Reconectando con el servidor...
              </p>
          )}

          {loading ? (
              <div className="display-card">
                <p className="display-message">Cargando...</p>
              </div>
          ) : publishedDisplay && publishedDisplay.responsibleName ? (
              <div className="display-card">
                <h2>{publishedDisplay.responsibleName}</h2>
                <p>{publishedDisplay.responsiblePosition || "Sin cargo informado"}</p>
              </div>
          ) : (
              <div className="display-card">
                <p className="display-message">No hay responsable disponible</p>
              </div>
          )}
        </section>
      </main>
  );
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

function translateAction(action) {
  const translations = {
    CREATE_PERSON: "Alta de persona",
    CHANGE_AVAILABILITY: "Cambio disponibilidad",
    MOVE_UP: "Subió en sucesión",
    MOVE_DOWN: "Bajó en sucesión",
    REMOVE_PERSON: "Eliminación",
    PUBLISH_DISPLAY: "Publicación cartelera",
    CREATE_USER: "Alta de usuario",
    UPDATE_USER_ROLE: "Cambio de rol",
    ENABLE_USER: "Activación de usuario",
    DISABLE_USER: "Desactivación de usuario",
    RESET_PASSWORD: "Reseteo de contraseña",
    REGISTER_USER: "Registro de usuario",
  };

  return translations[action] || action;
}

export default App;