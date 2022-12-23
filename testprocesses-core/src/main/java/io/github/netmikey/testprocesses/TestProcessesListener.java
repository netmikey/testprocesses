package io.github.netmikey.testprocesses;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.StringUtils;

/**
 * A {@link TestExecutionListener} that ties TestProcesses to Spring's Test
 * lifecycle.
 */
public class TestProcessesListener implements TestExecutionListener {

    private TestProcessesRegistry registry;

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        Set<TestProcess> classAnnotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
            testContext.getTestClass(), TestProcess.class);
        Set<TestProcess> methodAnnotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
            testContext.getTestMethod(), TestProcess.class);

        classAnnotations.forEach(annotation -> {
            executeWithBeanResolution(testContext, annotation, testProcessDefinitionBy -> registry(testContext)
                .start(testProcessDefinitionBy, annotation.startStrategy()));
        });

        methodAnnotations.forEach(annotation -> {
            executeWithBeanResolution(testContext, annotation, testProcessDefinitionBy -> registry(testContext)
                .start(testProcessDefinitionBy, annotation.startStrategy()));
        });

        registry(testContext).beforeTestMethod(testContext);
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        registry(testContext).afterTestMethod(testContext);

        Set<TestProcess> methodAnnotations = AnnotatedElementUtils.findAllMergedAnnotations(testContext.getTestMethod(),
            TestProcess.class);
        Set<TestProcess> classAnnotations = AnnotatedElementUtils.findAllMergedAnnotations(testContext.getTestClass(),
            TestProcess.class);

        methodAnnotations.forEach(annotation -> {
            if (StopStrategy.STOP_AFTER_TEST.equals(annotation.stopStrategy())) {
                executeWithBeanResolution(testContext, annotation, registry(testContext)::stop);
            }
        });
        classAnnotations.forEach(annotation -> {
            if (StopStrategy.STOP_AFTER_TEST.equals(annotation.stopStrategy())) {
                executeWithBeanResolution(testContext, annotation, registry(testContext)::stop);
            }
        });
    }

    private void executeWithBeanResolution(TestContext testContext, TestProcess annotation,
        Consumer<TestProcessDefinitionBy<?>> withReference) {

        Optional<Class<? extends TestProcessDefinition>> beanClass = beanClass(annotation);
        if (beanClass.isPresent() && StringUtils.hasText(annotation.beanName())) {
            throw new IllegalArgumentException("Invalid @TestProcess annotation found in type "
                + testContext.getTestClass().getName()
                + ": either beanClass or beanName should be declared, not both.");
        }
        if (beanClass.isEmpty() && !StringUtils.hasText(annotation.beanName())) {
            throw new IllegalArgumentException("Invalid @TestProcess annotation found in type "
                + testContext.getTestClass().getName() + ": either beanClass or beanName must be specified.");
        }

        if (beanClass.isPresent()) {
            withReference.accept(TestProcessDefinitionBy.clazz(beanClass.get()));
        } else {
            withReference.accept(TestProcessDefinitionBy.beanName(annotation.beanName()));
        }
    }

    private Optional<Class<? extends TestProcessDefinition>> beanClass(TestProcess annotation) {
        if (!VoidTestProcessDefinition.class.equals(annotation.beanClass())) {
            return Optional.of(annotation.beanClass());
        } else if (!VoidTestProcessDefinition.class.equals(annotation.value())) {
            return Optional.of(annotation.value());
        } else {
            return Optional.empty();
        }
    }

    private TestProcessesRegistry registry(TestContext testContext) {
        if (registry == null) {
            registry = testContext.getApplicationContext().getBean(TestProcessesRegistry.class);
        }
        return registry;
    }
}
