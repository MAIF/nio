# About Nio

At the beginning of 2017, we had the need to create a new environment to be able to create new "digital" products very quickly in an agile fashion at <a href="https://www.maif.fr/" target="_blank">MAIF</a>. We naturally turned to PaaS solutions and chose the excellent <a href="https://www.clever-cloud.com/">Clever-Cloud</a> product to run our apps.

We also chose that every feature team will have the freedom to choose its own technological stack to build its product. It was a nice move but it has also introduced some challenges in terms of homogeneity for traceability, security, logging, ...

We soon thought that it was very strategic to build a service that could handle all privacy matters exactly the way we would provide any kind of service. That is how **Nio** was born !

## Philosophy

Every OSS product build at <a href="https://www.maif.fr/" target="_blank">MAIF</a> like <a href="https://maif.github.io/otoroshi/" target="_blank">Otoroshi</a> and <a href="https://maif.github.io/izanami/" target="_blank">Izanami</a> follows a common philosophy :

* the services or API provided should be technology agnostic.
* http first: http is the right answer to the previous quote   
* api First: The UI is just another client of the api.
* secured: The services exposed need authentication for both humans or machines  
* event based: The services should expose a way to get notified of what happened inside.