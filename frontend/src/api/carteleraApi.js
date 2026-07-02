const API_BASE_URL = "/api";

function buildUrl(endpoint) {
    if (!endpoint.startsWith("/")) {
        return `${API_BASE_URL}/${endpoint}`;
    }

    return `${API_BASE_URL}${endpoint}`;
}

function withTimestamp(endpoint) {
    const separator = endpoint.includes("?") ? "&" : "?";
    return `${endpoint}${separator}ts=${Date.now()}`;
}

function encodePathValue(value) {
    return encodeURIComponent(String(value));
}

function extractErrorMessage(response, text) {
    if (!text) {
        return `Error HTTP ${response.status}`;
    }

    try {
        const parsed = JSON.parse(text);

        if (parsed.message) {
            return parsed.message;
        }

        if (parsed.detail) {
            return parsed.detail;
        }

        if (parsed.error) {
            return parsed.error;
        }

        if (parsed.title) {
            return parsed.title;
        }
    } catch {
        // El backend actualmente devuelve muchos errores como texto plano.
    }

    return text;
}

async function request(endpoint, options = {}) {
    const hasBody = options.body !== undefined && options.body !== null;
    const isFormData = typeof FormData !== "undefined" && options.body instanceof FormData;

    const headers = {
        ...(hasBody && !isFormData ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {}),
    };

    const response = await fetch(buildUrl(endpoint), {
        credentials: "include",
        cache: "no-store",
        ...options,
        headers,
    });

    if (!response.ok) {
        const errorText = await response.text();
        const error = new Error(extractErrorMessage(response, errorText));
        error.status = response.status;
        error.responseText = errorText;
        throw error;
    }

    if (response.status === 204) {
        return null;
    }

    const text = await response.text();

    if (!text) {
        return null;
    }

    return JSON.parse(text);
}

/**
 * Auth
 */
export function login(username, password) {
    return request("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
    });
}

export function logout() {
    return request("/auth/logout", {
        method: "POST",
    });
}

export function getCurrentUser() {
    return request(withTimestamp("/auth/me"));
}

/**
 * Plantas
 */
export function getPublicPlants() {
    return request(withTimestamp("/public/plants"));
}

export function getMyPlants() {
    return request(withTimestamp("/plants/my"));
}

export function getPlants() {
    return request(withTimestamp("/plants"));
}

export function createPlant(plantData) {
    return request("/plants", {
        method: "POST",
        body: JSON.stringify(plantData),
    });
}

export function updatePlant(plantCode, plantData) {
    return request(`/plants/${encodePathValue(plantCode)}`, {
        method: "PATCH",
        body: JSON.stringify(plantData),
    });
}

export function updatePlantStatus(plantCode, active) {
    return request(`/plants/${encodePathValue(plantCode)}/status`, {
        method: "PATCH",
        body: JSON.stringify({ active }),
    });
}

/**
 * Estado operativo por planta
 */
export function getPlantAdminState(plantCode) {
    return request(withTimestamp(`/plants/${encodePathValue(plantCode)}/admin/state`));
}

/**
 * Display por planta
 */
export function publishPlantDisplay(plantCode) {
    return request(`/plants/${encodePathValue(plantCode)}/display/publish`, {
        method: "POST",
    });
}

export function getPlantPublishedDisplay(plantCode) {
    return request(withTimestamp(`/plants/${encodePathValue(plantCode)}/display/published`));
}

export function getPublicPlantPublishedDisplay(plantCode) {
    return request(
        withTimestamp(`/public/plants/${encodePathValue(plantCode)}/display/published`)
    );
}

/**
 * Usuarios globales
 */
export function getGlobalUsers() {
    return request(withTimestamp("/users/global"));
}

export function createGlobalUser(userData) {
    return request("/users/global", {
        method: "POST",
        body: JSON.stringify(userData),
    });
}

export function updateGlobalUser(userId, userData) {
    return request(`/users/global/${encodePathValue(userId)}`, {
        method: "PATCH",
        body: JSON.stringify(userData),
    });
}

export function updateGlobalUserStatus(userId, active) {
    return request(`/users/global/${encodePathValue(userId)}/status`, {
        method: "PATCH",
        body: JSON.stringify({ active }),
    });
}

/**
 * Usuarios / roles por planta
 */
export function getPlantUsers(plantCode) {
    return request(withTimestamp(`/plants/${encodePathValue(plantCode)}/users`));
}

export function assignPlantUserRole(plantCode, userId, role) {
    return request(
        `/plants/${encodePathValue(plantCode)}/users/${encodePathValue(userId)}/role`,
        {
            method: "PUT",
            body: JSON.stringify({ role }),
        }
    );
}

export function removePlantUserRole(plantCode, userId) {
    return request(
        `/plants/${encodePathValue(plantCode)}/users/${encodePathValue(userId)}/role`,
        {
            method: "DELETE",
        }
    );
}

/**
 * Miembros de planta
 */
export function getPlantMembers(plantCode) {
    return request(withTimestamp(`/plants/${encodePathValue(plantCode)}/members`));
}

export function addPlantMember(plantCode, userId, position = "") {
    return request(`/plants/${encodePathValue(plantCode)}/members`, {
        method: "POST",
        body: JSON.stringify({ userId, position }),
    });
}

export function addPlantMemberByUserId(plantCode, userId, position = "") {
    return request(
        `/plants/${encodePathValue(plantCode)}/members/${encodePathValue(userId)}`,
        {
            method: "POST",
            body: JSON.stringify({ userId, position }),
        }
    );
}

export function updatePlantMember(plantCode, memberId, data) {
    return request(
        `/plants/${encodePathValue(plantCode)}/members/${encodePathValue(memberId)}`,
        {
            method: "PATCH",
            body: JSON.stringify(data),
        }
    );
}

export function removePlantMember(plantCode, memberId) {
    return request(
        `/plants/${encodePathValue(plantCode)}/members/${encodePathValue(memberId)}`,
        {
            method: "DELETE",
        }
    );
}

export function updatePlantMemberAvailability(plantCode, memberId, available) {
    return request(
        `/plants/${encodePathValue(plantCode)}/members/${encodePathValue(
            memberId
        )}/availability`,
        {
            method: "PATCH",
            body: JSON.stringify({ available }),
        }
    );
}

/**
 * Sucesión
 */
export function getPlantSuccession(plantCode) {
    return request(withTimestamp(`/plants/${encodePathValue(plantCode)}/succession`));
}

export function addMemberToSuccession(plantCode, memberId) {
    return request(
        `/plants/${encodePathValue(plantCode)}/succession/${encodePathValue(memberId)}`,
        {
            method: "POST",
        }
    );
}

export function removeMemberFromSuccession(plantCode, memberId) {
    return request(
        `/plants/${encodePathValue(plantCode)}/succession/${encodePathValue(memberId)}`,
        {
            method: "DELETE",
        }
    );
}

export function moveSuccessionMemberUp(plantCode, memberId) {
    return request(
        `/plants/${encodePathValue(plantCode)}/succession/${encodePathValue(
            memberId
        )}/move-up`,
        {
            method: "POST",
        }
    );
}

export function moveSuccessionMemberDown(plantCode, memberId) {
    return request(
        `/plants/${encodePathValue(plantCode)}/succession/${encodePathValue(
            memberId
        )}/move-down`,
        {
            method: "POST",
        }
    );
}

/**
 * Auditoría
 */
export function getPlantAuditLogs(plantCode) {
    return request(withTimestamp(`/plants/${encodePathValue(plantCode)}/audit-logs`));
}

export function getGlobalAuditLogs() {
    return request(withTimestamp("/audit-logs/global"));
}

/**
 * Compatibilidad temporal para que compilen páginas viejas.
 * Estas funciones se van a eliminar cuando termine la migración completa.
 */
export function getAdminState() {
    throw new Error("Endpoint legacy eliminado. Usar getPlantAdminState(plantCode).");
}

export function createPerson() {
    throw new Error("Función legacy eliminada. Usar addPlantMember.");
}

export function updateAvailability() {
    throw new Error(
        "Función legacy eliminada. Usar updatePlantMemberAvailability(plantCode, memberId, available)."
    );
}

export function movePersonUp() {
    throw new Error("Función legacy eliminada. Usar moveSuccessionMemberUp.");
}

export function movePersonDown() {
    throw new Error("Función legacy eliminada. Usar moveSuccessionMemberDown.");
}

export function deletePerson() {
    throw new Error(
        "Función legacy eliminada. Usar removePlantMember o removeMemberFromSuccession."
    );
}

export function publishDisplay() {
    throw new Error("Endpoint legacy eliminado. Usar publishPlantDisplay(plantCode).");
}

export function getPublishedDisplay() {
    throw new Error(
        "Endpoint legacy eliminado. Usar getPublicPlantPublishedDisplay(plantCode)."
    );
}

export function getAuditLogs() {
    throw new Error("Endpoint legacy eliminado. Usar getPlantAuditLogs o getGlobalAuditLogs.");
}

export function getUsers() {
    throw new Error("Endpoint legacy eliminado. Usar getGlobalUsers o getPlantUsers.");
}

export function createUser() {
    throw new Error("Endpoint legacy eliminado. Usar createGlobalUser.");
}

export function updateUserRole() {
    throw new Error(
        "Endpoint legacy eliminado. Usar assignPlantUserRole o removePlantUserRole."
    );
}

export function updateUserStatus() {
    throw new Error("Endpoint legacy eliminado. Usar updateGlobalUserStatus.");
}

export function resetUserPassword() {
    throw new Error("Reset de contraseña eliminado. Futuro: Entra ID.");
}

export function registerUser() {
    throw new Error(
        "Registro público eliminado. Futuro: alta desde usuarios globales o Entra ID."
    );
}