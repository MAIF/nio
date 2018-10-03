# DEV


## Mongo, S3 Server, Kafka, Zookeeper

```sbtshell
docker-compose up
```

## Nio 

Backend : 

```sbtshell
sbt 'project nio-server' '~run'
```

Frontend : 

```sbtshell
cd nio-server/javascript
yarn install && yarn start
```

## Nio provider

Backend : 

```sbtshell
sbt -Dhttp.port=9001 'project nio-provider' '~run'
```

Frontend : 

```sbtshell
cd nio-provider/javascript
yarn install && yarn start
```

