# Scalar DL example (Bank app)

A very small simple CLI application to use Scalar DL. This application supports some commands to handle bank account information

## Build

```
$ ./gradlew installDist
```

## Setup

This tool uses certification files to communicate with Scalar DL server.

```
$ ./setup.sh
```

## Execute command

```
$ cd build/install/scalardl-example-bank/
$ bin/scalardl-example-bank -h
Usage: bank [-hV] -c=<path> [COMMAND]
Just a sample application for Scalar DL
  -c, --config=<path>   Client config file
  -h, --help            Show this help message and exit.
  -V, --version         Print version information and exit.
Commands:
  register-cert  register-cert
```

### Register a cert

```
$ cd build/install/scalardl-example-bank/
$ bin/scalardl-example-bank 
```


## Integration testing

TODO

## TODO

- CI

