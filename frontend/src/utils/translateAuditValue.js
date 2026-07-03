export function translateAuditValue(value) {
    if (!value || !value.toString().trim()) {
        return "-";
    }

    const booleanLabels = {
        active: {
            true: "Activo",
            false: "Inactivo",
        },
        available: {
            true: "Disponible",
            false: "No disponible",
        },
        platformAdmin: {
            true: "Administrador de plataforma",
            false: "Usuario estándar",
        },
    };

    const keyLabels = {
        orderNumber: "Orden",
        userId: "Usuario",
        corporateEmail: "Email",
        displayName: "Nombre para mostrar",
        displayTitle: "Título",
        position: "Cargo",
        code: "Código",
        name: "Nombre",
    };

    return value
        .split(/\s*,\s*/)
        .map((chunk) => {
            const [key, ...rest] = chunk.split("=");
            const rawValue = rest.join("=").trim();
            const normalizedKey = key?.trim();

            if (!normalizedKey) {
                return chunk;
            }

            if (booleanLabels[normalizedKey]) {
                return booleanLabels[normalizedKey][rawValue] || `${keyLabels[normalizedKey] || normalizedKey}: ${rawValue}`;
            }

            if (keyLabels[normalizedKey]) {
                return `${keyLabels[normalizedKey]}: ${rawValue}`;
            }

            if (rawValue) {
                return `${normalizedKey}: ${rawValue}`;
            }

            return normalizedKey;
        })
        .join("; ");
}
