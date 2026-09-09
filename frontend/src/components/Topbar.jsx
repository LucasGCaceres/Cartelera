function Topbar({
                    subtitle,
                    activeRoute,
                    onNavigate,
                    currentUser,
                    onLogout,
                    selectedPlantCode,
                    onPlantChange,
                }) {
    const plants = currentUser?.plants || [];
    const isPlatformAdmin = Boolean(currentUser?.platformAdmin);

    const effectivePlantCode = selectedPlantCode || plants[0]?.code || "ezeiza";
    const selectedPlant = plants.find((plant) => plant.code === effectivePlantCode);
    const selectedRole = selectedPlant?.role || (isPlatformAdmin ? "ADMIN" : "Sin permisos");

    const canViewUsers = isPlatformAdmin || selectedPlant?.role === "ADMIN";
    const canViewHistory = canViewUsers;

    function handlePlantChange(event) {
        onPlantChange?.(event.target.value);
    }

    return (
        <header className="topbar">
            <div>
                <h1>Cartelera responsable de planta</h1>
                {subtitle && <p>{subtitle}</p>}
            </div>

            <div className="topbar-actions">
                {plants.length > 0 && (
                    <label className="topbar-plant-selector">
                        {" "}
                        <select value={effectivePlantCode} onChange={handlePlantChange}>
                            {plants.map((plant) => (
                                <option key={plant.code} value={plant.code}>
                                    {plant.displayName || plant.name || plant.code}
                                </option>
                            ))}
                        </select>
                    </label>
                )}

                <button
                    type="button"
                    className={`nav-button ${activeRoute === "admin" ? "active" : ""}`}
                    onClick={() => onNavigate("admin")}
                >
                    Panel
                </button>

                {canViewHistory && (
                    <button
                        type="button"
                        className={`nav-button ${activeRoute === "history" ? "active" : ""}`}
                        onClick={() => onNavigate("history")}
                    >
                        Historial
                    </button>
                )}

                {canViewUsers && (
                    <button
                        type="button"
                        className={`nav-button ${activeRoute === "users" ? "active" : ""}`}
                        onClick={() => onNavigate("users")}
                    >
                        Usuarios
                    </button>
                )}

                <button
                    type="button"
                    className="nav-button display-nav-button"
                    onClick={() => onNavigate("app", effectivePlantCode)}
                >
                    Abrir cartelera
                </button>
            </div>

            <div className="user-pill">
                <span>{currentUser?.fullName || currentUser?.username || "Usuario"}</span>
                <strong>{isPlatformAdmin ? "PLATFORM ADMIN" : selectedRole}</strong>
            </div>

            <button
                type="button"
                className="nav-button logout-button"
                onClick={onLogout}
            >
                Salir
            </button>
        </header>
    );
}

export default Topbar;