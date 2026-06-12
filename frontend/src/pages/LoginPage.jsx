function LoginPage({ onLoginSuccess }) {
    const [mode, setMode] = useState("login");

    const [formData, setFormData] = useState({
        username: "",
        password: "",
        fullName: "",
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    function handleChange(event) {
        const { name, value } = event.target;

        setFormData((current) => ({
            ...current,
            [name]: value,
        }));
    }

    function switchMode(nextMode) {
        setMode(nextMode);
        setError("");
        setSuccessMessage("");

        setFormData({
            username: "",
            password: "",
            fullName: "",
        });
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
            setSuccessMessage("");

            const user = await login(formData.username.trim(), formData.password);

            onLoginSuccess(user);
        } catch (err) {
            if (err.status === 423) {
                setError("El usuario está deshabilitado. Contactá a un administrador.");
            } else {
                setError("Usuario o contraseña incorrectos.");
            }

            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    async function handleRegisterSubmit(event) {
        event.preventDefault();

        if (
            !formData.username.trim() ||
            !formData.password.trim() ||
            !formData.fullName.trim()
        ) {
            setError("Usuario, nombre completo y contraseña son obligatorios.");
            return;
        }

        try {
            setLoading(true);
            setError("");
            setSuccessMessage("");

            await registerUser({
                username: formData.username.trim(),
                password: formData.password,
                fullName: formData.fullName.trim(),
            });

            setSuccessMessage(
                "Cuenta creada como LECTOR. Ahora podés iniciar sesión."
            );

            setMode("login");

            setFormData({
                username: formData.username.trim(),
                password: "",
                fullName: "",
            });
        } catch (err) {
            if (err.status === 400) {
                setError("No se pudo crear la cuenta. Revisá los datos ingresados.");
            } else if (err.status === 409) {
                setError("Ya existe un usuario con ese nombre.");
            } else {
                setError("No se pudo crear la cuenta. Verificá que el usuario no exista.");
            }

            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    const isRegisterMode = mode === "register";

    return (
        <main className="login-page">
            <section className="login-card">
                <span className="eyebrow">Cartelera Responsable de Planta</span>

                <h1>{isRegisterMode ? "Crear cuenta" : "Iniciar sesión"}</h1>

                <p>
                    {isRegisterMode
                        ? "La cuenta se creará inicialmente como LECTOR."
                        : "Ingresá con tu usuario para administrar la cartelera."}
                </p>

                {error && <div className="error-box">{error}</div>}

                {successMessage && (
                    <div className="pending-box">
                        <strong>{successMessage}</strong>
                    </div>
                )}

                <form
                    className="person-form"
                    onSubmit={isRegisterMode ? handleRegisterSubmit : handleLoginSubmit}
                >
                    {isRegisterMode && (
                        <label>
                            Nombre completo
                            <input
                                type="text"
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleChange}
                                required
                            />
                        </label>
                    )}

                    <label>
                        Usuario
                        <input
                            type="text"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            autoComplete="username"
                            required
                        />
                    </label>

                    <label>
                        Contraseña
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            autoComplete={isRegisterMode ? "new-password" : "current-password"}
                            required
                        />
                    </label>

                    <button type="submit" disabled={loading}>
                        {loading
                            ? isRegisterMode
                                ? "Creando..."
                                : "Ingresando..."
                            : isRegisterMode
                                ? "Crear cuenta"
                                : "Ingresar"}
                    </button>
                </form>

                <div className="login-help">
                    {isRegisterMode ? (
                        <>
                            <strong>¿Ya tenés cuenta?</strong>

                            <button
                                type="button"
                                className="link-button"
                                onClick={() => switchMode("login")}
                            >
                                Volver al inicio de sesión
                            </button>
                        </>
                    ) : (
                        <>
                            <strong>¿No tenés cuenta?</strong>

                            <button
                                type="button"
                                className="link-button"
                                onClick={() => switchMode("register")}
                            >
                                Crear cuenta nueva
                            </button>
                        </>
                    )}
                </div>
            </section>
        </main>
    );
}