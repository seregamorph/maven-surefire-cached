# maven-surefire-cached
Maven surefire/failsafe plugins supporting build caching

First build without tests
```shell
mvn clean install -DskipTests=true
```

Then run unit tests
```shell
mvn surefire-cached:test
```

Or via phase
```shell
mvn test
```

Then run integration tests
```shell
mvn failsafe-cached:integration-test -Dit.test=SampleIT
```
or via phase
```shell
mvn verify
```

Using remote cache
```shell
mvn clean install -DcacheStorageUrl=http://localhost:8080/cache
```
