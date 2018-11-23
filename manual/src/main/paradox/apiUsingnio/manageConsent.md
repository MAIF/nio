# Manage Consents

Once the *Organisation* and *permissions* managed. The user will be able to manage these consents and modify them. The management of the consents is done by the acceptance or the refusal of the permissions requested by the organisation. For a new user, you can retrieve the latest published version of the permissions of the organisation.

## Data model 

```json
{
  "userId": "string",
  "orgKey": "string",
  "lastUpdate": "string",
  "metaData": [
    {
      "userId": "string",
      "orgKey": "string"
    }
  ],
  "doneBy": {
    "userId": "string",
    "role": "string"
  },
  "version": 0,
  "groups": [
    {
      "key": "string",
      "label": "string",
      "consents": [
        {
          "key": "string",
          "label": "string",
          "checked": true
        }
      ]
    }
  ],
  "offers": [
    {
      "key": "string",
      "label": "string",
      "version": 0,
      "groups": [
        {
          "key": "string",
          "label": "string",
          "consents": [
            {
              "key": "string",
              "label": "string",
              "checked": true
            }
          ]
        }
      ]
    }
  ]
}
```

## API

**Get consent fact template**

```sh
curl --include -X GET http://localhost:9000/api/demo/organisations/newOrga/users/_template \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "userId": "fill",
  "doneBy": {
    "userId": "fill",
    "role": "fill"
  },
  "version": 1,
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        },
        {
          "key": "email",
          "label": "Par e-mail",
          "checked": false
        }
      ]
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        }
      ]
    }
  ],
  "lastUpdate": "2018-11-23T11:12:12Z",
  "orgKey": "newOrga"
}
```

**Note** : You can specify a *userId* query param to build a template based on the last publish of a given organisation and consent fact validated by the given user. It can be very useful if a new version of an organisation has been publish and you want to propose to your user the new version of consent fact pre-filled with the actual consent fact of your user. 


**Create consent fact for a given user**

```sh
curl --include -X PUT http://localhost:9000/api/demo/organisations/newOrga/users/user1 \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
-d '{
  "userId": "user1",
  "doneBy": {
    "userId": "user1",
    "role": "user"
  },
  "version": 1,
  "groups": [
    {
      "key": "grp1",
      "label": "J'\''accepte de recevoir les offres personnalisées",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        },
        {
          "key": "email",
          "label": "Par e-mail",
          "checked": false
        }
      ]
    },
    {
      "key": "grp2",
      "label": "J'\''accepte de recevoir les offres personnalisées des partenaires",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        }
      ]
    }
  ],
  "lastUpdate": "2018-11-23T10:16:05Z",
  "orgKey": "newOrga"
}'
```

Will respond with a 200 status code : 

```json
{
  "userId": "user1",
  "doneBy": {
    "userId": "user1",
    "role": "user"
  },
  "version": 1,
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        },
        {
          "key": "email",
          "label": "Par e-mail",
          "checked": false
        }
      ]
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        }
      ]
    }
  ],
  "lastUpdate": "2018-11-23T10:16:05Z",
  "orgKey": "newOrga"
}
```

**Get user consent fact**

```sh 
curl --include -X GET http://localhost:9000/api/demo/organisations/newOrga/users/user1 \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "userId": "user1",
  "doneBy": {
    "userId": "user1",
    "role": "user"
  },
  "version": 1,
  "groups": [
    {
      "key": "grp1",
      "label": "J'accepte de recevoir les offres personnalisées",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        },
        {
          "key": "email",
          "label": "Par e-mail",
          "checked": false
        }
      ]
    },
    {
      "key": "grp2",
      "label": "J'accepte de recevoir les offres personnalisées des partenaires",
      "consents": [
        {
          "key": "phone",
          "label": "Par téléphone",
          "checked": false
        }
      ]
    }
  ],
  "lastUpdate": "2018-11-23T10:16:05Z",
  "orgKey": "newOrga"
}
```

**Note** : If you got a 404 status code, don't panic! Your user doesn't exist on Nio application but you can get a consent fact template based on organisation groups and permissions.
