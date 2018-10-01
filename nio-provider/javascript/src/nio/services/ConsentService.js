export function getConsents(tenant, organisationKey, userId) {

  return fetch(`/api/${tenant}/organisations/${organisationKey}/users/${userId}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  });
}

export function getConsentsTemplate(tenant, organisationKey, userId) {

  return fetch(`/api/${tenant}/organisations/${organisationKey}/users/_template?userId=${userId}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  });
}

export function saveConsents(tenant, organisationKey, userId, user) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/users/${userId}`, {
    method: "PUT",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(user)
  }).then(r => r.json());
}

export function getConsentsHistory(tenant, organisationKey, userId, page, pageSize) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/users/${userId}/logs?page=${page}&pageSize=${pageSize}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}