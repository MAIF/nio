# Manage Consents

Once the `organization` and `permissions` managed. The user will be able to manage these consents and modify them. The management of the consents is done by the acceptance or the refusal of the permissions requested by the organization. For a new user, you can retrieve the latest published version of the permissions of the organization.

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