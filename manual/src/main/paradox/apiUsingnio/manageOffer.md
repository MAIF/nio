# Manage Offer

An *Offer* corresponds to the addition of specific permissions to an application on a given organisation.

*Note* : You only can create an offer on a released organisation.

## Data model 

```json
{
    "key": "string",
    "label": "string",
    "version": 0,
    "groups": [
      {
        "key": "string",
        "label": "string",
        "permissions": [
          {
            "key": "string",
            "label": "string"
          }
        ]
      }
    ]
}
```

## CRUD API

Offers expose a classic CRUD REST api : 

*Note* : Access to offers is restricted by user credentials.

**Create an offer**

```sh
curl --include -X POST http://localhost:9000/api/demo/organisations/newOrga/offers \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
-d '{
    "key": "offer1",
    "label": "Ma première offre",
    "version": 0,
    "groups": [
      {
        "key": "grp1",
        "label": "J'\''accepte de recevoir des offres personnalisées",
        "permissions": [
          {
            "key": "phone",
            "label": "Par téléphone"
          },
          {
            "key": "email",
            "label": "Par e-mail"
          }
        ]
      }
    ]
}'
```

Will respond with a 201 status code : 

```json
{
  "key": "offer1",
  "label": "Ma première offre",
  "version": 1,
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir des offres personnalisées",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        },
        {
          "key": "email",
          "label": "Par e-mail"
        }
      ]
    }
  ]
}
```

**Update an offer**

```sh
curl --include -X PUT http://localhost:9000/api/demo/organisations/newOrga/offers/offer1 \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
-d '{
    "key": "offer1",
    "label": "Ma première offre",
    "version": 0,
    "groups": [
      {
        "key": "grp1",
        "label": "J'\''accepte de recevoir des offres personnalisées",
        "permissions": [
          {
            "key": "phone",
            "label": "Par téléphone"
          },
          {
            "key": "email",
            "label": "Par e-mail"
          },
          {
            "key": "postmail",
            "label": "Par courrier postal"
          }
        ]
      }
    ]
}'
```

Will respond with a 200 status code : 

```json
{
  "key": "offer1",
  "label": "Ma première offre",
  "version": 2,
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir des offres personnalisées",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        },
        {
          "key": "email",
          "label": "Par e-mail"
        },
        {
          "key": "postmail",
          "label": "Par courrier postal"
        }
      ]
    }
  ]
}
```

**List all**

```sh
curl --include -X GET http://localhost:9000/api/demo/organisations/newOrga/offers \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
[{
  "key": "offer1",
  "label": "Ma première offre",
  "version": 2,
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir des offres personnalisées",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        },
        {
          "key": "email",
          "label": "Par e-mail"
        },
        {
          "key": "postmail",
          "label": "Par courrier postal"
        }
      ]
    }
  ]
}]
```

**Delete an offer**


```sh
curl --include -X DELETE http://localhost:9000/api/demo/organisations/newOrga/offers/offer1 \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code.