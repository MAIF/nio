# Manage tenants

For the first time, we need to create a brand new tenant.
Before this, let's see how many tenants exists :

```
curl -X GET "http://localhost:9000/api/tenants" \
-H 'Accept:application/json'
```

Ok, demo tenant doesn't exist, let's create it :

```
curl -X POST "http://localhost:9000/api/tenants" \
-H 'tenant-admin-secret: <XXX>' \
-H 'Nio-Client-Id: <XXX>' \
-H 'Nio-Client-Secret: <XXX>' \
-H 'Content-Type: application/json' \
-H 'Accept: application/json' \
-d '{"key": "demo","description": "Demo Environnement"}'
```