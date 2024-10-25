# HelfenKannJeder project: Helf-O-Mat

[![Build Status](https://travis-ci.org/HelfenKannJeder/helf-o-mat.svg?branch=master)](https://travis-ci.org/HelfenKannJeder/helf-o-mat)

This project is the [Helf-O-Mat](http://helf-o-mat.de) project of HelfenKannJeder.
It is a single page application based on Spring Boot and Angular.

## Build

Everything is available as maven task and the project can be build with `./mvnw -P helfomat-web,helfomat-web-ui,helfomat-import clean package` on the project root level.
There is an embedded node environment which will build the frontend.

To build only one of the different packages, please go ahead and specify the profile accordingly (`-P`):
* `helfomat-import` is going to build the backend service, the artifact is going to be placed in `helfomat-import/target`
* `helfomat-web` is going to build the backend service, the artifact is going to be placed in `helfomat-web/target`
* `helfomat-web-ui` is going to build the backend service, the artifact is going to be placed in `helfomat-web-ui/target`

## Requirements

For the execution of the web app there are two dependencies:

* PostgreSQL Database (or another JDBC database if you change the configuration)
* Elasticsearch 7.x

For the execution of the import app there are three dependencies:

* PostgreSQL Database (or another JDBC database if you change the configuration)
* MySQL (or another JDBC database if you change the configuration)
* Elasticsearch 7.x

To download the basic organizations you can enable the Spring Boot profile `enable-download` which will give you the main organizations available at HelfenKannJeder.de.
Based on them, you are able to create location specific organizations.

## Deployment

For the deployment we are providing docker containers at [Docker Hub](https://hub.docker.com/u/helfenkannjeder).
The container names are aligned to the names of the maven modules. 

### Setup Keycloak

Login to Keycloak Backend  https://keycloakurl.tld/admin  (find credentials in K8s:Config/Secrets/keycloak-secret).
In the [Select-realm]-Dropdown, klick the [Add realm] button and import the [realm-export.json](helfomat-docker/realm-export.json).
In the "Realm Settings" tab "Themes", set Login- and Account-Theme to `helfenkannjeder`.
In the menu "Clients", select `Helfomat-web-profile-admin` Tab "Credentials" and copy the Secret to K8s (helfomat-web-secret: helfomat.keycloak.client.client-secret)
Select Client `Helfomat-web-ui` and set Redirect-URI (`.../helf-o-mat/authenticate`) and Web Origin (App- and Keycloak-Domain)

Now, select realm `master` and import the [helfomat-web-profile-admin.json](helfomat-docker/helfomat-web-profile-admin.json) as a new client.
Inside the client, you still need to go to `Service Account Roles` and select the role `admin` as an Assigned Role.

Next, select the realm Helfenkannjeder, go to Users and create a new user with a password.
The user must have the Role Mapping `helfomat_admin` to be able to i.e. review organization changes.

## Development

For your convenience there is a [docker-compose.yml](helfomat-docker/docker-compose.yml) file available.
This file is starting all required dependencies.
For development mode you can start `helfomat-web` like any normal spring boot applications.
You should use the spring profile `local` to enable the default connectivity to the docker containers.
Inside the frontend project `helfomat-web-ui` you can execute `npm run start` to run it in development mode.
This command is automatically proxying the API to the backend service.

You can reach the API at `http://localhost:8080/`.
The frontend is running at `http://localhost:4200/`.

### Setting up the Users

You first need to go to Keycloak http://localhost:8085 and sign in with the user `admin` and the password `secret`.
Once you signed in, select the realm `master` and import the [helfomat-web-profile-admin.json](helfomat-docker/helfomat-web-profile-admin.json) as a new client.
Inside the client, you still need to go to `Service Account Roles` and select the role `admin` as an Assigned Role.

Next, select the realm Helfenkannjeder, go to Users and create a new user with a password.
The user must have the Role Mapping `helfomat_admin` to be able to grant everything.

### Offline Usage (Unsupported)

For offline usage you can start the spring boot app with the parameter `--spring.profiles.active=offline`.
The frontend can be started with `npm run start:offline`.

## License

GNU GENERAL PUBLIC LICENSE Version 3
