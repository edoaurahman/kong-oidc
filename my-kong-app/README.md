# my-kong-app/my-kong-app/README.md

# My Kong App

This project sets up a Kong API Gateway with a Keycloak plugin for OpenID Connect (OIDC) authentication.

## Project Structure

- `Dockerfile`: Used to build a Docker image based on Kong version 3.9.0.
- `scripts/add-keycloak.sh`: A script that copies the Keycloak introspection plugin and Kong configuration into the Kong container and restarts the Kong service.

## Getting Started

### Prerequisites

- Docker installed on your machine.
- Basic knowledge of Docker and Kong.

### Building the Docker Image

To build the Docker image, navigate to the project directory and run:

```bash
docker build -t my-kong-app .
```

### Running the Docker Container

After building the image, you can run the container with the following command:

```bash
docker run -d --name kong -p 8000:8000 -p 8443:8443 my-kong-app
```

### Adding the Keycloak Plugin

Once the container is running, you can add the Keycloak plugin by executing the following command:

```bash
docker exec -it kong /bin/bash /usr/local/share/lua/5.1/kong/plugins/add-keycloak.sh
```

### Accessing Kong

You can access the Kong API Gateway at:

- HTTP: `http://localhost:8000`
- HTTPS: `https://localhost:8443`

## License

This project is licensed under the MIT License.