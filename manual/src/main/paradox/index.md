# Nio


**Nio** is a privacy management toolbox written in [Scala](https://www.scala-lang.org/) and developped by the [MAIF OSS](https://maif.github.io/) team that implement GDPR.

[GDPR](https://en.wikipedia.org/wiki/General_Data_Protection_Regulation) who is the new EU reglementation on data protection and data privacy. 

The aim of **Nio** is to provide tools to handle, main features are :

* Organisations management
* Consents management
* Data access request API (simple access or portability)
* Data erasure request API

**Nio** does not to the effective erasure or the effective access in your information system (who knows better than your team and your operating system how to provide and to erase data).

> A famous Japanese wooden Kongorikishi (Agyō) statue at Tōdai-ji, Nara (World Heritage Site). It was made by Busshi Unkei and Kaikei in 1203
Niō (仁王) or Kongōrikishi (金剛力士) are two wrathful and muscular guardians of the Buddha standing today at the entrance of many Buddhist temples in East Asian Buddhism in the form of frightening wrestler-like statues. - source : [Wikipedia](https://en.wikipedia.org/wiki/Nio)

@@@ div { .centered-img }
[![Build Status](https://travis-ci.org/MAIF/nio.svg?branch=master)](https://travis-ci.org/MAIF/nio) [![Join the chat at https://gitter.im/MAIF/nio](https://badges.gitter.im/MAIF/nio.svg)](https://gitter.im/MAIF/nio?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [ ![Download](https://img.shields.io/github/release/MAIF/nio.svg) ](https://dl.bintray.com/maif/binaries/nio.jar/latest/nio.jar)
@@@

@@@ div { .centered-img }
<img src="./img/nio_logo.svg" width="300"/>
@@@


@@@ warning

Work is still in progress, stay tuned for our next releases.

Coming next :

* Erasure API
* Front Office
* ...

@@@

## Installation

You can download the latest build of Nio as a [fat jar](https://dl.bintray.com/maif/binaries/nio.jar/latest/nio.jar), as a [zip package](https://dl.bintray.com/maif/binaries/nio-dist/latest/nio-dist.zip) or as a [docker image](getnio/fromdocker.md).

You can install and run Nio with this little bash snippet


```sh
curl -L -o nio.jar 'https://dl.bintray.com/maif/binaries/nio.jar/latest/nio.jar'
export SECURITY_MODE=default
export DB_FLUSH=true
# Java 8
java -jar nio.jar
```

or using docker

```sh
docker run -e "SECURITY_MODE=default" -e "DB_FLUSH=true" -p "9000:9000" maif/nio:latest
```

now open your browser to [http://localhost:9000/sandbox/fo](http://localhost:9000/sandbox/fo), **log in with the credential generated in the logs** and explore by yourself, if you want better instructions, just go to the [Quick Start](quickstart.md) or directly to the [installation instructions](./gitnio/index.md).

## Documentation 

* @ref:[About Nio](about.md)

## Discussion 

Join the [Nio channel](https://gitter.im/MAIF/nio?source=orgpage) on the [MAIF Gitter](https://gitter.im/MAIF).

## Sources

The sources of Nio are available on [Gihub](https://github.com/MAIF/nio)

## Logo

You can find the official Nio logo on [Github](https://github.com/MAIF/nio/blob/master/nio-server/public/images/opun-nio.png). The Nio logo has been created by François Galioto ([@fgalioto](https://twitter.com/fgalioto)).

## Patrons  

The work on Nio was funded by [MAIF](https://www.maif.fr/) with the help of the community.



@@@ index

* [About Nio](about.md)
* [Architecture](architecture.md)
* [Features](features.md)
* [Get Nio](getnio/index.md)
* [First run](firstrun/index.md)
* [Using Nio](uiUsingnio/index.md)
* [Using Nio](apiUsingnio/index.md)

@@@
