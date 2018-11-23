# Manage Data extraction

The extraction of user data is the responsibility of the applications, Nio offers an API to warn that a request to retrieve data has been requested. The extraction request corresponds to the addition of a typed event in Kafka. Once the event read by an application it will then load the file corresponding to the extraction of the user in Nio. Once the file is loaded, an email with a link to the file is sent to the user.

## Data model 

```json
{
  "_id": "string",
  "tenant": "string",
  "orgKey": "string",
  "userId": "string",
  "email": "string",
  "startedAt": "string",
  "uploadStartedAt": "string",
  "endedAt": "string"
}
```

## API

**Data extraction request**

```sh
curl --include -X POST http://localhost:9000/api/demo/organisations/newOrga/users/user1/_extract \
    -H 'Content-Type: application/json' \
    -H 'Accept: application/json' \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx' \
-d '{
  "email": "user1@nio.fr" 
}'
```

Will respond with a 200 status code : 

```json
{
  "_id": "5bf800df210000f4c872428a",
  "tenant": "demo",
  "orgKey": "newOrga",
  "userId": "user1",
  "email": "user1@nio.fr",
  "startedAt": "2018-11-23T13:30:07Z"
}
```

## Upload file 

```sh
curl --include -X POST -F upload=@readme.md http://localhost:9000/api/demo/organisations/newOrga/users/user1/_files/readme.md \
    -H 'Nio-Client-Id: xxx' \
    -H 'Nio-Client-Secret: xxx'
```

Will respond with a 200 status code : 

```json
{
  "url": "http://localhost:9000/_download/readme.md?uploadToken=ZXlKaGJHY2lPaUpJVXpVeE1pSjkuZXlKbGVIUnlZV04wVkdGemEwbGtJam9pTldKbU9EQXdaR1l5TVRBd01EQm1OR000TnpJME1qaGhJaXdpYVhOeklqb2lUM1J2Y205emFHa2lMQ0p1WVcxbElqb2ljbVZoWkcxbExtMWtJaXdpYjNKblMyVjVJam9pYm1WM1QzSm5ZU0lzSW5WelpYSkpaQ0k2SW5WelpYSXhJaXdpWTI5dWRHVnVkRlI1Y0dVaU9pSmhjSEJzYVdOaGRHbHZiaTl2WTNSbGRDMXpkSEpsWVcwaUxDSjBaVzVoYm5RaU9pSmtaVzF2SW4wLm1lU05mLVhZT3BaOWtfM0FuV2RnY2ZWM0ZmNU9wY3pvZzJWVDJUM3VXMTJOeWlJaEpNOHRuY3JlbnFFQkpxOHRJTHM3UmxSdHFCcVdlLWw1WlRWbk13"
}
```
