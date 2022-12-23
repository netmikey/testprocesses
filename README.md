TestProcesses
=============

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.netmikey.testprocesses/testprocesses-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.netmikey.testprocesses/testprocesses-core)

A Java library that manages the lifecycle of applications/programs (processes) needed during testing.


## Purpose

When running integration-, functional- or blackbox-tests, it is often necessary to have additional processes running, besides the JVM actually executing the tests, that provide required functionality for the tests to be run. A few examples are:

  - a shell-script or native non-Java application simulating a specific interface or business need
  - the system-under-test when blackbox-testing[^1]

TestProcesses enables managing one or more of these "test processes" for your tests declaratively.

### When <u>not</u> to use it

- **Unit tests**

  When you're testing isolated code on a small scale, you probably don't want to use TestProcesses. If you're trying to simulate/mock something on that level, you're probably looking for [Mockito](https://site.mockito.org) or some other Java mocking framework.

- **Non-blackbox functional or integration tests**

  In this case, it'll be much easier to have all or part of your application running directly besides your tests, e.g. using [Spring Integration Testing](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#integration-testing).

- **When there's a test library for the test-supporting feature you need**

  When you need to test against mock/assertable variants of common services, chances are there's a Java testing library for that. If that's the case, you'll be far better off using these. Here are a few examples of test libraries for common services:

  - **http server**: [WireMock](https://wiremock.org), [MockServer](https://www.mock-server.com/)
  - **http proxy**: [LittleProxy](https://github.com/adamfisk/LittleProxy), [WireMock](https://wiremock.org), [MockServer](https://www.mock-server.com/)
  - **smtp/pop3/imap email server**: [GreenMail](https://github.com/greenmail-mail-test/greenmail)


- **When you can use Docker**
  
  In case your system-under-test or test-supporting process is available as Docker container, you should probably look into [TestContainers](https://www.testcontainers.org) (see the similarity in name? 😉)


## Features

  - Declarative and imperative using annotations or a Java API
  - Test processes can be anything the OS can run
  - A single test process only has to be defined one and can be reused by as many tests as neccessary
  - A test-process is automatically started before a test if it is not running yet
  - All running test-processes are automatically shut down (destroyed) before the JVM (or more precisely: Spring's Test Context) is shut down
  - Different mechanisms can be used for detecting when a process has finished starting up and shutting down (e.g. TCP Port, Test/pattern in Log-File or stdOut/stdErr,...)
  - Fine-grained control about whether a test process should continue running or be stopped / restarted between test methods
  - A test process' stdOut/stdErr streams can be easily obtained to run assertions. Either the full stream (since the process started) can be returned or just the specific part that has been written during the current test method.
  - It is possible to block the test thread while waiting for an event on the test-process (e.g. a log message on stdOut/stdErr/logFile, a TCP Port to be opened/closed,...)


## Requirements

- Java 11 or above
- JUnit Jupiter
- You must be using Spring Test (ideally `spring-boot-starter-test`)


## Installation

Add TestProcesses to your project's dependencies by declaring `io.github.netmikey.testprocesses:testprocesses-core` as a test-compile-time dependency:

```gradle
dependencies {
    testImplementation("io.github.netmikey.testprocesses:testprocesses-core:1.0.0")
}
```


## Usage

### Defining a test process

Before you can use a test-process, you will have to define it. To do so, you have to implement the `TestProcessDefinition` interface. It is highly recommanded to inherit from `AbstractTestProcessDefinition`:

```java
@Component
public class MyTestProcess extends AbstractTestProcessDefinition {

    public EchoTestProcess() {
        setStartupDetector(LogPatternEventDetector
            .onStdOut()
            .withMarker("My process has started"));
    }

    @Override
    protected void buildProcess(ProcessBuilder builder) {
        builder.command("my-process", "-some", "argument");
    }

}
```

In the example above, we define a test process in a class `MyTestProcess`.

In the `buildProcess()` method, we obtain a [`ProcessBuilder`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/ProcessBuilder.html) instance. We will use it to define how our test process should be started.

In the constructor, we configure a "startupDetector". StartupDetector and ShutdownDetector returned by a `TestProcessDefinition` are used by the framework to detect when that test process has finished starting up or finished shutting down respectively. For both of them, `EventDetector` implementations are used. TestProcesses provides some `EventDetector`s out of the box:

  - `LogPatternEventDetector`: detects the presence of marker strings or text matching a regular expression in log files or stdOur/stdErr streams.
  - `TcpPortEventDetector`: detects when a given port has been opened or closed.
  - `RecursiveProcessTerminationEventDetector`: detects when a test process has stopped running. This EventDetector is used by default as ShutdownDetector in `AbstractTestProcessDefinition`.

In the example above, we tell TestProcesses that whenever it starts the `MyTestProcess` definition, it should block and wait for the process to print the string "My process has started" on its stdOut stream before continuing the tests.

Finally, note the presence of Spring's `@Component` annotation. Using this annotation will create an instance of the `MyTestProcess` definition and register it as singleton in Spring's test context. Registering a definition as Spring bean is one way to have TestProcesses find it when we want to use it later.


### Using a test process for a test

Once a TestProcessDefinition has been created, we can tell TestProcesses to make sure it is running before starting a given test method:

```java
@Test
@TestProcess(MyTestProcess.class)
public void testSomethingThatRequiresMyTestProcess() {
    // my-process will be running here
}
```

> **Warning**
>
> Make sure to enable [auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration) in your Spring test context (e.g. by making sure you have [`@EnableAutoConfiguration`](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/autoconfigure/EnableAutoConfiguration.html) declared).

There are a couple of ways to reference a `TestProcessDefinition`. Referencing it by its class will have TestProcesses look into the Spring Test Context to find a bean of that type (that's why we added `@Component` on `MyTestProcess` above).

It is also possible to use the Spring bean name (note that when using `@Component`, Spring uses the lowercased classname as bean name, which is what we use here):

```java
@Test
@TestProcess(beanName = "myTestProcess")
public void testUsingTheBeanName() {
    // my-process will be running here
}
```

### Test process lifecycle

Using the `@TestProcess` annotation wil have TestProcesses start the targetted `TestProcessDefinition` and make sure it is running before starting the annotated test method. By default, the test process will be left running so that it can be reused by multiple test methods. This avoids stopping and re-starting test processes between test methods, which significantly speeds up testing.

If you need more control over when a test process is stopped and/or restarted, you can specify that in the annotation:

```java
// The test process will be stopped immediately after this test method
@TestProcess(
    beanClass = MyTestProcess.class,
    stopStrategy = StopStrategy.STOP_AFTER_TEST)

// Even if the test process is already running, stop and restart it before starting this test
@TestProcess(
    beanClass = MyTestProcess.class,
    startStrategy = StartStrategy.REQUIRE_RESTART)

// Make sure to (re-)start the test process before this test and to stop it immediately after this test
@TestProcess(
    beanClass = MyTestProcess.class,
    startStrategy = StartStrategy.REQUIRE_RESTART,
    stopStrategy = StopStrategy.STOP_AFTER_TEST)
```

For even more fine-grained control, you will need to use the API.


### Test process identifiers

Each test process definition needs to provide a test process identifier. Identifiers are used as unique keys for detecting wheter a test process is already running or not: whenever a provess with the same identifier is already running, the running process is stopped first before the new one is started, even if the default `StopStrategy.LEAVE_RUNNING` is used.

This enables you to have multiple mutually exclusive `TestProcessDefinition` implementations out of which *at most one* will be running at any time.

As a concrete example for when this might be useful: imagine a test process that needs to be started with different configuration or parameters for different sets of tests. You'd implement 2 `TestProcessDefinition`s with the same identifier. You could then annotate each test with whichever of the 2 `TestProcessDefinition`s it needs and TestProcesses would make sure the process is kept running as much as possible and restarted whenever necessary while never running more than once at a time.

By default, `AbstractTestProcessDefinition` uses the fully qualified implementation class name as identifier.


### Using the API

Sometimes, you need even more control over test processes or you have special use cases. This is when the API comes in handy.

The main entry to TestProcesses API will be the `TestProcessesRegistry`. When [Spring Boot AutoConfiguration](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.auto-configuration) is in use, TestProcesses will automatically register its central `TestProcessesRegistry` instance with your Spring test context. You can then obtain a reference to it for example by simply `@Autowire`ing it in your Spring test class.

The `TestProcessesRegistry` has methods for starting and stopping test processes, using `EventDetector`s to wait for an event on a specific test process, obtaining a test process' stdOur/stdErr streams and more. Feel free to explore its API. Here is a basic example:

```java
import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;

@SpringBootTest
@EnableAutoConfiguration
public class MyTest {

    @Autowired
    private TestProcessesRegistry registry;

    // Doesn't necessarily need to be a Spring bean
    private MyTestProcess myTestProcess = new MyTestProcess();

    @Test
    public void testTestProcessesApi() {
        // Start using a non-Spring TestProcessDefinition
        registry.start(instance(myTestProcess), StartStrategy.USE_EXISTING);

        // ... do something with my-process ...

        /*
         * Wait for my-process to output something like "Operation
         * ID:69 complete" on stdOut.
         * Will throw a TimeoutException if my-process doesn't print
         * a matching string on its stdOut within 5 seconds.
         */
        registry.waitForEventOn(instance(myTestProcess),
            LogPatternEventDetector
                .onStdOut()
                .withTimeoutMillis(5000)
                .withPattern("Operation ID:[\\d]+ complete"));

        /* 
         * You can also test the streams' content (beware of chatty
         * test processes as the streams' content will be loaded
         * into memory.
         */
        Assertions.assertThat(
            registry.stdOutAsStringOf(instance(myTestProcess)))
                .contains("Something was successful.");

        // Stop the test process. Reference it using the definition type.
        registry.stop(clazz(MyTestProcess.class));
    }
}
```

For more examples, see the functional tests in the `testprocesses-core` module.


## Limitations

Because the framework tries to reuse running test processes between tests (if not told otherwise), a test process becomes a shared resource and is, by nature, quite stateful. Because of this, running tests in parallel will most probably not behave as expected.


[^1]: **About blackbox testing**: When blackbox-testing, it's usually desirable to have the SUT as isolated as possible from the tests themselves. Running the SUT within the JVM that is also running your tests then isn't an option (classpath-, memory- and context pollution, different way ot launching, different behavior,...).