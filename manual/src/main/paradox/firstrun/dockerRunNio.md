# Run Nio with Docker

## With Otoroshi


**Build Nio Provider**

```sh
./nio-script.sh buildProvider
```

**Run all**

Before running, you have to setup your hosts. Add the following line to your `/etc/hosts` 

```sh
/etc/hosts: 127.0.0.1    nio.foo.bar nio-provider.foo.bar nio-download.foo.bar kibana.foo.bar elastic.foo.bar keycloak.foo.bar otoroshi.foo.bar otoroshi-api.foo.bar privateapps.foo.bar
```

Replace the ip address at line 161 and line 168 in the docker-compose.prod.yml file.

```sh
./nio-script.sh run
```

**Credentials** 

* [http://keycloak.foo.bar:8889](http://keycloak.foo.bar:8889) : `keycloak` / `password`
* [http://otoroshi.foo.bar:8889](http://otoroshi.foo.bar:8889) : `admin@otoroshi.io` / `password` 
* [http://nio.foo.bar:8889](http://nio.foo.bar:8889) : `admin@nio.io` / `password`
* [http://nio-provider.foo.bar:8889/web](http://nio.foo.bar:8889/web) : `admin@nio.io` / `password`


**Kibana**

[http://kibana.foo.bar:8889](http://kibana.foo.bar:8889) : create kibana index on otoroshi-events-* with @timestamp for time.

To import Otoroshi dashboard for kibana, go to management / saved object / import (the dashboard file is at `./config/kibana.json`)


## With Default security mode

```sh
docker-compose -f docker-compose.dev.yml up
```