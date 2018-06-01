// Get permissions history for an specific organisation
export function getOrganisationReleasesHistoric(tenant, organisationKey) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}

// Get last release for an specific organisation
export function getOrganisationLastRelease(tenant, organisationKey) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/last`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}

// Get a specific version for an specific organisation
export function getOrganisationReleaseVersion(tenant, organisationKey, version) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/${version}`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}

// Get draft for an specific organisation
export function getOrganisationDraft(tenant, organisationKey) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/draft`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}

// Update draft for an specific organisation
export function saveOrganisationDraft(tenant, organisationKey, organisation) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/draft`, {
    method: "PUT",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(organisation)
  }).then(r => r.json());
}


// Create release for an specific organisation
export function createOrganisationRelease(tenant, organisationKey) {
  return fetch(`/api/${tenant}/organisations/${organisationKey}/draft/_release`, {
    method: "POST",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}


export function getOrganisations(tenant) {

  return fetch(`/api/${tenant}/organisations`, {
    method: "GET",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    }
  }).then(r => r.json());
}

export function createOrganisation(tenant, organisation) {
  return fetch(`/api/${tenant}/organisations`, {
    method: "POST",
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(organisation)
  }).then(r => r.json());
}