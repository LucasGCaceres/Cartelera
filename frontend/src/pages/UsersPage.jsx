import { useEffect, useMemo, useState } from "react";
import Topbar from "../components/Topbar.jsx";
import {
    assignPlantUserRole,
    createGlobalUser,
    getPlantUsers,
    removePlantUserRole,
    updateGlobalUser,
    updateGlobalUserStatus,
} from "../api/carteleraApi.js";

function UsersPage({
                       activeRoute,
                       onNavigate,
                       currentUser,
                       onLogout,
                       selectedPlantCode,
                       onPlantChange,
                   }) {
    const selectedPlant = currentUser?.plants?.find(
        (plant) => plant.code === selectedPlantCode
    );

    const plantCode = selectedPlantCode || currentUser?.plants?.[0]?.code || "ezeiza";

    const plantDisplayName =
        selectedPlant?.displayName || selectedPlant?.name || plantCode;

    const [plantUsers, setPlantUsers] = useState([]);
    const [formData, setFormData] = useState({
        username: "",
        corporateEmail: "",
        fullName: "",
        password: "",
        platformAdmin: false,
    });

    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState("");

    const isPlatformAdmin = Boolean(currentUser?.platformAdmin);

    const currentPlantRole = useMemo(() => {
        if (isPlatformAdmin) {
            return "ADMIN";
        }

        return (
            currentUser?.plants?.find((plant) => plant.code === plantCode)?.role ||
            "Sin permisos"
        );
    }, [currentUser, isPlatformAdmin, plantCode]);

    const canManagePlantUsers = isPlatformAdmin || currentPlantRole === "ADMIN";

    const sortedPlantUsers = useMemo(() => {
        return [...plantUsers].sort((a, b) =>
            (a.fullName || a.username || "").localeCompare(
                b.fullName || b.username || "",
                "es",
                { sensitivity: "base" }
            )
        );
    }, [plantUsers]);

    async function loadUsers() {
        try {
            setError("");

            if (!canManagePlantUsers) {
                setPlantUsers([]);
                return;
            }

            const plantData = await getPlantUsers(plantCode);
            setPlantUsers(plantData || []);
        } catch (err) {
            setError(err.message || "No se pudieron cargar los usuarios de la planta.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        setLoading(true);
        loadUsers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [plantCode, currentPlantRole, isPlatformAdmin]);

    function handleInputChange(event) {
        const { name, value, type, checked } = event.target;

        setFormData((current) => ({
            ...current,
            [name]: type === "checkbox" ? checked : value,
        }));
    }

    async function handleCreateUser(event) {
        event.preventDefault();

        if (!isPlatformAdmin) {
            setError("Solo un platform admin puede crear usuarios globales.");
            return;
        }

        if (
            !formData.username.trim() ||
            !formData.corporateEmail.trim() ||
            !formData.fullName.trim() ||
            !formData.password.trim()
        ) {
            setError(
                "Username, email corporativo, nombre completo y contraseña temporal son obligatorios."
            );
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await createGlobalUser({
                username: formData.username.trim(),
                corporateEmail: formData.corporateEmail.trim(),
                fullName: formData.fullName.trim(),
                password: formData.password,
                platformAdmin: formData.platformAdmin,
            });

            setFormData({
                username: "",
                corporateEmail: "",
                fullName: "",
                password: "",
                platformAdmin: false,
            });

            await loadUsers();
        } catch (err) {
            setError(err.message || "No se pudo crear el usuario. Verificá que no exista.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleRoleChange(user, nextRole) {
        if (!canManagePlantUsers) {
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            if (!nextRole) {
                await removePlantUserRole(plantCode, user.userId);
            } else {
                await assignPlantUserRole(plantCode, user.userId, nextRole);
            }

            await loadUsers();
        } catch (err) {
            setError(err.message || "No se pudo actualizar el rol en la planta.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handlePlatformAdminChange(user, platformAdmin) {
        if (!isPlatformAdmin) {
            return;
        }

        if (user.username === "admin" && !platformAdmin) {
            setError("No se puede quitar platformAdmin al administrador principal.");
            return;
        }

        if (user.username === currentUser.username && !platformAdmin) {
            setError("No podés quitarte platformAdmin a tu propio usuario.");
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await updateGlobalUser(user.userId, { platformAdmin });
            await loadUsers();
        } catch (err) {
            setError(err.message || "No se pudo cambiar el permiso platformAdmin.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleToggleGlobalStatus(user) {
        if (!isPlatformAdmin) {
            return;
        }

        if (user.username === currentUser.username) {
            setError("No podés desactivar tu propio usuario.");
            return;
        }

        if (user.username === "admin") {
            setError("No se puede desactivar el administrador principal.");
            return;
        }

        const confirmed = window.confirm(
            user.active
                ? `¿Seguro que querés desactivar globalmente a ${user.username}?`
                : `¿Seguro que querés activar globalmente a ${user.username}?`
        );

        if (!confirmed) {
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await updateGlobalUserStatus(user.userId, !user.active);
            await loadUsers();
        } catch (err) {
            setError(err.message || "No se pudo cambiar el estado global del usuario.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    return (
        <div className="page">
            <Topbar
                subtitle={`Usuarios y permisos — ${plantDisplayName}`}
                activeRoute={activeRoute}
                onNavigate={onNavigate}
                currentUser={currentUser}
                onLogout={onLogout}
                selectedPlantCode={plantCode}
                onPlantChange={onPlantChange}
            />

            {error && <div className="error-box">{error}</div>}

            {!canManagePlantUsers && (
                <div className="pending-box warning-pending">
                    <strong>No tenés permisos para administrar usuarios en esta planta.</strong>
                    <span>Solo un platform admin o un ADMIN de planta puede administrar permisos.</span>
                </div>
            )}

            {isPlatformAdmin && (
                <section className="card">
                    <span className="eyebrow">Usuarios globales</span>
                    <h2>Crear usuario</h2>

                    <form onSubmit={handleCreateUser} className="person-form compact-user-form">
                        <label>
                            Username
                            <input
                                name="username"
                                value={formData.username}
                                onChange={handleInputChange}
                                placeholder="ej: jperez o jperez@empresa.com"
                                disabled={actionLoading}
                            />
                        </label>

                        <label>
                            Email corporativo
                            <input
                                name="corporateEmail"
                                value={formData.corporateEmail}
                                onChange={handleInputChange}
                                placeholder="usuario@empresa.com"
                                disabled={actionLoading}
                            />
                        </label>

                        <label>
                            Nombre completo
                            <input
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleInputChange}
                                placeholder="Nombre Apellido"
                                disabled={actionLoading}
                            />
                        </label>

                        <label>
                            Contraseña temporal
                            <input
                                name="password"
                                type="password"
                                value={formData.password}
                                onChange={handleInputChange}
                                disabled={actionLoading}
                            />
                        </label>

                        <label className="availability-toggle compact-check">
                            <input
                                name="platformAdmin"
                                type="checkbox"
                                checked={formData.platformAdmin}
                                onChange={handleInputChange}
                                disabled={actionLoading}
                            />
                            Platform admin
                        </label>

                        <button type="submit" className="create-user-button" disabled={actionLoading}>
                            {actionLoading ? "Guardando..." : "Crear usuario"}
                        </button>
                    </form>
                </section>
            )}

            <section className="card">
                <div className="section-header">
                    <div>
                        <span className="eyebrow">Permisos por planta</span>
                        <h2>Usuarios de {plantCode}</h2>
                    </div>

                    <button type="button" onClick={loadUsers} disabled={actionLoading}>
                        Actualizar
                    </button>
                </div>

                {loading ? (
                    <p className="empty-message">Cargando usuarios...</p>
                ) : sortedPlantUsers.length === 0 ? (
                    <p className="empty-message">No hay usuarios registrados o no tenés permisos para verlos.</p>
                ) : (
                    <div className="table-wrapper">
                        <table>
                            <thead>
                                <tr>
                                    <th>Usuario</th>
                                    <th>Email</th>
                                    <th>Nombre completo</th>
                                    <th>Estado global</th>
                                    {isPlatformAdmin && <th>Platform admin</th>}
                                    <th>Rol en esta planta</th>
                                    {isPlatformAdmin && <th>Acciones</th>}
                                </tr>
                            </thead>

                            <tbody>
                            {sortedPlantUsers.map((user) => {
                                const roleInPlant = user.roleInPlant || "Sin permisos";
                                const roleValue = roleInPlant === "Sin permisos" ? "" : roleInPlant;
                                const isCurrentUser = user.username === currentUser.username;
                                const isPrimaryAdmin = user.username === "admin";
                                const isGlobalAdmin = Boolean(user.platformAdmin);

                                return (
                                    <tr key={user.userId}>
                                        <td>{user.username}</td>
                                        <td>{user.corporateEmail || "-"}</td>
                                        <td>{user.fullName}</td>
                                        <td>
                                            {user.active ? (
                                                <span className="responsible-badge">Activo</span>
                                            ) : (
                                                <span className="not-responsible-badge">Inactivo</span>
                                            )}
                                        </td>
                                        {isPlatformAdmin && (
                                            <td>
                                                <label className="availability-toggle compact-check">
                                                    <input
                                                        type="checkbox"
                                                        checked={isGlobalAdmin}
                                                        onChange={(event) =>
                                                            handlePlatformAdminChange(user, event.target.checked)
                                                        }
                                                        disabled={
                                                            actionLoading ||
                                                            !isPlatformAdmin ||
                                                            isCurrentUser ||
                                                            isPrimaryAdmin
                                                        }
                                                    />
                                                    {isGlobalAdmin ? "Sí" : "No"}
                                                </label>
                                            </td>
                                        )}
                                        <td>
                                            <select
                                                value={roleValue}
                                                onChange={(event) => handleRoleChange(user, event.target.value)}
                                                disabled={
                                                    actionLoading ||
                                                    !canManagePlantUsers ||
                                                    !user.active ||
                                                    isGlobalAdmin
                                                }
                                            >
                                                <option value="">Sin permisos</option>
                                                <option value="ADMIN">ADMIN</option>
                                                <option value="OPERADOR">OPERADOR</option>
                                            </select>
                                        </td>
                                        {isPlatformAdmin && (
                                            <td>
                                                <div className="actions">
                                                    <button
                                                        type="button"
                                                        className={user.active ? "danger-button" : "ghost-button"}
                                                        onClick={() => handleToggleGlobalStatus(user)}
                                                        disabled={actionLoading || isCurrentUser || isPrimaryAdmin}
                                                    >
                                                        {user.active ? "Desactivar" : "Activar"}
                                                    </button>
                                                </div>
                                            </td>
                                        )}
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    );
}

export default UsersPage;
