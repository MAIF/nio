export function createAccount(account) {
    return fetch(`/api/nio/accounts`, {
        method: "POST",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(account)
    }).then(r => r.json());
}

export function updateAccount(id, account) {
    return fetch(`/api/nio/accounts/${id}`, {
        method: "PUT",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(account)
    }).then(r => r.json());
}

export function getAccount(id) {
    return fetch(`/api/nio/accounts/${id}`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function deleteAccount(id) {
    return fetch(`/api/nio/accounts/${id}`, {
        method: "DELETE",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function getAccounts(page, pageSize) {
    return fetch(`/api/nio/accounts?page=${page}&pageSize=${pageSize}`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}