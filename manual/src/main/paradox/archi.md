# Architecture

**Nio's** architecture is composed of a JVM-compliant service (Play2 + Scala), a MongoDB databse and a Kafka Broker. For now, you will also need [Otoroshi](https://maif.github.io/otoroshi/) as an API gateway. We will later consider to provide an implementation of Nio that does not need Otoroshi but not yet :D

@@@ div { .centered-img }
<img src="./img/nio-architecture.png" />
@@@

## Database

**Nio's** database is MongoDB, which mostly stores consents and account data.

### Multi-tenancy

**Nio's** database is multi-tenant, which let's you instanciate multiple environnement with only one database. This can be very usefull for any *non-prod* envs.

## Events Broker

**Nio** provides events through a Kafka broker (Kafka is the only supprted broker for now).

## (Optional) S3-compliant storage for events record management

**Nio** also provides events record management which consists in a chained event logs that needs to be stored in a S3-compliant store.

Record management is optional.

## Soon : email notification using mail gun.

**Nio** provides features to send e-mails using [mail gun](https://www.mailgun.com/).
