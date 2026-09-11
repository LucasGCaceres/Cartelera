import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Topbar from "../components/Topbar.jsx";
import {
    getAllAuditLogs,
    getGlobalUsers,
    getPlantAuditLogs,
    getPlantUsers,
} from "../api/carteleraApi.js";
import { formatDateTime } from "../utils/formatDateTime.js";
import { translateAction, getAuditActionOptions } from "../utils/translateAction.js";
import { translateEntityName } from "../utils/translateEntityName.js";

const PAGE_SIZE = 25;
const EMPTY_PAGINATION = {
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
};

function HistoryPage({
    activeRoute,
    onNavigate,
    currentUser,
    onLogout,
    selectedPlantCode,
    onPlantChange,
}) {
    const currentUserId = currentUser?.id;
    const isPlatformAdmin = Boolean(currentUser?.platformAdmin);

    const manageablePlants = useMemo(() => {
        const plants = currentUser?.plants ?? [];
        if (isPlatformAdmin) {
            return plants;
        }

        return plants.filter((plant) => plant.role === "ADMIN");
    }, [currentUser?.plants, isPlatformAdmin]);

    const hasManageablePlants = manageablePlants.length > 0;

    const initialPlantCode = useMemo(() => {
        if (isPlatformAdmin) {
            return "";
        }

        const selectedPlant = manageablePlants.find(
            (plant) => plant.code === selectedPlantCode
        );

        return selectedPlant?.code ?? manageablePlants[0]?.code ?? "";
    }, [isPlatformAdmin, manageablePlants, selectedPlantCode]);

    const initialFilters = useMemo(
        () => ({
            plantCode: initialPlantCode,
            action: "",
            username: "",
            from: "",
            to: "",
        }),
        [initialPlantCode]
    );

    const [draftFilters, setDraftFilters] = useState(initialFilters);
    const [appliedFilters, setAppliedFilters] = useState(initialFilters);
    const [page, setPage] = useState(0);
    const [auditLogs, setAuditLogs] = useState([]);
    const [pagination, setPagination] = useState(EMPTY_PAGINATION);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [validationError, setValidationError] = useState("");
    const initializedRef = useRef(false);
    const [usernameOptions, setUsernameOptions] = useState([]);
    const [showUsernameDropdown, setShowUsernameDropdown] = useState(false);
    const usernameAutocompleteRef = useRef(null);

    useEffect(() => {
        if (!initializedRef.current && currentUserId !== undefined) {
            setDraftFilters(initialFilters);
            setAppliedFilters(initialFilters);
            setPage(0);
            initializedRef.current = true;
        }
    }, [currentUserId, initialFilters]);

        useEffect(() => {
        let cancelled = false;

        async function loadUsernameOptions() {
            try {
                const plantCodeForLookup = String(draftFilters.plantCode || "").trim();

                const users = plantCodeForLookup
                    ? await getPlantUsers(plantCodeForLookup)
                    : isPlatformAdmin
                        ? await getGlobalUsers()
                        : [];

                if (!cancelled) {
                    setUsernameOptions(users || []);
                }
            } catch (err) {
                if (!cancelled) {
                    setUsernameOptions([]);
                }
                console.error(err);
            }
        }

        loadUsernameOptions();

        return () => {
            cancelled = true;
        };
    }, [draftFilters.plantCode, isPlatformAdmin]);

    useEffect(() => {
        function handleClickOutside(event) {
            if (
                usernameAutocompleteRef.current &&
                !usernameAutocompleteRef.current.contains(event.target)
            ) {
                setShowUsernameDropdown(false);
            }
        }

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const matchingUsernameOptions = useMemo(() => {
        const query = draftFilters.username.trim().toLowerCase();

        if (!query) {
            return [];
        }

        return usernameOptions
            .filter((user) =>
                [user.username, user.fullName]
                    .filter(Boolean)
                    .some((value) => value.toLowerCase().includes(query))
            )
            .slice(0, 8);
    }, [usernameOptions, draftFilters.username]);

    function handleSelectUsernameOption(username) {
        setDraftFilters((previous) => ({
            ...previous,
            username,
        }));
        setShowUsernameDropdown(false);
    }
    
    const plantOptions = useMemo(() => {
        const options = manageablePlants.map((plant) => ({
            value: plant.code,
            label: plant.displayName || plant.name || plant.code,
        }));

        options.sort((a, b) => a.label.localeCompare(b.label, "es"));

        if (isPlatformAdmin) {
            return [{ value: "", label: "Todas las plantas" }, ...options];
        }

        if (!hasManageablePlants) {
            return [{ value: "", label: "Sin plantas administrables" }];
        }

        return options;
    }, [manageablePlants, isPlatformAdmin, hasManageablePlants]);

    const actionOptions = useMemo(
        () => [
            { value: "", label: "Todas las acciones" },
            ...getAuditActionOptions(),
        ],
        []
    );

    const canQueryAuditLogs =
        isPlatformAdmin || (hasManageablePlants && String(appliedFilters.plantCode).trim() !== "");

    const loadAuditLogs = useCallback(async () => {
        if (currentUserId === undefined) {
            return;
        }

        if (!isPlatformAdmin && !hasManageablePlants) {
            setError("No tenés permisos para ver el historial de ninguna planta.");
            setAuditLogs([]);
            setPagination(EMPTY_PAGINATION);
            return;
        }

        if (!isPlatformAdmin && !String(appliedFilters.plantCode).trim()) {
            setError("No tenés permiso para consultar el historial sin una planta seleccionada.");
            setAuditLogs([]);
            setPagination(EMPTY_PAGINATION);
            return;
        }

        setError("");
        setLoading(true);

        try {
            const response = isPlatformAdmin
                ? await getAllAuditLogs({
                      plantCode: appliedFilters.plantCode,
                      action: appliedFilters.action,
                      username: appliedFilters.username,
                      from: appliedFilters.from,
                      to: appliedFilters.to,
                      page,
                      size: PAGE_SIZE,
                  })
                : await getPlantAuditLogs(appliedFilters.plantCode, {
                      action: appliedFilters.action,
                      username: appliedFilters.username,
                      from: appliedFilters.from,
                      to: appliedFilters.to,
                      page,
                      size: PAGE_SIZE,
                  });

            setAuditLogs(response?.content ?? []);
            setPagination({
                page: response?.page ?? 0,
                size: response?.size ?? PAGE_SIZE,
                totalElements: response?.totalElements ?? 0,
                totalPages: response?.totalPages ?? 0,
                first: Boolean(response?.first),
                last: Boolean(response?.last),
            });
        } catch (err) {
            setAuditLogs([]);
            setPagination(EMPTY_PAGINATION);
            setError(err?.message || "No se pudo cargar el historial.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, [currentUserId, isPlatformAdmin, hasManageablePlants, appliedFilters, page]);

    useEffect(() => {
        if (!currentUserId || !canQueryAuditLogs) {
            return;
        }

        (async () => {
            await loadAuditLogs();
        })();
    }, [currentUserId, canQueryAuditLogs, loadAuditLogs]);

    const handleDraftChange = (event) => {
        const { name, value } = event.target;

        setDraftFilters((previous) => ({
            ...previous,
            [name]: value,
        }));
    };

    const handleApplyFilters = (event) => {
        event.preventDefault();
        setValidationError("");
        setError("");

        if (
            draftFilters.from &&
            draftFilters.to &&
            draftFilters.from > draftFilters.to
        ) {
            setValidationError(
                "La fecha desde no puede ser posterior a la fecha hasta."
            );
            return;
        }

        if (!isPlatformAdmin && !String(draftFilters.plantCode).trim()) {
            setValidationError("Debes seleccionar una planta.");
            return;
        }

        setAppliedFilters(draftFilters);
        setPage(0);

        if (
            onPlantChange &&
            draftFilters.plantCode &&
            draftFilters.plantCode !== selectedPlantCode
        ) {
            onPlantChange(draftFilters.plantCode);
        }
    };

    const handleClearFilters = () => {
        setValidationError("");
        setError("");

        setDraftFilters(initialFilters);
        setAppliedFilters(initialFilters);
        setPage(0);

        if (
            onPlantChange &&
            !isPlatformAdmin &&
            initialFilters.plantCode &&
            initialFilters.plantCode !== selectedPlantCode
        ) {
            onPlantChange(initialFilters.plantCode);
        }
    };

    const handlePreviousPage = () => {
        setPage((prevPage) => Math.max(prevPage - 1, 0));
    };

    const handleNextPage = () => {
        setPage((prevPage) =>
            Math.min(prevPage + 1, Math.max(pagination.totalPages - 1, 0))
        );
    };

    return (
        <div className="app-shell">
            <Topbar
                subtitle="Historial"
                activeRoute={activeRoute}
                onNavigate={onNavigate}
                currentUser={currentUser}
                onLogout={onLogout}
                selectedPlantCode={selectedPlantCode}
                onPlantChange={onPlantChange}
            />

            <section className="card">
                <div className="section-header">
                    <div>
                        <p className="eyebrow">Filtros</p>
                        <h2>Historial de movimientos</h2>
                    </div>
                </div>

                {error && <div className="alert alert-error">{error}</div>}
                {validationError && (
                    <div className="alert alert-error">{validationError}</div>
                )}
                {!isPlatformAdmin && !hasManageablePlants && (
                    <div className="alert alert-warning">
                        No tenés permisos para ver el historial de ninguna planta.
                    </div>
                )}

                <form className="person-form" onSubmit={handleApplyFilters}>
                    <div className="form-grid history-filters-form">
                        <label>
                            Planta
                            <select
                                name="plantCode"
                                value={draftFilters.plantCode}
                                onChange={handleDraftChange}
                                disabled={loading || (!isPlatformAdmin && !hasManageablePlants)}
                            >
                                {plantOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Acción
                            <select
                                name="action"
                                value={draftFilters.action}
                                onChange={handleDraftChange}
                                disabled={loading}
                            >
                                {actionOptions.map((option) => (
                                    <option key={option.value} value={option.value}>
                                        {option.label}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Usuario
                            <div className="autocomplete-wrapper" ref={usernameAutocompleteRef}>
                                <input
                                    type="text"
                                    name="username"
                                    placeholder="Buscar por username"
                                    value={draftFilters.username}
                                    onChange={handleDraftChange}
                                    onFocus={() => setShowUsernameDropdown(true)}
                                    disabled={loading}
                                    autoComplete="off"
                                />
                                {showUsernameDropdown && matchingUsernameOptions.length > 0 && (
                                    <div className="autocomplete-dropdown">
                                        {matchingUsernameOptions.map((user) => (
                                            <button
                                                key={user.userId || user.id}
                                                type="button"
                                                className="autocomplete-option"
                                                onClick={() => handleSelectUsernameOption(user.username)}
                                            >
                                                {user.fullName} — {user.username}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </label>

                        <label>
                            Fecha desde
                            <input
                                type="date"
                                name="from"
                                value={draftFilters.from}
                                onChange={handleDraftChange}
                                disabled={loading}
                            />
                        </label>

                        <label>
                            Fecha hasta
                            <input
                                type="date"
                                name="to"
                                value={draftFilters.to}
                                onChange={handleDraftChange}
                                disabled={loading}
                            />
                        </label>
                    </div>

                    <div className="actions">
                        <button
                            type="button"
                            className="ghost-button"
                            onClick={handleClearFilters}
                            disabled={loading}
                        >
                            Limpiar filtros
                        </button>
                        <button type="submit" disabled={loading}>
                            Aplicar filtros
                        </button>
                        <button
                            type="button"
                            onClick={loadAuditLogs}
                            disabled={loading || (!isPlatformAdmin && !hasManageablePlants)}
                        >
                            Actualizar
                        </button>
                    </div>
                </form>

                {!loading && !error && canQueryAuditLogs && (
                    <p>Se encontraron {pagination.totalElements} movimientos.</p>
                )}

                {loading ? (
                    <p>Cargando historial...</p>
                ) : !canQueryAuditLogs ? null : auditLogs.length === 0 ? (
                    <p>No se encontraron movimientos con los filtros aplicados.</p>
                ) : (
                    <>
                        <div className="actions">
                            <button
                                type="button"
                                onClick={handlePreviousPage}
                                disabled={
                                    loading || pagination.first || page === 0
                                }
                            >
                                Anterior
                            </button>
                            <span>
                                Página {pagination.page + 1} de {pagination.totalPages}
                            </span>
                            <button
                                type="button"
                                onClick={handleNextPage}
                                disabled={
                                    loading || pagination.last ||
                                    page >= pagination.totalPages - 1
                                }
                            >
                                Siguiente
                            </button>
                        </div>

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
                                            <td>{log.plantCode || "Global"}</td>
                                            <td>{log.username || "-"}</td>
                                            <td>
                                                <span className="badge badge-info">
                                                    {translateAction(log.action)}
                                                </span>
                                            </td>
                                            <td>{translateEntityName(log.entityName)}</td>
                                            <td>{log.description?.trim() || "-"}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {pagination.totalPages > 0 && (
                            <div className="actions">
                                <button
                                    type="button"
                                    onClick={handlePreviousPage}
                                    disabled={
                                        loading || pagination.first || page === 0
                                    }
                                >
                                    Anterior
                                </button>
                                <span>
                                    Página {pagination.page + 1} de {pagination.totalPages}
                                </span>
                                <button
                                    type="button"
                                    onClick={handleNextPage}
                                    disabled={
                                        loading || pagination.last ||
                                        page >= pagination.totalPages - 1
                                    }
                                >
                                    Siguiente
                                </button>
                            </div>
                        )}
                    </>
                )}
            </section>
        </div>
    );
}

export default HistoryPage;
