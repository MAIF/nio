export function extractData(tenant, orgKey, userId, content) {
    return fetch(`/api/${tenant}/organisations/${orgKey}/users/${userId}/_extract`, {
        method: "POST",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(content)
    }).then(r => r.json());
}