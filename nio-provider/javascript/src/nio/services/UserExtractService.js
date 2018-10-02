export function uploadFile(tenant, orgKey, userId, formData, fileName) {
    return fetch(`/api/${tenant}/organisations/${orgKey}/users/${userId}/_files/${fileName}`, {
        method: "POST",
        credentials: 'include',
        body: formData
    }).then(r => r.json());
}