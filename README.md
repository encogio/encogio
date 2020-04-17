# encogio

## Usage

### Install

Install [Homebrew](https://brew.sh)

Install [Redis](https://gist.github.com/tomysmile/1b8a321e7c58499ef9f9441b2faa0aa8)

```sh
brew install redis
```

Install Leiningen

```sh
brew install leiningen
```

Install [sassc](https://github.com/sass/sassc)

```sh
brew install sassc
```

### Development

Ensure Redis is running:

```sh
redis-cli PING
# PONG
```

Run this command on the project directory

```sh
lein figwheel
```

Visit [localhost:3449](http://localhost:3449)

### Configuration

The URL shortener is configured with environment variables.

 - `PORT`: default to 8000
 - `REDIS_URL`: Redis URI, default to 127.0.0.1
 - `SECRET_KEY`: private key for signing tokens
 - `SITE_HOST`: The host of the site, defaults to localhost
 - `SITE_SCHEME`: default to "http"

### Deployment

Create uberjar

```sh
lein uberjar
```

Set environment variables for config and run with

```sh
java $JVM_OPTS -cp target/encogio.jar clojure.main -m encogio.server
```
