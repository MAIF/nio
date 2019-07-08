#manage ~~users~~ consents

Nio can manage users **but** by the  prism of consents. So, informed, "to create user", just put new consents.
If user already has consents, they will be updated if not created.

let's have a nice usecase. we want to show to a user (userId in our system USER-1) his consentFacts to update it.
```
curl -X GET "http://localhost:9000/api/demo/organisations/organisation-demo/users/USER-1" \
-H 'accept: application/json' \
-H 'content-type: application/json' \
-H 'Nio-Client-Id: <XXX>' \
-H 'Nio-Client-Secret: <XXX>'
```

the status of the response must be 404, this not an error just an information who tells us that the user is not fouond (because it's a new user). 
No problem, let's get back the consents template :

```
curl -X GET "http://localhost:9000/api/demo/organisations/organisation-demo/users/_template " \
-H 'accept: application/json' \
-H 'content-type: application/json' \
-H 'Nio-Client-Id: <XXX>' \
-H 'Nio-Client-Secret: <XXX>'
```
the response contains consents initialized at false and require the update of some informations  :
```
{
  "userId": "fill",
  "doneBy": {
    "userId": "fill",
    "role": "fill"
  },
  "version": 1,
  "groups": [
    {
      "key": "notifications",
      "label": "I accept to receive personalized offers from organisation-demo",
      "consents": [
        {
          "key": "phone",
          "label": "over phone",
          "checked": false
        },
        {
          "key": "email",
          "label": "over email",
          "checked": false
        }
      ]
    },
    {
      "key": "partners_notifications",
      "label": "I accept to receive personalized offers from organisation-demo's pertners",
      "consents": [
        {
          "key": "phone",
          "label": "over phone",
          "checked": false
        },
        {
          "key": "email",
          "label": "over email",
          "checked": false
        }
      ]
    }
  ],
  "lastUpdate": "2019-07-05T13:23:56Z",
  "orgKey": "organisationDemo"
}
```

Ok, we can put user consentFact now : 

```
curl -X PUT "http://localhost:9000/api/demo/organisations/organisation-demo/users/USER_1 " \
-H 'accept: application/json' \
-H 'content-type: application/json' \
-H 'Nio-Client-Id: <XXX>' \
-H 'Nio-Client-Secret: <XXX>'
-d '{
      "userId": "USER_1",
      "doneBy": {
        "userId": "admin_demo",
        "role": "admin"
      },
      "version": 1,
      "groups": [
        {
          "key": "notifications",
          "label": "I accept to receive personalized offers from organisation-demo",
          "consents": [
            {
              "key": "phone",
              "label": "over phone",
              "checked": true
            },
            {
              "key": "email",
              "label": "over email",
              "checked": true
            }
          ]
        },
        {
          "key": "partners_notifications",
          "label": "I accept to receive personalized offers from organisation-demo's pertners",
          "consents": [
            {
              "key": "phone",
              "label": "over phone",
              "checked": true
            },
            {
              "key": "email",
              "label": "over email",
              "checked": true
            }
          ]
        }
      ],
      "orgKey": "organisationDemo"
    }'
```

it's as easy as it looks like.
