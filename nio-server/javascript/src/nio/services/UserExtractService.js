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


export function fetchExtractHistory(tenant, orgKey, page, pageSize) {
    return fetch(`/api/${tenant}/organisations/${orgKey}/_extracted?page=${page}&pageSize=${pageSize}`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function fetchUserExtractHistory(tenant, orgKey, userId, page, pageSize) {
    return fetch(`/api/${tenant}/organisations/${orgKey}/users/${userId}/_extracted?page=${page}&pageSize=${pageSize}`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function uploadFile(tenant, orgKey, userId, formData, fileName, type) {
    return fetch(`/api/${tenant}/organisations/${orgKey}/users/${userId}/_files/${fileName}`, {
        method: "POST",
        credentials: 'include',
        // headers: {
        //     'Content-Type': "multipart/form-data; boundary=974767299852498929531610575",
        // },
        body: formData
    }).then(r => r.json());
}