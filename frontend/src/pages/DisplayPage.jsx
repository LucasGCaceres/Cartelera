import { useEffect, useMemo, useRef, useState } from "react";
import { getPublishedDisplay } from "../api/carteleraApi.js";
import "../styles/display.css";

const DISPLAY_BACKGROUNDS = [
    "/assets/display/Fondo1.jpg",
    // "/assets/display/Fondo2.jpg", // Agregar cuando esté corregida/reexportada
    "/assets/display/Fondo3.jpg",
    "/assets/display/Fondo4.jpg",
];

const DISPLAY_LOGO = "/assets/display/Logo_Gate.png";

function DisplayPage({ currentUser, onLogout }) {
    const [publishedDisplay, setPublishedDisplay] = useState(null);
    const [loading, setLoading] = useState(true);
    const [connectionError, setConnectionError] = useState(false);
    const [now, setNow] = useState(new Date());
    const [backgroundIndex, setBackgroundIndex] = useState(0);
    const [showControls, setShowControls] = useState(false);

    const controlsTimeoutRef = useRef(null);

    async function loadPublishedDisplay() {
        try {
            const data = await getPublishedDisplay();
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
        if (!currentUser) {
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
        DISPLAY_BACKGROUNDS.forEach((src) => {
            const image = new Image();
            image.src = src;
        });
    }, []);

    useEffect(() => {
        loadPublishedDisplay();

        const displayIntervalId = window.setInterval(() => {
            loadPublishedDisplay();
        }, 2000);

        return () => window.clearInterval(displayIntervalId);
    }, []);

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
    }, [currentUser]);

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
        publishedDisplay?.responsiblePosition || "Responsable de Planta";

    return (
        <main
            className="display-page"
            style={{
                backgroundImage: `linear-gradient(90deg, rgba(5, 20, 35, 0.44), rgba(5, 20, 35, 0.12)), url("${backgroundImage}")`,
            }}
        >
            <img
                className="display-logo"
                src={DISPLAY_LOGO}
                alt="Logo Gate Gourmet"
            />

            {currentUser && showControls && (
                <div className="display-session-controls">
                    <button
                        type="button"
                        className="display-hidden-logout-button"
                        onClick={onLogout}
                    >
                        Salir
                    </button>
                </div>
            )}

            <section className="display-layout">
                <article className="display-panel">
                    <div className="display-date-card" aria-label="Fecha actual">
                        <span>{dateParts.month}</span>
                        <strong>{dateParts.day}</strong>
                        <span>{dateParts.year}</span>
                    </div>

                    <div className="display-responsible-card">
                        <div className="display-plant-name">
                            {publishedDisplay?.plantName || "PLANTA EZEIZA"}
                        </div>

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
                                <p>{publishedDisplay?.mainTitle || "Responsable de Planta"}</p>
                            </>
                        )}
                    </div>
                </article>
            </section>

            <time className="display-clock" dateTime={now.toISOString()}>
                {dateParts.time}
            </time>
        </main>
    );
}

export default DisplayPage;