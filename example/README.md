# Example project

Here's how you would run this HTTP echo JSON pretty printer service:

```shell
$ mvn function:run
```

Then test it with curl:

```shell
jq --null-input '{"a": 1, "b": 3}' | curl --data @/dev/stdin --header Content-Type:application/json :8080
```
