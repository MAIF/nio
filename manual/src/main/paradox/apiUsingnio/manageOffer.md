# Manage Offer

The notion of `offer` corresponds to the addition of specific permissions to an application on a given organization.

Example : 

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