package io.github.netmikey.testprocesses.functional;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Meta-annotation for our tests. Mainly to have a central place to define and
 * not have to repeat our logging property.
 */
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = TestSpringConfiguration.class,
    properties = "logging.level.io.github.netmikey.testprocesses=TRACE")
public @interface TestProcessesSpringBootTest {
    // Nothing else to do
}
