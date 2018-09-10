export function extractData(tenant, orgKey, userId) {
    return fetch(`/api/${tenant}/organisations/${orgKey}/users/${userId}/_extract`, {
        method: "POST",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}