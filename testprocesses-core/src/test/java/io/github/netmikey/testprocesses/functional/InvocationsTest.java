package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;
import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.github.netmikey.testprocesses.StartStrategy;
import io.github.netmikey.testprocesses.StopStrategy;
import io.github.netmikey.testprocesses.TestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;

/**
 * Test the different ways TestProcesses can be invoked. They all make sure to
 * be stopped after each test and thus each test can make sure the test process
 * has been started through that test spcifically.
 */
@TestProcessesSpringBootTest
public class InvocationsTest {

    @Autowired
    private TestProcessesRegistry registry;

    @Autowired
    @Qualifier("echoTestProcess")
    private EchoTestProcess springEchoTestProcessDefinition;

    private EchoTestProcess anotherEchoTestProcessDefinition = new EchoTestProcess();

    /**
     * Test that a {@link TestProcessDefinition} can be started using the
     * {@link TestProcess} annotation directly on the test method. This test
     * specifies the annotation's {@link TestProcess#beanClass()} attribute.
     */
    @Test
    @TestProcess(beanClass = EchoTestProcess.class, stopStrategy = StopStrategy.STOP_AFTER_TEST)
    public void testInvokeUsingMethodAnnotationWithBeanClass() {
        assertEchoRunningByClass(registry);
    }

    /**
     * Test that a {@link TestProcessDefinition} can be started using the
     * {@link TestProcess} annotation directly on the test method. This test
     * specifies the annotation's {@link TestProcess#beanName()} attribute.
     */
    @Test
    @TestProcess(beanName = "echoTestProcess", stopStrategy = StopStrategy.STOP_AFTER_TEST)
    public void testInvokeUsingMethodAnnotationWithBeanName() {
        assertEchoRunningByClass(registry);
    }

    /**
     * Test that a {@link TestProcessDefinition} can be started and stopped
     * using the {@link TestProcessesRegistry}'s API. This test references the
     * {@link TestProcessDefinition} using the bean class.
     */
    @Test
    public void testInvokeUsingApiWithBeanClass() {
        assertEchoNotRunningByClass(registry);
        registry.start(clazz(EchoTestProcess.class), StartStrategy.USE_EXISTING);
        assertEchoRunningByClass(registry);
        assertEchoRunningByInstance(registry, springEchoTestProcessDefinition);
        assertEchoNotRunningByInstance(registry, anotherEchoTestProcessDefinition);
        registry.stop(clazz(EchoTestProcess.class));
        assertEchoNotRunningByClass(registry);
    }

    /**
     * Test that a {@link TestProcessDefinition} can be started and stopped
     * using the {@link TestProcessesRegistry}'s API. This test references the
     * {@link TestProcessDefinition} using the bean name.
     */
    @Test
    public void testInvokeUsingApiWithBeanName() {
        assertEchoNotRunningByClass(registry);
        registry.start(beanName("echoTestProcess"), StartStrategy.USE_EXISTING);
        assertEchoRunningByClass(registry);
        assertEchoRunningByInstance(registry, springEchoTestProcessDefinition);
        assertEchoNotRunningByInstance(registry, anotherEchoTestProcessDefinition);
        registry.stop(beanName("echoTestProcess"));
        assertEchoNotRunningByClass(registry);
    }

    /**
     * Test that a {@link TestProcessDefinition} can be started and stopped
     * using the {@link TestProcessesRegistry}'s API. This test references the
     * {@link TestProcessDefinition} instance from the Spring context directly.
     */
    @Test
    public void testInvokeUsingApiWithSpringBeanInstance() {
        assertEchoNotRunningByClass(registry);
        registry.start(instance(springEchoTestProcessDefinition), StartStrategy.USE_EXISTING);
        assertEchoRunningByClass(registry);
        assertEchoRunningByInstance(registry, springEchoTestProcessDefinition);
        assertEchoNotRunningByInstance(registry, anotherEchoTestProcessDefinition);
        registry.stop(instance(springEchoTestProcessDefinition));
        assertEchoNotRunningByClass(registry);
    }

    /**
     * Test that a {@link TestProcessDefinition} can be started and stopped
     * using the {@link TestProcessesRegistry}'s API. This test references a
     * {@link TestProcessDefinition} instance defined locally within this test
     * class. This demonstrates that {@link TestProcessDefinition} instances
     * don't necessarily have to be Spring Beans.
     */
    @Test
    public void testInvokeUsingApiWithCustomInstance() {
        assertEchoNotRunningByClass(registry);
        registry.start(instance(anotherEchoTestProcessDefinition), StartStrategy.USE_EXISTING);
        assertEchoRunningByClass(registry);
        assertEchoNotRunningByInstance(registry, springEchoTestProcessDefinition);
        assertEchoRunningByInstance(registry, anotherEchoTestProcessDefinition);
        registry.stop(instance(anotherEchoTestProcessDefinition));
        assertEchoNotRunningByClass(registry);
    }
}
