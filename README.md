# Kong API Gateway with Custom Plugins

A customized Kong API Gateway with Keycloak Introspection and Token Manager plugins.

## Features

- **Token Manager Plugin**: Handles token management with Redis storage
- **Keycloak Introspection**: Integration with Keycloak for token validation
- **Custom Authorization Headers**: Configurable authorization header formats
- **Redis Integration**: Token persistence using Redis

## Prerequisites

- Docker
- Docker Compose

## Installation

1. Clone the repository:
```bash
git clone <your-repository-url>
cd kong-oidc
```

2. Build the Docker image:
```bash
docker build -t my-kong-app:1.0 .
```

3. Start the services:
```bash
docker-compose up -d
```

## Configuration

### Token Manager Plugin

Configure the plugin through Kong's admin API:

```bash
curl -X POST http://localhost:8001/services/{service}/plugins \
    --data "name=token-manager" \
    --data "config.header_authorization=Authorization: Bearer \$access_token" \
    --data "config.refresh_endpoint=http://your-auth-server/token" \
    --data "config.content_type=application/json"
```

#### Custom Header Configuration

You can customize the authorization header format:

```json
{
    "header_authorization": "X-API-Key: $access_token"
}
```

or

```json
{
    "header_authorization": "Api-Key: $access_token"
}
```

### Environment Variables

- `KONG_DATABASE`: Set to "off" for DB-less mode
- `KONG_PROXY_ACCESS_LOG`: Location of access logs
- `KONG_ADMIN_ACCESS_LOG`: Location of admin access logs
- `KONG_PROXY_ERROR_LOG`: Location of error logs
- `KONG_ADMIN_ERROR_LOG`: Location of admin error logs
- `KONG_ADMIN_LISTEN`: Admin API URL
- `KONG_ADMIN_GUI_URL`: Admin GUI URL


## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

MIT License

## Contact

- Maintainer: edoaurahman@gmail.com