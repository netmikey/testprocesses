package io.github.netmikey.testprocesses;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot Autoconfiguration support for the TestProcesses library.
 */
@AutoConfiguration
public class TestProcessesAutoConfiguration {

    /**
     * Instantiate the {@link TestProcessesRegistry} as singleton within the
     * Spring Context.
     * 
     * @return The new {@link TestProcessesRegistry} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public TestProcessesRegistry testProcessesRegistry() {
        return new TestProcessesRegistry();
    }
}
