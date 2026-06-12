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
            setError(err.message || "No se pudo cambiar el rol del usuario.");
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
            setError(err.message || "No se pudo cambiar el estado del usuario.");
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
                                <option value="ADMIN">Administrador</option>
                                <option value="OPERADOR">Operador</option>
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
                            <strong>LECTOR:</strong> solo tiene acceso al display.
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
                                                actionLoading ||
                                                user.username === currentUser.username ||
                                                user.username === "admin"
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
                                                    actionLoading ||
                                                    user.username === currentUser.username ||
                                                    user.username === "admin"
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