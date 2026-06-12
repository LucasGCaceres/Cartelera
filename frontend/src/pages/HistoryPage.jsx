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