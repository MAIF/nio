# Manage organisations and consents

Tenant is created, now we can create the organisation with the consent structure :

```
curl -X POST \
  http://localhost:9000/api/demo/organisations \
-H 'Nio-Client-Id: <XXX>' \
-H 'Nio-Client-Secret: <XXX>' \
-H 'accept: application/json' \
-H 'content-type: application/json' \
-d '{
"key": "organisation-demo",
"label": "organisation-demo",
"groups": [{
    "key": "notifications",
    "label": "I accept to receive personalized offers from organisation-demo",
    "permissions": [{
        "key": "phone",
        "label": "over phone"
    }, {
        "key": "email",
        "label": "over email"
    }]
}, {
    "key": "partners_notifications",
    "label": "I accept to receive personalized offers from organisation-demo's pertners",
    "permissions": [{
        "key": "phone",
        "label": "over phone"
    }, {
        "key": "email",
        "label": "over email"
    }]
}]
}'
```

The consents of the organisation are currently just draft, only remains to release it :

```
curl -X POST "http://localhost:9000/api/demo/organisations/organisation-demo/draft/_release" \
-H 'accept: application/json' \
-H 'content-type: application/json' \
-H 'Nio-Client-Id: <XXX>' \
-H 'Nio-Client-Secret: <XXX>'
```
