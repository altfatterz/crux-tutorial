# crux-tutorial

### Running
```
$ lein new app crux-tutorial
$ lein run
```

### Creating uberjar
```
$ lein uberjar
$ java -jar target/uberjar/crux-tutorial-0.1.0-SNAPSHOT-standalone.jar
```

### REPL

```
$ lein repl
$ (use 'crux-tutorial.core :reload)
$ (-main)
$ (hello)
```

### Crux

1. PUT

Each document must be in EDN (https://opencrux.com/docs#tutorials-essential-edn) format and must have 