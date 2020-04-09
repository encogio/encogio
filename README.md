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

### Deployment

Create uberjar

```sh
lein uberjar
```

Set environment variables:
 - REDIS_URL: default to 127.0.0.1
 - SITE_HOST: default to encog.io
 - PORT: default to 8000

Run it

```sh
java $JVM_OPTS -cp target/encogio.jar clojure.main -m encogio.app
```