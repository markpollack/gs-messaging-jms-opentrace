# gs-messaging-jms-opentrace

Experimenting with https://github.com/opentracing-contrib/java-jms

Steps

# Jaeger backend

* Run Jaeger via
```
$ docker run --rm -it --network=host jaegertracing/all-in-one
```

* Run the app which will send and receive a single message
```
$ ./mvnw clean package
$ java -jar target/gs-messaging-jms-0.1.0.jar 
```

* Open the Jaeger UI at `http://localhost:16686/` and look for the service named `spring-boot-jms` and push the `Find Traces` button, you should see one trace with a send and a receive

# Zipkin backend 

**Not working due to version incompatibilities in opentracing libs :( )**

* Run Zipkin via
``` 
docker run --rm -it -p 9411:9411 openzipkin/zipkin
```

* Comment out the `@Bean` method of `public io.opentracing.Tracer jaegerTracer()` in Application.java
* Comment in the `@Bean` method of `public io.opentracing.Tracer zipkinTracer()` in Application.java
* Run the app which will send and receive a single message
```
$ ./mvnw clean package
$ java -jar target/gs-messaging-jms-0.1.0.jar 
```

* Open the Zipkin UI at http://localhost:9411

