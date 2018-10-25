export function getOffers(tenant, organisationKey) {
    return fetch(`/api/${tenant}/organisations/${organisationKey}/offers`, {
        method: "GET",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    }).then(r => r.json());
}

export function createOffer(tenant, organisationKey, offerToCreate) {
    return fetch(`/api/${tenant}/organisations/${organisationKey}/offers`, {
        method: "POST",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(offerToCreate)
    });
}

export function updateOffer(tenant, organisationKey, offerKey, offerToUpdate) {
    return fetch(`/api/${tenant}/organisations/${organisationKey}/offers/${offerKey}`, {
        method: "PUT",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(offerToUpdate)
    });
}

export function deleteOffer(tenant, organisationKey, offerKey) {
    return fetch(`/api/${tenant}/organisations/${organisationKey}/offers/${offerKey}`, {
        method: "DELETE",
        credentials: 'include',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        }
    });
}