# PetClinic Live Debugger Demo

This repository is a fork of the original **Spring PetClinic** application with **two intentionally introduced bugs**.  
It exists to demonstrate the value of a **Live Debugger** for investigating problems directly in a running application.

## Run Petclinic locally

Spring Petclinic is a [Spring Boot](https://spring.io/guides/gs/spring-boot) application built using [Maven](https://spring.io/guides/gs/maven/).
Java 17 or later is required for the build, and the application can run with Java 17 or newer.

You first need to clone the project locally:

```bash
git clone git@github.com:dmitriimorskii/petclinic-livedebugger-demo.git
cd petclinic-livedebugger-demo
```
If you are using Maven, you can start the application on the command-line as follows:

```bash
./mvnw spring-boot:run
```

You can then access the Petclinic at <http://localhost:8080/>.

<img width="1042" alt="petclinic-screenshot" src="https://cloud.githubusercontent.com/assets/838318/19727082/2aee6d6c-9b8e-11e6-81fe-e889a5ddfded.png">

You can, of course, run Petclinic in your favorite IDE.
See below for more details.

## Demo Scenarios (Intentional Bugs)

### 1) HTTP 500 on POST request (Unhandled Exception)

A POST request to the following endpoint returns **500** due to an **unhandled exception**:
```http
POST http://localhost:8080/sms/city/Monona
```
The exception is not properly observable through existing logs, making it a good candidate for live snapshots and runtime inspection.

### 2) Silent Logic Bug (Missing Logs)

Adding a new visit behaves inconsistently depending on the user:

#### User 1
```http
http://localhost:8080/owners/1/pets/1/visits/new
```
No email-related log message is printed.

#### User 2
```http
http://localhost:8080/owners/2/pets/2/visits/new
```
Correctly logs:

```css
Sending Email betty.davis@email.com to owner: Betty Davis
```
This scenario demonstrates a non-exceptional logical bug where behavior differs by runtime state and input, and traditional observability tools provide insufficient insight.

## Contributing

The [issue tracker](https://github.com/spring-projects/spring-petclinic/issues) is the preferred channel for bug reports, feature requests and submitting pull requests.

For pull requests, editor preferences are available in the [editor config](.editorconfig) for easy use in common text editors. Read more and download plugins at <https://editorconfig.org>. All commits must include a __Signed-off-by__ trailer at the end of each commit message to indicate that the contributor agrees to the Developer Certificate of Origin.
For additional details, please refer to the blog post [Hello DCO, Goodbye CLA: Simplifying Contributions to Spring](https://spring.io/blog/2025/01/06/hello-dco-goodbye-cla-simplifying-contributions-to-spring).

## License

The Spring PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
