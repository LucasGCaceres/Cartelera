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