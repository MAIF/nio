# Nio - Dev curl init


## see tenants

```
curl -X GET "http://localhost:9000/api/tenants" \
-H 'Accept:application/json' \
--include
```

## create demo tenant

```
curl -X POST "http://localhost:9000/api/tenants" \
-H 'tenant-admin-secret: changeme' \
-H 'Content-Type: application/json' \
-H 'Accept: application/json' \
--include \
-d '{"key": "demo","description": "Environnement de démonstration"}'
```

## delete demo tenant

```
curl -X DELETE "http://localhost:9000/api/tenants/demo" \
-H 'tenant-admin-secret: changeme' \
-H 'Content-Type: application/json' \
-H 'Accept: application/json' \
--include
```

## create maif organisation

```
curl -X POST \
  http://localhost:9000/api/demo/organisations \
  -H 'accept: application/json' \
  -H 'content-type: application/json' \
  -d '{
    "key": "test",
    "label": "TEST",
    "version": {
        "status": "RELEASED",
        "num": 1,
        "latest": true,
        "lastUpdate" : "2018-05-30T14:21:16Z"
    },
    "groups": [{
        "key": "test_notifications",
        "label": "J'\''accepte de recevoir des offres personnalisées du groupe TEST",
        "permissions": [{
            "key": "phone",
            "label": "Par contact téléphonique"
        }, {
            "key": "email",
            "label": "Par courrier électronique"
        }]
    }, {
        "key": "partners_notifications",
        "label": "J'\''accepte de recevoir des offres personnalisées des partenaires du groupe TEST",
        "permissions": [{
            "key": "phone",
            "label": "Par contact téléphonique"
        }, {
            "key": "email",
            "label": "Par courrier électronique"
        }]
    }]
}'
```

## publish test organisation

```
curl -X POST "http://localhost:9000/api/demo/organisations/test/draft/_release" \
-H 'accept: application/json' \
-H 'content-type: application/json'
```

## create an offer on test organisation 

```
curl -X POST "http://localhost:9000/api/demo/organisations/test/offers" \
-H 'accept: application/json' \
-H 'content-type: application/json' \
-d '{
       "key": "offer1",
       "label": "offre 1",
       "groups": [{
           "key": "test_notifications",
           "label": "J'\''accepte de recevoir des offres personnalisées du groupe TEST",
           "permissions": [{
               "key": "sms",
               "label": "Par sms"
           }]
       }, {
           "key": "partners_notifications",
           "label": "J'\''accepte de recevoir des offres personnalisées des partenaires du groupe TEST",
           "permissions": [{
               "key": "sms",
               "label": "Par sms"
           }]
       }]
   }'
```


## check test organisation has been created

```
curl -X GET "http://localhost:9000/api/demo/organisations/test/last" \
-H 'Accept:application/json' \
--include
```

## get user template

```
curl -X GET "http://localhost:9000/api/demo/organisations/test/users/_template" \
-H 'Accept:application/json' \
--include
```