export function createApiKey(apiKey) {
    return fetch(`/api/apikeys`, {
        method: "POST",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(apiKey)
    }).then(r => r.json());
}

export function updateApiKey(id, apiKey) {
    return fetch(`/api/apikeys/${id}`, {
        method: "PUT",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(apiKey)
    }).then(r => r.json());
}

export function getApiKey(id) {
    return fetch(`/api/apikeys/${id}`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function deleteApiKey(id) {
    return fetch(`/api/apikeys/${id}`, {
        method: "DELETE",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function getApiKeys(page, pageSize) {
    return fetch(`/api/apikeys?page=${page}&pageSize=${pageSize}`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}