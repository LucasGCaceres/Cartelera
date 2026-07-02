import { useState } from "react";
import { login } from "../api/carteleraApi.js";

function LoginPage({ onLoginSuccess }) {
    const [formData, setFormData] = useState({
        username: "",
        password: "",
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    function handleChange(event) {
        const { name, value } = event.target;

        setFormData((current) => ({
            ...current,
            [name]: value,
        }));
    }

    async function handleLoginSubmit(event) {
        event.preventDefault();

        if (!formData.username.trim() || !formData.password.trim()) {
            setError("Usuario y contraseña son obligatorios.");
            return;
        }

        try {
            setLoading(true);
            setError("");

            const user = await login(formData.username.trim(), formData.password);
            onLoginSuccess(user);
        } catch (err) {
            if (err.status === 423) {
                setError("El usuario está deshabilitado. Contactá a un administrador.");
            } else if (err.status === 401) {
                setError("Usuario o contraseña incorrectos.");
            } else {
                setError("No se pudo iniciar sesión. Verificá que el backend esté levantado.");
            }

            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="login-page">
            <section className="login-card">
                <h1>Cartelera Responsable de Planta</h1>

                <h2>Iniciar sesión</h2>

                <p>Ingresá con tu usuario para administrar la cartelera.</p>

                {error && <div className="alert alert-error">{error}</div>}

                <form onSubmit={handleLoginSubmit} className="form-grid">
                    <label>
                        Usuario
                        <input
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            autoComplete="username"
                            autoFocus
                        />
                    </label>

                    <label>
                        Contraseña
                        <input
                            name="password"
                            type="password"
                            value={formData.password}
                            onChange={handleChange}
                            autoComplete="current-password"
                        />
                    </label>

                    <button type="submit" disabled={loading}>
                        {loading ? "Ingresando..." : "Ingresar"}
                    </button>
                </form>
            </section>
        </main>
    );
}

export default LoginPage;