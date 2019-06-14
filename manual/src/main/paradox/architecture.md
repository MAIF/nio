# Architecture

We chose to make Nio the central part of our privacy management and provides API to manage consents for your users, request data extraction or request information deletion. 

@@@ div { .centered-img }
<img src="./img/nio-overview.png" />
@@@


Nio tries to embrace @ref:[global philosophy](./about.md#philosophy) by providing a full featured REST admin api, a gorgeous admin dashboard written in [React](https://reactjs.org/) that uses the api. 
Architecture is composed of a JVM-compliant service (Play2 + Scala), a MongoDB database and a Kafka Broker. 


@@@ div { .centered-img }
<img src="./img/nio-architecture.png" />
@@@

## Database

**Nio's** database is useful to stores consents and account data.

You can choose between Mongo or Postgres (at least 9.6), by default Nio uses Mongo database

### Multi-tenancy

**Nio's** database is multi-tenant, which let's you instanciate multiple environnement with only one database. This can be very useful for any *non-prod* envs.

## Events Broker

**Nio** provides events through a Kafka broker (Kafka is the only supported broker for now).

## (Optional) S3-compliant storage for events record management

**Nio** also provides events record management which consists in a chained event logs that needs to be stored in a S3-compliant store.

Record management is optional.

To enable record management, you have to specify an environment variable : 

```sh 
export ENABLE_RECORD_MANAGEMENT=true
export ENABLE_S3_MANAGEMENT=true
```

## (Optional) email notification.

**Nio** provides features to send e-mails using [Mail Gun](https://www.mailgun.com/) or [MailJet](https://api.mailjet.com/), we manage the sending of an e-mail during the finalization of a request of extraction of data with the link of the file to be downloaded.

Email sending is optional.

To enable mail sending, you have to specify an environment variable : 

```sh 
export ENABLE_MAIL_SENDING=true
```

To choose mailgun or mailjet you have to specify an environment variable : 

```sh 
# possible values (mailgun, mailjet), default value mailgun
export MAIL_SENDING_PROVIDER=mailgun
```

For the configuration :

mail gun

```sh
export MAIL_GUN_API_KEY=<my-mail-gun-api-key>
export MAIL_GUN_SENDER=<mymailgun@sender.com>
export MAIL_GUN_ENDPOINT=<https://api.mailgun.net/v3/my-mail-gun-domain>
```

or mailjet

```sh
export MAIL_JET_API_KEY_PUBLIC=<my-mailjet-public-key>
export MAIL_JET_API_KEY_PRIVATE=<my-mailjet-private-key>
export MAIL_JET_SENDER=<mymailjet@sender.com>
export MAIL_JET_ENDPOINT=<https://api.mailjet.com>
```