import { useEffect, useState } from "react";
import "./App.css";

import {
  createPerson,
  deletePerson,
  getAdminState,
  getPublishedDisplay,
  movePersonDown,
  movePersonUp,
  publishDisplay,
  updateAvailability,
} from "./api/carteleraApi";

function App() {
  const [route, setRoute] = useState("admin");

  useEffect(() => {
    const currentPath = window.location.pathname;

    if (currentPath.includes("display")) {
      setRoute("display");
    } else {
      setRoute("admin");
    }
  }, []);

  if (route === "display") {
    return <DisplayPage />;
  }

  return <AdminPage />;
}

function AdminPage() {
  const [adminState, setAdminState] = useState({
    currentResponsible: null,
    successionList: [],
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

  if (loading) {
    return (
        <main className="page">
          <section className="card">
            <p>Cargando panel...</p>
          </section>
        </main>
    );
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

  const currentResponsible = adminState.currentResponsible;
  const successionList = adminState.successionList || [];

  return (
      <main className="page">
        <header className="topbar">
          <div>
            <h1>Cartelera responsable de planta</h1>
            <p>Planta Ezeiza</p>
          </div>

          <a className="secondary-link" href="/display" target="_blank" rel="noreferrer">
            Abrir cartelera
          </a>
        </header>

        {error && <div className="error-box">{error}</div>}

        <section className="grid">
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

              {adminState.publishedDisplay ? (
                  adminState.publishedDisplay.responsibleName ? (
                      <>
                        <strong>{adminState.publishedDisplay.responsibleName}</strong>
                        <span>
            {adminState.publishedDisplay.responsiblePosition ||
                "Sin cargo informado"}
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
                disabled={actionLoading}
            >
              {actionLoading ? "Sincronizando..." : "Sincronizar cartelera"}
            </button>
          </section>

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
        </section>

        <section className="card">
          <div className="section-header">
            <div>
              <span className="eyebrow">Sucesión</span>
              <h2>Lista de sucesión y disponibilidad</h2>
            </div>

            <button type="button" className="ghost-button" onClick={loadAdminState}>
              Actualizar
            </button>
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
                                disabled={actionLoading}
                            />
                            <span>{item.person.available ? "Sí" : "No"}</span>
                          </label>
                        </td>

                        <td>
                          <div className="actions">
                            <button
                                type="button"
                                onClick={() => handleMoveUp(item.person.id)}
                                disabled={actionLoading || item.orderNumber === 1}
                            >
                              Subir
                            </button>

                            <button
                                type="button"
                                onClick={() => handleMoveDown(item.person.id)}
                                disabled={
                                    actionLoading ||
                                    item.orderNumber === successionList.length
                                }
                            >
                              Bajar
                            </button>

                            <button
                                type="button"
                                className="danger-button"
                                onClick={() => handleDeletePerson(item.person)}
                                disabled={actionLoading}
                            >
                              Eliminar
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
  const [responsible, setResponsible] = useState(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [connectionError, setConnectionError] = useState(false);

  async function loadResponsible() {
    try {
      const data = await getPublishedDisplay();
      setResponsible(data);
    } catch (err) {
      console.error(err);
      setResponsible(null);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadResponsible(true);

    const intervalId = window.setInterval(() => {
      loadResponsible(false);
    }, 2000);

    return () => window.clearInterval(intervalId);
  }, []);

  return (
      <main className="display-page">
        <section className="display-content">
          <h1>{responsible?.plantName || "PLANTA EZEIZA"}</h1>
          <p className="display-title">
            {responsible?.mainTitle || "Responsable de planta"}
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
          ) : responsible && responsible.responsibleName ? (
              <div className="display-card">
                <h2>{responsible.responsibleName}</h2>
                <p>{responsible.responsiblePosition || "Sin cargo informado"}</p>
              </div>
          ) : (
              <div className="display-card">
                <p className="display-message">No hay responsable disponible</p>
              </div>
          )}

          {lastUpdated && (
              <p className="display-last-check">
                Última verificación: {lastUpdated.toLocaleTimeString()}
              </p>
          )}
        </section>
      </main>
  );
}

export default App;