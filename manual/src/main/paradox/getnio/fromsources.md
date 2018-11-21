# From sources

to build Nio from sources, you need the following tools :

* git
* JDK 8
* SBT
* node
* yarn

Once you've installed all those tools, go to the [Nio github page](https://github.com/MAIF/nio) and clone the sources :

```sh
git clone https://github.com/MAIF/nio.git
```

then you need to run the `build.sh` script to build the documentation, the React UI and the server :

```sh
sh ./scripts/build.sh
```

and that's all, you can grab your Nio package at `nio/target/scala-2.12/nio` or `nio/target/universal/`.

For those who want to build only parts of Nio, read the following.

## Build the documentation only

Go to `manual` folder and run :

```sh
sbt ';clean;paradox'
```

The documentation is located at `manual/target/paradox/site/main`

## Build the React UI

Go to the `nio-server/javascript` folder and run :

```sh
yarn install && yarn build
```

You will find the JS bundle at `nio/public/javascripts/bundle/bundle.js`.

## Build the Nio server

Run :

```sh
sbt 'project nio-server' ;clean;compile;dist;assembly'
```

You will find your Nio package at `nio/target/scala-2.12/nio` or `nio/target/universal/`.