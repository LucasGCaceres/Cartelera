import { useEffect, useMemo, useState } from "react";
import Topbar from "../components/Topbar.jsx";
import {
    addMemberToSuccession,
    addPlantMember,
    getPlantAdminState,
    getPlantMembers,
    getPlantUsers,
    moveSuccessionMemberDown,
    moveSuccessionMemberUp,
    publishPlantDisplay,
    removeMemberFromSuccession,
    removePlantMember,
    updatePlantMemberAvailability,
} from "../api/carteleraApi.js";

const EMPTY_ADMIN_STATE = {
    plant: null,
    currentUserRole: null,
    currentResponsible: null,
    successionList: [],
    publishedDisplay: null,
};

function AdminPage({
                       activeRoute,
                       onNavigate,
                       currentUser,
                       onLogout,
                       selectedPlantCode,
                       onPlantChange,
                   }) {
    const [adminState, setAdminState] = useState(EMPTY_ADMIN_STATE);
    const [plantUsers, setPlantUsers] = useState([]);
    const [members, setMembers] = useState([]);

    const [memberForm, setMemberForm] = useState({
        userId: "",
        position: "",
    });

    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState("");

    const plantCode = selectedPlantCode || currentUser?.plants?.[0]?.code || "ezeiza";

    const currentUserRole = adminState.currentUserRole;
    const isPlatformAdmin = Boolean(currentUser?.platformAdmin);
    const isPlantAdmin = isPlatformAdmin || currentUserRole === "ADMIN";
    const isPlantOperator = currentUserRole === "OPERADOR";

    const canAdminPlant = isPlantAdmin;
    const canOperatePlant = isPlantAdmin || isPlantOperator;

    const successionList = adminState.successionList || [];
    const currentResponsible = adminState.currentResponsible;
    const publishedDisplay = adminState.publishedDisplay;

    const successionMemberIds = useMemo(() => {
        return new Set(
            successionList
                .map((item) => item.member?.id)
                .filter((id) => id !== null && id !== undefined)
        );
    }, [successionList]);

    const membersOutsideSuccession = useMemo(() => {
        return members.filter((member) => !successionMemberIds.has(member.id));
    }, [members, successionMemberIds]);

    const availableUsersToAdd = useMemo(() => {
        const activeMemberUserIds = new Set(
            members.filter((member) => member.active).map((member) => member.userId)
        );

        return plantUsers
            .filter((user) => user.active)
            .filter((user) => !activeMemberUserIds.has(user.userId));
    }, [plantUsers, members]);

    const previewUserId = currentResponsible?.userId ?? null;
    const publishedUserId = publishedDisplay?.userId ?? null;

    const hasInitialPublication = Boolean(publishedDisplay);
    const hasPendingChanges = hasInitialPublication && previewUserId !== publishedUserId;
    const canPublish = canOperatePlant && (!hasInitialPublication || hasPendingChanges);

    async function loadAllData() {
        if (!plantCode) {
            return;
        }

        try {
            setError("");

            const stateData = await getPlantAdminState(plantCode);
            const normalizedState = stateData || EMPTY_ADMIN_STATE;
            const roleFromState = normalizedState.currentUserRole;
            const canLoadPlantUsers = isPlatformAdmin || roleFromState === "ADMIN";

            const [membersData, usersData] = await Promise.all([
                getPlantMembers(plantCode),
                canLoadPlantUsers ? getPlantUsers(plantCode) : Promise.resolve([]),
            ]);

            setAdminState(normalizedState);
            setMembers(membersData || []);
            setPlantUsers(usersData || []);
        } catch (err) {
            setError(err.message || "No se pudo cargar el estado del panel.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    async function reloadStateOnly() {
        try {
            const [stateData, membersData] = await Promise.all([
                getPlantAdminState(plantCode),
                getPlantMembers(plantCode),
            ]);

            setAdminState(stateData || EMPTY_ADMIN_STATE);
            setMembers(membersData || []);
        } catch (err) {
            setError(err.message || "No se pudo actualizar el panel.");
            console.error(err);
        }
    }

    useEffect(() => {
        setLoading(true);
        setMemberForm({
            userId: "",
            position: "",
        });

        loadAllData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [plantCode]);

    function handleMemberFormChange(event) {
        const { name, value } = event.target;

        setMemberForm((current) => ({
            ...current,
            [name]: value,
        }));
    }

    async function handleAddMember(event) {
        event.preventDefault();

        if (!memberForm.userId) {
            setError("Seleccioná un usuario para agregar como responsable operativo.");
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await addPlantMember(
                plantCode,
                Number(memberForm.userId),
                memberForm.position.trim()
            );

            setMemberForm({
                userId: "",
                position: "",
            });

            await loadAllData();
        } catch (err) {
            setError(err.message || "No se pudo agregar el miembro de planta.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleAddToSuccession(memberId) {
        try {
            setActionLoading(true);
            setError("");

            await addMemberToSuccession(plantCode, memberId);
            await reloadStateOnly();
        } catch (err) {
            setError(err.message || "No se pudo agregar a la sucesión.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleAvailabilityChange(memberId, available) {
        try {
            setActionLoading(true);
            setError("");

            await updatePlantMemberAvailability(plantCode, memberId, available);
            await reloadStateOnly();
        } catch (err) {
            setError(err.message || "No se pudo actualizar la disponibilidad.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleMoveUp(memberId) {
        try {
            setActionLoading(true);
            setError("");

            await moveSuccessionMemberUp(plantCode, memberId);
            await reloadStateOnly();
        } catch (err) {
            setError(err.message || "No se pudo subir en la lista.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleMoveDown(memberId) {
        try {
            setActionLoading(true);
            setError("");

            await moveSuccessionMemberDown(plantCode, memberId);
            await reloadStateOnly();
        } catch (err) {
            setError(err.message || "No se pudo bajar en la lista.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleRemoveFromSuccession(member) {
        const confirmed = window.confirm(
            `¿Seguro que querés quitar a ${member.fullName} de la sucesión?`
        );

        if (!confirmed) {
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await removeMemberFromSuccession(plantCode, member.id);
            await reloadStateOnly();
        } catch (err) {
            setError(err.message || "No se pudo quitar de la sucesión.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleRemoveMember(member) {
        const confirmed = window.confirm(
            `¿Seguro que querés quitar a ${member.fullName} como miembro operativo de esta planta?`
        );

        if (!confirmed) {
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await removePlantMember(plantCode, member.id);
            await loadAllData();
        } catch (err) {
            setError(err.message || "No se pudo quitar el miembro de planta.");
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

            await publishPlantDisplay(plantCode);
            await reloadStateOnly();
        } catch (err) {
            setError(err.message || "No se pudo sincronizar la cartelera.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    if (loading) {
        return (
            <div className="page">
                <div className="card">Cargando panel...</div>
            </div>
        );
    }

    return (
        <div className="page">
            <Topbar
                subtitle={adminState.plant?.displayName || "Panel operativo"}
                activeRoute={activeRoute}
                onNavigate={onNavigate}
                currentUser={currentUser}
                onLogout={onLogout}
                selectedPlantCode={plantCode}
                onPlantChange={onPlantChange}
            />

            {error && <div className="error-box">{error}</div>}

            {!canOperatePlant && (
                <div className="pending-box warning-pending">
                    <strong>No tenés permisos operativos en esta planta.</strong>
                    <span>Pedile a un administrador que te asigne un rol en esta planta.</span>
                </div>
            )}

            {!hasInitialPublication && (
                <div className="pending-box warning-pending">
                    <strong>Cartelera sin publicación inicial.</strong>
                    <span>Todavía no se sincronizó la cartelera de esta planta.</span>
                </div>
            )}

            {hasPendingChanges && (
                <div className="pending-box">
                    <strong>Hay cambios pendientes sin publicar.</strong>
                    <span>La vista previa actual no coincide con la cartelera publicada.</span>
                </div>
            )}

            <section className="grid">
                <div className="card responsible-card">
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
                            <p>Marcá al menos un miembro de la sucesión como disponible.</p>
                        </>
                    )}
                </div>

                <div className="card">
                    <span className="eyebrow">Cartelera publicada</span>
                    <h2>Estado publicado</h2>

                    {publishedDisplay ? (
                        publishedDisplay.responsibleName ? (
                            <div className="publish-box">
                                <strong>{publishedDisplay.responsibleName}</strong>
                                <span>{publishedDisplay.responsiblePosition || "Sin cargo informado"}</span>
                            </div>
                        ) : (
                            <div className="publish-box">
                                <strong>Sin responsable publicado</strong>
                                <span>La última publicación no tenía responsable disponible.</span>
                            </div>
                        )
                    ) : (
                        <div className="publish-box">
                            <strong>Sin publicación inicial</strong>
                            <span>Presioná sincronizar para publicar por primera vez.</span>
                        </div>
                    )}

                    <button
                        type="button"
                        className="publish-button"
                        onClick={handlePublishDisplay}
                        disabled={actionLoading || !canPublish}
                    >
                        {actionLoading
                            ? "Sincronizando..."
                            : !hasInitialPublication
                                ? "Publicar cartelera"
                                : hasPendingChanges
                                    ? "Sincronizar cartelera"
                                    : "Cartelera actualizada"}
                    </button>
                </div>
            </section>

            {canAdminPlant && (
                <section className="card">
                    <span className="eyebrow">Responsables operativos</span>
                    <h2>Agregar miembro de planta</h2>

                    <form onSubmit={handleAddMember} className="person-form">
                        <label>
                            Usuario
                            <select
                                name="userId"
                                value={memberForm.userId}
                                onChange={handleMemberFormChange}
                                disabled={actionLoading}
                            >
                                <option value="">Seleccionar usuario...</option>
                                {availableUsersToAdd.map((user) => (
                                    <option key={user.userId} value={user.userId}>
                                        {user.fullName} — {user.corporateEmail || user.username}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Cargo en esta planta
                            <input
                                name="position"
                                value={memberForm.position}
                                onChange={handleMemberFormChange}
                                placeholder="Ej: Responsable operativo"
                                disabled={actionLoading}
                            />
                        </label>

                        <button type="submit" disabled={actionLoading || !memberForm.userId}>
                            {actionLoading ? "Guardando..." : "Agregar miembro"}
                        </button>
                    </form>

                    {availableUsersToAdd.length === 0 && (
                        <p className="empty-message">
                            No hay usuarios activos disponibles para agregar. Creá usuarios desde la pantalla Usuarios.
                        </p>
                    )}
                </section>
            )}

            <section className="card">
                <div className="section-header">
                    <div>
                        <span className="eyebrow">Sucesión</span>
                        <h2>Lista de sucesión y disponibilidad</h2>
                    </div>
                </div>

                {successionList.length === 0 ? (
                    <p className="empty-message">Todavía no hay miembros en la sucesión de esta planta.</p>
                ) : (
                    <div className="table-wrapper">
                        <table>
                            <thead>
                            <tr>
                                <th>Orden</th>
                                <th>Nombre</th>
                                <th>Email</th>
                                <th>Cargo</th>
                                <th>Disponible</th>
                                <th>Responsable</th>
                                <th>Acciones</th>
                            </tr>
                            </thead>

                            <tbody>
                            {successionList.map((item) => {
                                const member = item.member;

                                if (!member) {
                                    return null;
                                }

                                return (
                                    <tr key={item.id}>
                                        <td>{item.orderNumber}</td>
                                        <td>{member.fullName}</td>
                                        <td>{member.corporateEmail || "-"}</td>
                                        <td>{member.position || "-"}</td>
                                        <td>
                                            <label className="availability-toggle">
                                                <input
                                                    type="checkbox"
                                                    checked={Boolean(member.available)}
                                                    onChange={(event) =>
                                                        handleAvailabilityChange(member.id, event.target.checked)
                                                    }
                                                    disabled={actionLoading || !canOperatePlant}
                                                />
                                                {member.available ? "Sí" : "No"}
                                            </label>
                                        </td>
                                        <td>
                                            {item.currentResponsible ? (
                                                <span className="responsible-badge">Sí</span>
                                            ) : (
                                                <span className="not-responsible-badge">No</span>
                                            )}
                                        </td>
                                        <td>
                                            <div className="actions">
                                                <button
                                                    type="button"
                                                    onClick={() => handleMoveUp(member.id)}
                                                    disabled={actionLoading || item.orderNumber === 1 || !canOperatePlant}
                                                >
                                                    Subir
                                                </button>

                                                <button
                                                    type="button"
                                                    onClick={() => handleMoveDown(member.id)}
                                                    disabled={
                                                        actionLoading ||
                                                        item.orderNumber === successionList.length ||
                                                        !canOperatePlant
                                                    }
                                                >
                                                    Bajar
                                                </button>

                                                {canAdminPlant && (
                                                    <button
                                                        type="button"
                                                        className="danger-button"
                                                        onClick={() => handleRemoveFromSuccession(member)}
                                                        disabled={actionLoading}
                                                    >
                                                        Quitar
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            {canAdminPlant && (
                <section className="card">
                    <span className="eyebrow">Miembros fuera de sucesión</span>
                    <h2>Agregar a la sucesión</h2>

                    {membersOutsideSuccession.length === 0 ? (
                        <p className="empty-message">No hay miembros activos fuera de la sucesión.</p>
                    ) : (
                        <div className="table-wrapper">
                            <table>
                                <thead>
                                <tr>
                                    <th>Nombre</th>
                                    <th>Email</th>
                                    <th>Cargo</th>
                                    <th>Disponible</th>
                                    <th>Acciones</th>
                                </tr>
                                </thead>

                                <tbody>
                                {membersOutsideSuccession.map((member) => (
                                    <tr key={member.id}>
                                        <td>{member.fullName}</td>
                                        <td>{member.corporateEmail || "-"}</td>
                                        <td>{member.position || "-"}</td>
                                        <td>{member.available ? "Sí" : "No"}</td>
                                        <td>
                                            <div className="actions">
                                                <button
                                                    type="button"
                                                    onClick={() => handleAddToSuccession(member.id)}
                                                    disabled={actionLoading}
                                                >
                                                    Agregar a sucesión
                                                </button>

                                                <button
                                                    type="button"
                                                    className="danger-button"
                                                    onClick={() => handleRemoveMember(member)}
                                                    disabled={actionLoading}
                                                >
                                                    Quitar miembro
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
            )}
        </div>
    );
}

export default AdminPage;