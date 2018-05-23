export function getUsers(tenant, page, pageSize) {
  return fetch(`/api/${tenant}/users?page=${page}&pageSize=${pageSize}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}

export function getUsersByOrganisations(tenant, organisationKey, page, pageSize) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/users?page=${page}&pageSize=${pageSize}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}