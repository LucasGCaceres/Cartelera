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
        const error = new Error(errorText || `Error HTTP ${response.status}`);
        error.status = response.status;
        throw error
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

export function getUsers() {
    return request(`/users?ts=${Date.now()}`);
}

export function createUser(userData) {
    return request("/users", {
        method: "POST",
        body: JSON.stringify(userData),
    });
}

export function updateUserRole(userId, role) {
    return request(`/users/${userId}/role`, {
        method: "PATCH",
        body: JSON.stringify({ role }),
    });
}

export function updateUserStatus(userId, active) {
    return request(`/users/${userId}/status`, {
        method: "PATCH",
        body: JSON.stringify({ active }),
    });
}

export function resetUserPassword(userId, password) {
    return request(`/users/${userId}/password`, {
        method: "PATCH",
        body: JSON.stringify({ password }),
    });
}

export function registerUser(userData) {
    return request("/auth/register", {
        method: "POST",
        body: JSON.stringify(userData),
    });
}