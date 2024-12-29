# Sample non-reactive Rest API implementation using log-safe-web-spring-boot-starter

### What you will need

* About < 5 min using starter project
* Git Client
* JDK 21
* Maven 3.3+

### Steps to run/test
* Simply clone the project
* Do `mvn clean install`
	- Integration tests built should start the server on random (available port) and show the request and response data with sample redaction.
* To run the project do `java -jar target/sample-spirngboot-rest-web-0.0.1-SNAPSHOT.jar`
* Following are sample test curl commands to see the output

```shell
curl --location 'http://localhost:8080/test' \
--header 'Authorization: Bearer secure token blah blah' \
--header 'Content-Type: text' \
--data '{
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "ssn": "123-45-6789",
    "employeeType": "FULL_TIME",
    "address": {
        "state": "NY",
        "city": "New York",
        "phoneNumber": "555-1234"
    }
}'
```

```shell
curl --location 'http://localhost:8080/test?apiKey=test-key&aSecret=blah-secret' \
--header 'Authorization: Bearer blah'
```