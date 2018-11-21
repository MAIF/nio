# Manage Tenant

A *Tenant* is a dedicated environment that is totally isolated from other tenants.

## Data model

A *Tenant* is just an String id and a description. 

```json
{
  "key": "string",
  "description": "string"
}
```

## Security

Additionnal security header is added to manage the creation and deletion of a tenant.

Please refer to the @ref[documentation](../firstrun/configWithEnv.md#tenant-configuration).


## API

Tenant expose a classic REST API : 

**List all**

```sh
curl --include -X GET http://localhost:9000/api/tenants \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code :


```json
[
  {
    "key": "sandbox",
    "description": "Default tenant from config file"
  }
]
```

**Create a tenant**

```sh
curl --include -X POST http://localhost:9000/api/tenants \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
    -H 'tenant-admin-secret: xxx' \
    -d '{"key": "aNewTenant", "description" : "my new tenant"}'
```

Will respond with a 201 status code :

```json
{
  "key": "aNewTenant",
  "description": "my new tenant"
}
```

**Delete a tenant**

```sh
curl --include -X POST http://localhost:9000/api/tenants/aNewTenant \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
    -H 'tenant-admin-secret: xxx'
```

Will respond with a 200 status code.