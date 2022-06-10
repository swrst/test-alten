# test-alten
TEST: Booking API Hotel Cancun

Technologies used:
- Java 11
- Maven
- Spring boot
- Spring Web for REST API
- H2 In-Memory DB
- JPA access repository
- JUnit/Mockito tests
- Spring Doc for API documentation

Project template created with Spring initializer.
Spring REST APIs in BookingController class.

Since we have only one room available we have an in-memory db only
for the reservations. We could configure a "real" DB datasource in application.properties

To launch the application run: BookingApplication.java

The root path of the application is /cancunHotel

The API documentation with a running app, can be displayed:
http://localhost:8080/cancunHotel/swagger-ui/index.html

or downloaded here: http://localhost:8080/cancunHotel/api-docs.yaml

Tests JUnit of the BookingController with mocked reservation repository.