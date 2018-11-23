# Run Nio

Now you are ready to run Nio. You can run the followinf command with some tweaks depending on the way you want to configure Nio. If you want to pass a custom configuration file, use the `-Dconfig.file=/path/to/file.conf` flasg in the following commands.

## From .zip file

```sh
unzip nio-dist.zip
cd nio
./bin/nio-server
```

## From .jar file

For Java 8

```sh
java -jar nio.jar
```

## From Docker 

```sh
docker run -p "9000:9000" maif/nio:latest
```