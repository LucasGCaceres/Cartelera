import { useEffect, useMemo, useState } from "react";
import Topbar from "../components/Topbar.jsx";
import {
    getGlobalAuditLogs,
    getPlantAuditLogs,
} from "../api/carteleraApi.js";
import { formatDateTime } from "../utils/formatDateTime.js";
import { translateAction } from "../utils/translateAction.js";
import { translateEntityName } from "../utils/translateEntityName.js";

function HistoryPage({
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

    const plantCode =
        selectedPlantCode || currentUser?.plants?.[0]?.code || "ezeiza";

    const plantDisplayName =
        selectedPlant?.displayName || selectedPlant?.name || plantCode;

    const [auditLogs, setAuditLogs] = useState([]);
    const [scope, setScope] = useState("plant");
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

    const canViewPlantHistory =
        isPlatformAdmin || currentPlantRole === "ADMIN";

    const canViewGlobalHistory = isPlatformAdmin;

    function formatAuditDetail(log) {
        const description = log.description?.trim();
        return description || "-";
    }

    async function loadAuditLogs() {
        try {
            setError("");
            setLoading(true);
            setActionLoading(true);

            if (scope === "global") {
                if (!canViewGlobalHistory) {
                    setAuditLogs([]);
                    setError("No tenés permiso para ver el historial global.");
                    return;
                }

                const data = await getGlobalAuditLogs();
                setAuditLogs(data || []);
                return;
            }

            if (!canViewPlantHistory) {
                setAuditLogs([]);
                setError("No tenés permiso para ver el historial de esta planta.");
                return;
            }

            const data = await getPlantAuditLogs(plantCode);
            setAuditLogs(data || []);
        } catch (err) {
            setError(err.message || "No se pudo cargar el historial.");
            console.error(err);
        } finally {
            setLoading(false);
            setActionLoading(false);
        }
    }

    useEffect(() => {
        loadAuditLogs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [plantCode, scope]);

    function handleScopeChange(event) {
        setScope(event.target.value);
    }

    return (
        <div className="app-shell">
            <Topbar
                subtitle={`Historial — ${scope === "global" ? "Global" : plantDisplayName}`}
                activeRoute={activeRoute}
                onNavigate={onNavigate}
                currentUser={currentUser}
                onLogout={onLogout}
                selectedPlantCode={plantCode}
                onPlantChange={onPlantChange}
            />

            {error && <div className="alert alert-error">{error}</div>}

            {!canViewPlantHistory && scope === "plant" && (
                <div className="alert alert-warning">
                    No tenés permisos para ver el historial de esta planta.
                </div>
            )}

            {!canViewGlobalHistory && scope === "global" && (
                <div className="alert alert-warning">
                    No tenés permisos para ver el historial global.
                </div>
            )}

            <section className="card">
                <p className="eyebrow">Auditoría</p>
                <h3>Historial de movimientos</h3>

                <div className="action-row">
                    <label>
                        Alcance{" "}
                        <select value={scope} onChange={handleScopeChange}>
                            <option value="plant">Planta seleccionada</option>
                            {isPlatformAdmin && <option value="global">Global</option>}
                        </select>
                    </label>

                    <button
                        type="button"
                        onClick={loadAuditLogs}
                        disabled={actionLoading}
                    >
                        {actionLoading ? "Actualizando..." : "Actualizar"}
                    </button>
                </div>

                {loading ? (
                    <p>Cargando historial...</p>
                ) : auditLogs.length === 0 ? (
                    <p>Todavía no hay movimientos registrados.</p>
                ) : (
                    <div className="table-wrapper">
                        <table>
                            <thead>
                            <tr>
                                <th>Fecha y hora</th>
                                <th>Planta</th>
                                <th>Usuario</th>
                                <th>Acción</th>
                                <th>Tipo</th>
                                <th>Detalle</th>
                            </tr>
                            </thead>

                            <tbody>
                            {auditLogs.map((log) => (
                                <tr key={log.id}>
                                    <td>{formatDateTime(log.createdAt)}</td>
                                    <td>{log.plantCode || "-"}</td>
                                    <td>{log.username || "-"}</td>
                                    <td>
                      <span className="badge badge-info">
                        {translateAction(log.action)}
                      </span>
                                    </td>
                                    <td>{translateEntityName(log.entityName)}</td>
                                    <td>{formatAuditDetail(log)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>
        </div>
    );
}

export default HistoryPage;