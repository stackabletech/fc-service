## Build & Test Procedure

Ensure you have JDK 11 (or newer), Maven 3.5.4 (or newer) and Git installed

First clone the Federated Catalogue repository:

```
>git clone https://gitlab.com/gaia-x/data-infrastructure-federation-services/cat/fc-service.git
```
Then go to the project folder and build it with maven:

```
>mvn clean install
```

This will build all modules and run the testsuite.

To see how the FC Service works go to `/docker` folder and start it with docker-compose:

```
>cd docker
>docker-compose up
```
### Keycloak setup

When all components started you should setup Keycloak which is used as Identity and Access Management layer in the project. Add keycloak host to your local `hosts` file:

```
127.0.0.1	key-server
```

- Open keycloak admin console at `http://key-server:8080/admin`, with `admin/admin` credentials, select `gaia-x` realm. 
- Go to `Clients` section, select `federated-catalogue` client, go to Credentials tab, Regenerate client Secret, copy it and set to `/docker/.env` file in `FC_CLIENT_SECRET` variable
- Go to users and create one to work with. Set its username and other attributes, save. Then go to Credentials tab, set its password twice, disable Temporary switch, save. Go to Role Mappings tab, select Ro-MU-CA role and add it to Assigned Roles.
- Restart federated-catalogue-server container to pick up changes applied at the second step above.

Now you can test FC Service with Demo Portal web app. Go to `http://localhost:8088` in your browser and press Login button. You should be redirected to Keycloak Login page. Use  user credentials you created above..
