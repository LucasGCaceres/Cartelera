import Topbar from "../components/Topbar.jsx";
import {useState, useEffect} from "react";
import {
    createPerson,
        deletePerson,
        getAdminState,
        movePersonDown,
        movePersonUp,
        publishDisplay,
        updateAvailability,
} from "../api/carteleraApi.js";


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
            setError(err.message || "No se pudo crear la persona.");
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

export default AdminPage;