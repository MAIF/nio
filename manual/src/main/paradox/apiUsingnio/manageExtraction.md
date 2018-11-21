# Manage Data extraction

The extraction of user data is the responsibility of the applications, Nio offers an API to warn that a request to retrieve data has been requested. The extraction request corresponds to the addition of a typed event in Kafka. Once the event read by an application it will then load the file corresponding to the extraction of the user in Nio. Once the file is loaded, an email with a link to the file is sent to the user.
