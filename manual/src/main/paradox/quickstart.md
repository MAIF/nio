# Quickstart

This is a quick start guide to start with **Nio**.

## Docker Image

You want to start quick ? We provide a docker image in order to be able to start whitout installing all elements of the Architecture.

@@@ div { .centered-img }
<img src="./img/nio-architecture-quickstart.png"/>
@@@

## Getting started

### Nio

First install [Docker](https://docs.docker.com/install/) and [Docker Compose](https://docs.docker.com/compose/install/) for your favorite operating system.

Start Docker, in order to provide [MongoDB](https://www.mongodb.com/), [Kafka](https://kafka.apache.org/) and [S3-compliant](https://fr.wikipedia.org/wiki/Amazon_S3) services in dev mode (for production mode, you'll need to install these services) :

```sh
sudo docker-compose up
```

You will need to install [SBT](https://www.scala-sbt.org/download.html?_ga=2.189648747.1180427007.1527096641-566511216.1527096641) and [Yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable) first.


Start server in dev mode:
```sh
sbt 'project nio-server' '~run'
```

Start client in dev mode :
```sh
cd nio-server/javascript
yarn install
yarn start
```

You may now connect to Nio's back office on [http://localhost:9000/prod1/bo](http://localhost:9000/prod1/bo), default tenant `prod1` is created.

### Going forward with Otoroshi

You need to install to get closer to production environment [Otoroshi](https://maif.github.io/otoroshi/). Look how lucky you are since **Otoroshi** provides an [official Docker image](https://maif.github.io/otoroshi/manual/getotoroshi/fromdocker.html), but you can also get it other ways.

**Otoroshi** will provide some security and API management to Nio.

Once you have installed Otoroshi, you need to [configure **Otoroshi** to expose **Nio** as a service](https://maif.github.io/otoroshi/manual/usage/2-services.html).
