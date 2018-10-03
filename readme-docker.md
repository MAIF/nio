# Docker container

## Build Nio docker image

Build a docker image of maif/nio

```sbtshell
./nio-script.sh build
```


## Build Nio provider docker image

Build a docker image of mail/nio-provider

```sbtshell
./nio-script.sh buildProvider
```


## Run Nio, Nio provider with Otoroshi

Run a docker-compose file to start Nio, Nio-provider, Otoroshi. This script use docker-compose.prod.yml.

```sbtshell
./nio-script.sh run
```


## Clean Nio Build folder

```sbtshell
./nio-script.sh cleanBuild nio-build
```

## Clean Nio run folder and docker

```sbtshell
./nio-script.sh cleanRun nio-run
```



