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
