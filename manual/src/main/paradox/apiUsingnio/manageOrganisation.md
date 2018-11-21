# Manage Organisation

An *Organization* is a logical entity, for example Maif, Altima ... Groups can be defined in an organization, for example within the Maif organization one can have a Maif group and a Maif partner group. Permissions are defined for each of these groups (eg contact by email, phone ...). Version management is done in draft or in publication. The draft version can be published, so it will become unmodifiable, but a new draft version will be created.

## Data model 

```json
[
  {
    "key": "string",
    "label": "string",
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
    ],
    "version": {
      "status": "string",
      "num": 0,
      "latest": true,
      "neverReleased": true,
      "lastUpdate": "string"
    }
  }
]
```

## API

**Create an organisation**

When you create an organisation, you create a *draft* version of this organisation. A draft is just a copy to work on and perform your groups and your permissions. You can not use it as is to manage users.

```sh
curl --include -X POST http://localhost:9000/api/demo/organisations \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
    -d '{
      "key": "newOrga",
      "label": "Ma nouvelle organisation",
      "groups": [
        {
          "key": "grp1",
          "label": "J'\''accepte de recevoir les offres personnalisées",
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
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "version": {
    "status": "DRAFT",
    "num": 1,
    "latest": false,
    "lastUpdate": "2018-11-23T08:25:49Z"
  },
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
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

### Organisation draft lifecycle

**Get the draft version**

```sh
curl --include -X GET http://localhost:9000/api/demo/organisations/newOrga/draft \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "version": {
    "status": "DRAFT",
    "num": 1,
    "latest": false,
    "lastUpdate": "2018-11-23T08:25:49Z"
  },
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
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

**Update a draft**

*Note* : You cannot update the status, version number, latest. This fields are only managed by the Nio backend.

```sh 
curl --include -X PUT http://localhost:9000/api/demo/organisations/newOrga/draft \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
-d '{
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "groups": [
    {
      "key": "grp1",
      "label": "J'\''accepte de recevoir les offres personnalisées",
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
    },
    {
      "key": "grp2",
      "label": "J'\''accepte de recevoir les offres personnalisées des partenaires",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        }
      ]
    }
  ]
}'
```

Will respond with a 200 status code : 

```json
{
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "version": {
    "status": "DRAFT",
    "num": 1,
    "latest": false,
    "lastUpdate": "2018-11-23T08:42:34Z"
  },
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
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
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        }
      ]
    }
  ]
}
```

**Release draft**

```sh 
curl --include -X POST http://localhost:9000/api/demo/organisations/newOrga/draft/_release \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "version": {
    "status": "RELEASED",
    "num": 1,
    "latest": true,
    "lastUpdate": "2018-11-23T08:49:31Z"
  },
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
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
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        }
      ]
    }
  ]
}
```

*Note* : You can now use this organisation to associate consent facts.

### Organisation recovery

**Get all**

Retrieving the list of known organizations either by taking the latest version published for this organization or by taking the draft version.

```sh
curl --include -X GET http://localhost:9000/api/demo/organisations \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
[
  {
    "key": "newOrga",
    "label": "Ma nouvelle organisation",
    "version": {
      "status": "RELEASED",
      "num": 1,
      "lastUpdate": "2018-11-23T08:49:31Z"
    }
  }
]
```

**Get the last published version of a given organisation**

```sh
curl --include -X GET http://localhost:9000/api/demo/organisations/newOrga/last \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "version": {
    "status": "RELEASED",
    "num": 1,
    "latest": true,
    "lastUpdate": "2018-11-23T08:49:31Z"
  },
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
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
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        }
      ]
    }
  ]
}
```

**Get the specific published version of a given organisation**

```sh
curl --include -X GET http://localhost:9000/api/demo/organisations/newOrga/1\
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "key": "newOrga",
  "label": "Ma nouvelle organisation",
  "version": {
    "status": "RELEASED",
    "num": 1,
    "latest": true,
    "lastUpdate": "2018-11-23T08:49:31Z"
  },
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
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
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "permissions": [
        {
          "key": "phone",
          "label": "Par téléphone"
        }
      ]
    }
  ]
}
```