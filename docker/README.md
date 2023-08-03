## Build & Test Procedure

Ensure you have JDK 17, Maven 3.5.4 (or newer) and Git installed

First clone the Federated Catalogue repository:

``` sh
git clone https://gitlab.com/gaia-x/data-infrastructure-federation-services/cat/fc-service.git
```

## Run Catalog
User docker compose to run start the stack needed to use the Catalog.
 
``` sh
docker-compose up
```
Option for Development that starts the stack with locally build jar files. To build the jars run
`mvn clean install` in the root folder of this repository.
```sh
docker-compose --env-file dev.env up --build
```
### Keycloak setup

When all components started you should setup Keycloak which is used as Identity and Access Management layer in the project. Add keycloak host to your local `hosts` file:

```
127.0.0.1	key-server
```

- Open keycloak admin console at `http://key-server:8080/admin`, with `admin/admin` credentials, select `gaia-x` realm. 
- Go to `Clients` section, select `federated-catalogue` client, go to Credentials tab, Regenerate client Secret, copy it and set to `/docker/.env` file in `FC_CLIENT_SECRET` variable
- Go to users and create one to work with. Set its username and other attributes, save. Then go to Credentials tab, set its password twice, disable Temporary switch, save. Go to Role Mappings tab, in Client Roles drop-down box choose `federated-catalogue` client, select `Ro-MU-CA` role and add it to Assigned Roles.
- Restart federated-catalogue-server container to pick up changes applied at the second step above.

Now you can test FC Service with Demo Portal web app. Go to `http://localhost:8088` in your browser and press Login button. You should be redirected to Keycloak Login page. Use  user credentials you created above..

## Run tests
To run all tests as part of this project run `mvn test` in the root folder of the repository.


## Build Docker-Images manually

### Docker based build 
*This methode only requires to have a working instance of docker installed on your system.*

Go to the main folder of this repository and run the following command:
```sh
# Build an image for the Catalog server 
docker build --target fc-service-server -t fc-service-server . 

# Build an image for the Catalog server 
docker build --target fc-service-server -t fc-service-server . 
```
### Maven based build
For a build without docker you can use the [Maven Jib plugin](https://github.com/GoogleContainerTools/jib) to build container for the catalogs components. 

1. Set the Environment variables  `CI_REGISTRY`, `CI_REGISTRY_USERNAME` and `CI_REGISTRY_PASSWORD`.
2. Run following command in the root folder of this repository:
    ```sh
    mvn compile jib:build
    ```