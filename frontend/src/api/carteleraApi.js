const API_BASE_URL = "http://localhost:8080/api";

async function request(endpoint, options = {}) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        credentials: "include",
        cache: "no-store",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {}),
        },
        ...options,
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Error HTTP ${response.status}`);
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
    return request("/auth/me");
}

export function getAdminState() {
    return request(`/admin/state?ts=${Date.now()}`);
}

export function createPerson(personData) {
    return request("/persons", {
        method: "POST",
        body: JSON.stringify(personData),
    });
}

export function updateAvailability(personId, available) {
    return request(`/persons/${personId}/availability`, {
        method: "PATCH",
        body: JSON.stringify({ available }),
    });
}

export function movePersonUp(personId) {
    return request(`/persons/${personId}/move-up`, {
        method: "POST",
    });
}

export function movePersonDown(personId) {
    return request(`/persons/${personId}/move-down`, {
        method: "POST",
    });
}

export function deletePerson(personId) {
    return request(`/persons/${personId}`, {
        method: "DELETE",
    });
}

export function publishDisplay() {
    return request("/display/publish", {
        method: "POST",
    });
}

export function getPublishedDisplay() {
    return request(`/display/published?ts=${Date.now()}`);
}

export function getAuditLogs() {
    return request(`/audit-logs?ts=${Date.now()}`);
}