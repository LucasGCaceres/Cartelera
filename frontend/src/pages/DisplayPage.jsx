import {useEffect, useState} from "react";
import {getPublishedDisplay} from "../api/carteleraApi.js";

function DisplayPage({ currentUser, onLogout }) {
    const [publishedDisplay, setPublishedDisplay] = useState(null);
    const [loading, setLoading] = useState(true);
    const [connectionError, setConnectionError] = useState(false);

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

    useEffect(() => {
        loadPublishedDisplay();

        const intervalId = window.setInterval(() => {
            loadPublishedDisplay();
        }, 2000);

        return () => window.clearInterval(intervalId);
    }, []);

    return (
        <main className="display-page">
            {currentUser && (
                <button
                    type="button"
                    className="display-logout-button"
                    onClick={onLogout}
                >
                    Salir
                </button>
            )}
            <section className="display-content">
                <h1>{publishedDisplay?.plantName || "PLANTA EZEIZA"}</h1>

                <p className="display-title">
                    {publishedDisplay?.mainTitle || "Responsable de planta"}
                </p>

                {connectionError && (
                    <p className="display-connection-warning">
                        Reconectando con el servidor...
                    </p>
                )}

                {loading ? (
                    <div className="display-card">
                        <p className="display-message">Cargando...</p>
                    </div>
                ) : publishedDisplay && publishedDisplay.responsibleName ? (
                    <div className="display-card">
                        <h2>{publishedDisplay.responsibleName}</h2>
                        <p>{publishedDisplay.responsiblePosition || "Sin cargo informado"}</p>
                    </div>
                ) : (
                    <div className="display-card">
                        <p className="display-message">No hay responsable disponible</p>
                    </div>
                )}
            </section>
        </main>
    );
}