import { useEffect, useMemo, useRef, useState } from "react";
import { getPublicPlantPublishedDisplay, getPublicPlants } from "../api/carteleraApi.js";
import "../styles/display.css";

const DISPLAY_BACKGROUNDS = [
    "/assets/display/Fondo1.jpg",
    "/assets/display/Fondo3.jpg",
    "/assets/display/Fondo4.jpg",
];

const DISPLAY_LOGO = "/assets/display/Logo_Gate.png";
const DEFAULT_PLANT_CODE = "ezeiza";

function getPlantCodeFromPath() {
    const path = window.location.pathname;
    const parts = path.split("/").filter(Boolean);

    if ((parts[0] === "display" || parts[0] === "app") && parts[1]) {
        return parts[1];
    }

    return null;
}

function getPlantNameFallback(plantCode) {
    return String(plantCode || DEFAULT_PLANT_CODE)
        .replaceAll("-", " ")
        .toUpperCase();
}

function DisplayPage({
                        plantCode,
                         currentUser,
                         onLogout,
                         interactive = false,
                         canOpenPanel = false,
                         onOpenPanel,
                         onPlantChange,
                     }) {
    const resolvedPlantCode = plantCode || getPlantCodeFromPath() || DEFAULT_PLANT_CODE;

    const [publishedDisplay, setPublishedDisplay] = useState(null);
    const [loading, setLoading] = useState(true);
    const [connectionError, setConnectionError] = useState(false);
    const [now, setNow] = useState(new Date());
    const [backgroundIndex, setBackgroundIndex] = useState(0);
    const [showControls, setShowControls] = useState(false);
    const [allPlants, setAllPlants] = useState([]);

    const controlsTimeoutRef = useRef(null);

    async function loadPublishedDisplay() {
        try {
            const data = await getPublicPlantPublishedDisplay(resolvedPlantCode);
            setPublishedDisplay(data);
            setConnectionError(false);
        } catch (err) {
            console.error(err);
            setConnectionError(true);
        } finally {
            setLoading(false);
        }
    }

    function showTemporaryControls() {
        if (!interactive || !currentUser) {
            return;
        }

        setShowControls(true);

        if (controlsTimeoutRef.current) {
            window.clearTimeout(controlsTimeoutRef.current);
        }

        controlsTimeoutRef.current = window.setTimeout(() => {
            setShowControls(false);
        }, 5000);
    }

    useEffect(() => {
        if (!interactive || !currentUser) {
            return undefined;
        }

        let cancelled = false;

        async function loadAllPlants() {
            try {
                const plantsData = await getPublicPlants();

                if (!cancelled) {
                    const sorted = [...(plantsData || [])].sort(
                        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
                    );
                    setAllPlants(sorted);
                }
            } catch (err) {
                console.error(err);
            }
        }

        loadAllPlants();

        return () => {
            cancelled = true;
        };
    }, [interactive, currentUser]);

    useEffect(() => {
        DISPLAY_BACKGROUNDS.forEach((src) => {
            const image = new Image();
            image.src = src;
        });
    }, []);

    useEffect(() => {
        setLoading(true);
        loadPublishedDisplay();

        const displayIntervalId = window.setInterval(() => {
            loadPublishedDisplay();
        }, 5000);

        return () => window.clearInterval(displayIntervalId);
    }, [resolvedPlantCode]);

    useEffect(() => {
        const clockIntervalId = window.setInterval(() => {
            setNow(new Date());
        }, 1000);

        return () => window.clearInterval(clockIntervalId);
    }, []);

    useEffect(() => {
        const backgroundIntervalId = window.setInterval(() => {
            setBackgroundIndex((current) => {
                if (DISPLAY_BACKGROUNDS.length <= 1) {
                    return 0;
                }

                return (current + 1) % DISPLAY_BACKGROUNDS.length;
            });
        }, 30000);

        return () => window.clearInterval(backgroundIntervalId);
    }, []);

    useEffect(() => {
        if (!interactive || !currentUser) {
            return undefined;
        }

        window.addEventListener("mousemove", showTemporaryControls);
        window.addEventListener("mousedown", showTemporaryControls);
        window.addEventListener("touchstart", showTemporaryControls);
        window.addEventListener("keydown", showTemporaryControls);

        return () => {
            window.removeEventListener("mousemove", showTemporaryControls);
            window.removeEventListener("mousedown", showTemporaryControls);
            window.removeEventListener("touchstart", showTemporaryControls);
            window.removeEventListener("keydown", showTemporaryControls);

            if (controlsTimeoutRef.current) {
                window.clearTimeout(controlsTimeoutRef.current);
            }
        };
    }, [interactive, currentUser]);

    const dateParts = useMemo(() => {
        const month = now
            .toLocaleDateString("es-AR", { month: "short" })
            .replace(".", "");

        return {
            month: month.charAt(0).toUpperCase() + month.slice(1),
            day: now.toLocaleDateString("es-AR", { day: "2-digit" }),
            year: now.toLocaleDateString("es-AR", { year: "numeric" }),
            time: now.toLocaleTimeString("es-AR", {
                hour: "2-digit",
                minute: "2-digit",
            }),
        };
    }, [now]);

    const backgroundImage = DISPLAY_BACKGROUNDS[backgroundIndex];
    const responsibleName = publishedDisplay?.responsibleName;
    const responsiblePosition =
        publishedDisplay?.responsiblePosition ||
        publishedDisplay?.mainTitle ||
        "Responsable de planta";
    const plantName = publishedDisplay?.plantName || getPlantNameFallback(resolvedPlantCode);

    return (
        <main
            className="display-page"
            style={{ backgroundImage: `url(${backgroundImage})` }}
            onMouseMove={showTemporaryControls}
            onMouseDown={showTemporaryControls}
            onTouchStart={showTemporaryControls}
        >
            <img className="display-logo" src={DISPLAY_LOGO} alt="Logo Gate Gourmet" />

            {showControls && (
                <div className={`display-controls ${showControls ? "visible" : ""}`}>
                    {interactive && currentUser && (
                        <>
                            {allPlants.length > 0 && (
                                <select
                                    className="display-plant-switcher"
                                    value={resolvedPlantCode}
                                    onChange={(event) => onPlantChange?.(event.target.value)}
                                    aria-label="Cambiar de planta"
                                >
                                    {allPlants.map((plant) => (
                                        <option key={plant.code} value={plant.code}>
                                            {plant.displayName || plant.name || plant.code}
                                        </option>
                                    ))}
                                </select>
                            )}
                            {canOpenPanel && (
                                <button
                                    type="button"
                                    className="display-hidden-logout-button"
                                    onClick={() => onOpenPanel?.(resolvedPlantCode)}
                                >
                                    Panel
                                </button>
                            )}

                            <button
                                type="button"
                                className="display-hidden-logout-button"
                                onClick={onLogout}
                            >
                                Salir
                            </button>
                        </>
                    )}
                </div>
            )}

            <section className="display-layout">
                <div className="display-panel">
                    <aside className="display-date-card" aria-label="Fecha actual">
                        <strong>{dateParts.day}</strong>
                        <span>{dateParts.month}</span>
                        <span>{dateParts.year}</span>
                    </aside>

                    <section className="display-responsible-card">
                        <div className="display-plant-name">{plantName}</div>

                        {connectionError && (
                            <div className="display-connection-warning">
                                Reconectando con el servidor...
                            </div>
                        )}

                        {loading ? (
                            <>
                                <h1>Cargando...</h1>
                                <p>Obteniendo cartelera publicada</p>
                            </>
                        ) : responsibleName ? (
                            <>
                                <h1>{responsibleName}</h1>
                                <p>{responsiblePosition}</p>
                            </>
                        ) : (
                            <>
                                <h1>No hay responsable disponible</h1>
                                <p>{publishedDisplay?.mainTitle || "Responsable de planta"}</p>
                            </>
                        )}
                    </section>
                </div>
            </section>

            <section className="display-clock" aria-label="Hora actual">
                {dateParts.time}
            </section>
        </main>
    );
}

export default DisplayPage;