package io.github.netmikey.testprocesses.functional;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} that configures the test context.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class TestSpringConfiguration {
    // Nothing more to do.
}
