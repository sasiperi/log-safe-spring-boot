# Spring Boot Starter for Rest API Logging with Sensitive Data Redacted

A lightweight Spring Boot Starter for logging Rest API request and response details, with built-in support for configurable sensitive data redaction. Simplifies integration and enhances security by masking sensitive information in logs.

#### Features
##### Request Logging
* Logs if `logsafe.logger.in.log-request=true`  (<mark>default value is **true** </mark>)
* Logs the following parameters of an incoming request as soon as it finds a handler (before being processed)
     * Payload (body). Fields @Redact annotated are masked as [REDACTED].
     * Header Key-Values. Header Keys configured as sensitive are masked  [REDACTED].
     * Request/Query Parameters. Param Keys configured as sensitive are masked  [REDACTED].
     * Logging Request Attributes is disabled.
     
     
##### Response Logging
* Logs if `logsafe.logger.in.log-response=true` (<mark>default value is **false**<mark>)
* Logs the following parameters of an incoming request at the end of the request processing, before returning the reponse to client.
     * Payload (body). Fields @Redact annotated are masked as [REDACTED].
     * Header Key-Values. Header Keys configured as sensitive are masked  [REDACTED].
     * Request/Query Parameters. Param Keys configured as sensitive are masked  [REDACTED].
     * Logging Request Attributes is disabled.

##### Sensitive data configuration (to redact)
* Any fields (Pojo/DTO etc.. that makes the payloads) annotated with <mark>@Redact</mark> are masked as [REDACTED], when logging JSON Payloads.
* Header, Request Parameters/Attribs are congured as "," seprated lists in the application properties (via any property source that Spring supports e.g. app-env.props, -Dparam, System-Vars, Env-Vars or via Cloud Config Server etc..)

##### <matk>Limitations (Release 1.0.0)</mark>
* Current version supports WebMVC Rest APIs, as most default to spring-web-*
	- <font color="green">*Support for reactive APIs (spring-reactive-web*) will be supported in Release 2.0.0.</font>
* Currently supports redaction for JSON (or JSON-compatible) MIME types only (i.e., request/response body). All binary content types are filtered.
	- <font color="green">*Support for plain text compatible MIME types (text/html, text/plain etc..) will be supported in Release 2.0.0.</font>
* Currently supports logging/redaction for incoming requests (Resource-Server/API), does not support out going API Calls to another service (Rest-Client)
	- <font color="green">*Support for out going requests (RestTemplate/WebClient) is aimed for Release 2.0.0.</font>

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing.

### What you will build
You will build simple Hello World custom skill, as a webservice (endpoint /helloworld)

### Prerequisites  (what you will need)

* About < 5 min using starter project
* JDK 17 or later (library is built with 21, but should work with 17)
* Gradle 4+ or Maven 3.3+
* Your favorite IDE.


### How to use 
The artifacts are released and available on MavenCentral: (log-safe-spring-boot-starter)[https://central.sonatype.com/artifact/io.github.sasiperi/log-safe-web-spring-boot-starter]

#### Add maven (or gradel) dependency
Crate a spring boot starter project using spring boot initializer available in IDE (rest) or from start.spring.io.




~~~xml
 <dependency>
    <groupId>io.github.sasiperi</groupId>
    <artifactId>log-safe-web-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
~~~

```groovy

 implementation group: 'io.github.sasiperi', name: 'log-safe-web-spring-boot-starter', version: '1.0.0'

```

#### Configuration
* Annotate fields that need to me [REDACTED] as @Redact. (*For sample implementation see [Employee.java](samples/sample-spirngboot-rest-web/src/main/java/io/github/sasiperi/logsafe/Employee.java#L34-L38*).
	   	
	```java
		@Redact
		public String ssn; ```
	

* Below are the available properties, override as needed. (*For sample overrides see [Sample Override](samples/sample-spirngboot-rest-web/src/main/resources/application.properties*).

```properties

logsafe.sensitive.headers=Authorization,x-api-key
logsafe.sensitive.query-params=password,apiKey,token
logsafe.sensitive.request-attributes=csrfToken,refreshToken

#logsafe.logger.base-package-name=

logsafe.logger.in.log-request=true
logsafe.logger.in.log-response=false

```


## Sample Implementation

* You can clone sample implementation from [sample-spring-web-rest-safe-log](samples/samples/sample-spirngboot-rest-web) and run it.
* You run the application, and can test the output with redacted header/query-params/payloads by sending request via postman OR by running integration tests provided within the application.


## Built and Released with

* [Maven](https://maven.apache.org/) - Dependency Management
* [SonaType](https://central.sonatype.com/)
* [Nexus Repo](oss.sonatype.org/) - Artifacts Repo
* [Maven Central](https://repo.maven.apache.org/maven2/io/github/sasiperi/log-safe-web-spring-boot-starter/)
	* [Sonatype Maven Central](https://central.sonatype.com/artifact/io.github.sasiperi/log-safe-web-spring-boot-starter)

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/sasiperi/log-safe-web-spring-boot-starter/CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

[SemVer](http://semver.org/) is used for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags). 

## Company
* [Fourth Quest](www.fourthquest.com)

## Authors

* [Sasi Peri](https://github.com/sasiperi) @FourthQuest
	* Company Home page [Fourth Quest](www.fourthquest.com)
	* [Blog Space](sasiperi.github.io)


## License

This project is licensed under the Apache V2 License - see the [LICENSE](https://github.com/sasiperi/log-safe-spring-boot/blob/master/LICENSE) file for details

## Acknowledgments
1. Thank you: [Jim Shingler](https://www.linkedin.com/in/jimshingler/); (*[@jshingler](https://github.com/jshingler)*)
2. Inspiration: [Timothy Dobransky](https://www.linkedin.com/in/timothy-dobransky-20543024/)

