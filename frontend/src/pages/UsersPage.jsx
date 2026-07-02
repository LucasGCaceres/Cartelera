import { useEffect, useMemo, useState } from "react";
import Topbar from "../components/Topbar.jsx";
import {
    assignPlantUserRole,
    createGlobalUser,
    getGlobalUsers,
    getPlantUsers,
    removePlantUserRole,
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
    const plantCode = selectedPlantCode || currentUser?.plants?.[0]?.code || "ezeiza";

    const [globalUsers, setGlobalUsers] = useState([]);
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

    const sortedGlobalUsers = useMemo(() => {
        return [...globalUsers].sort((a, b) =>
            (a.fullName || a.username || "").localeCompare(
                b.fullName || b.username || "",
                "es",
                { sensitivity: "base" }
            )
        );
    }, [globalUsers]);

    async function loadUsers() {
        try {
            setError("");

            if (!canManagePlantUsers) {
                setPlantUsers([]);
                setGlobalUsers([]);
                return;
            }

            const plantData = await getPlantUsers(plantCode);
            setPlantUsers(plantData || []);

            if (isPlatformAdmin) {
                const globalData = await getGlobalUsers();
                setGlobalUsers(globalData || []);
            } else {
                setGlobalUsers([]);
            }
        } catch (err) {
            setError(err.message || "No se pudieron cargar los usuarios.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    async function reloadPlantUsers() {
        try {
            if (!canManagePlantUsers) {
                setPlantUsers([]);
                return;
            }

            const plantData = await getPlantUsers(plantCode);
            setPlantUsers(plantData || []);
        } catch (err) {
            setError(err.message || "No se pudieron actualizar los usuarios de la planta.");
            console.error(err);
        }
    }

    useEffect(() => {
        setLoading(true);
        loadUsers();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [plantCode, isPlatformAdmin, currentPlantRole]);

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

    async function handleAssignRole(userId, role) {
        if (!canManagePlantUsers) {
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await assignPlantUserRole(plantCode, userId, role);
            await reloadPlantUsers();
        } catch (err) {
            setError(err.message || "No se pudo asignar el rol en la planta.");
            console.error(err);
        } finally {
            setActionLoading(false);
        }
    }

    async function handleRemoveRole(user) {
        if (!canManagePlantUsers) {
            return;
        }

        const roleInPlant = user.roleInPlant || "Sin permisos";

        if (roleInPlant === "Sin permisos") {
            return;
        }

        const confirmed = window.confirm(
            `¿Seguro que querés quitar los permisos de ${
                user.fullName || user.username
            } en esta planta?`
        );

        if (!confirmed) {
            return;
        }

        try {
            setActionLoading(true);
            setError("");

            await removePlantUserRole(plantCode, user.userId);
            await reloadPlantUsers();
        } catch (err) {
            setError(err.message || "No se pudo quitar el rol en la planta.");
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
                subtitle={`Usuarios y permisos — ${plantCode}`}
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

                    <form onSubmit={handleCreateUser} className="person-form">
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

                        <label className="availability-toggle">
                            <input
                                name="platformAdmin"
                                type="checkbox"
                                checked={formData.platformAdmin}
                                onChange={handleInputChange}
                                disabled={actionLoading}
                            />
                            Platform admin
                        </label>

                        <button type="submit" disabled={actionLoading}>
                            {actionLoading ? "Guardando..." : "Crear usuario"}
                        </button>
                    </form>

                    <p className="empty-message">
                        La contraseña es temporal mientras exista login manual. A futuro, con Entra ID,
                        la contraseña la administra Microsoft.
                    </p>
                </section>
            )}

            <section className="card">
                <div className="section-header">
                    <div>
                        <span className="eyebrow">Permisos por planta</span>
                        <h2>Usuarios en planta seleccionada</h2>
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
                                <th>Platform admin</th>
                                <th>Rol en esta planta</th>
                                <th>Acciones</th>
                            </tr>
                            </thead>

                            <tbody>
                            {sortedPlantUsers.map((user) => {
                                const roleInPlant = user.roleInPlant || "Sin permisos";
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
                                        <td>
                                            {isGlobalAdmin ? (
                                                <span className="action-badge">Sí</span>
                                            ) : (
                                                <span className="not-responsible-badge">No</span>
                                            )}
                                        </td>
                                        <td>
                        <span
                            className={
                                roleInPlant === "ADMIN" || roleInPlant === "OPERADOR"
                                    ? "action-badge"
                                    : "not-responsible-badge"
                            }
                        >
                          {roleInPlant}
                        </span>
                                        </td>
                                        <td>
                                            <div className="actions">
                                                {canManagePlantUsers && (
                                                    <>
                                                        <button
                                                            type="button"
                                                            onClick={() => handleAssignRole(user.userId, "ADMIN")}
                                                            disabled={
                                                                actionLoading ||
                                                                !user.active ||
                                                                roleInPlant === "ADMIN"
                                                            }
                                                        >
                                                            ADMIN
                                                        </button>

                                                        <button
                                                            type="button"
                                                            onClick={() => handleAssignRole(user.userId, "OPERADOR")}
                                                            disabled={
                                                                actionLoading ||
                                                                !user.active ||
                                                                roleInPlant === "OPERADOR"
                                                            }
                                                        >
                                                            OPERADOR
                                                        </button>

                                                        <button
                                                            type="button"
                                                            className="danger-button"
                                                            onClick={() => handleRemoveRole(user)}
                                                            disabled={
                                                                actionLoading ||
                                                                roleInPlant === "Sin permisos" ||
                                                                isGlobalAdmin
                                                            }
                                                        >
                                                            Quitar permisos
                                                        </button>
                                                    </>
                                                )}

                                                {isPlatformAdmin && (
                                                    <button
                                                        type="button"
                                                        className={user.active ? "danger-button" : "ghost-button"}
                                                        onClick={() => handleToggleGlobalStatus(user)}
                                                        disabled={actionLoading || isCurrentUser || isPrimaryAdmin}
                                                    >
                                                        {user.active ? "Desactivar" : "Activar"}
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

            {isPlatformAdmin && (
                <section className="card">
                    <span className="eyebrow">Usuarios globales</span>
                    <h2>Listado global</h2>

                    {sortedGlobalUsers.length === 0 ? (
                        <p className="empty-message">No hay usuarios globales.</p>
                    ) : (
                        <div className="table-wrapper">
                            <table>
                                <thead>
                                <tr>
                                    <th>Username</th>
                                    <th>Email</th>
                                    <th>Nombre</th>
                                    <th>Estado</th>
                                    <th>Platform admin</th>
                                    <th>Creado</th>
                                </tr>
                                </thead>

                                <tbody>
                                {sortedGlobalUsers.map((user) => (
                                    <tr key={user.id}>
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
                                        <td>
                                            {user.platformAdmin ? (
                                                <span className="action-badge">Sí</span>
                                            ) : (
                                                <span className="not-responsible-badge">No</span>
                                            )}
                                        </td>
                                        <td>{user.createdAt || "-"}</td>
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

export default UsersPage;
