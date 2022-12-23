package io.github.netmikey.testprocesses;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotates a type or function that should be wrapped by a process' execution.
 */
@Documented
@Retention(RUNTIME)
@Repeatable(TestProcesses.class)
@Target({ TYPE, METHOD })
public @interface TestProcess {
    /**
     * The {@link TestProcessDefinition} this annotation refers to.
     * 
     * @return The {@link TestProcessDefinition}-implementing class.
     */
    @AliasFor("beanClass")
    Class<? extends TestProcessDefinition> value() default VoidTestProcessDefinition.class;

    /**
     * The {@link TestProcessDefinition} this annotation refers to. Exactly one
     * bean of this type must exist in the test context. Specifying this is
     * mutually exclusive with specifying {@link #beanName()}.
     * 
     * @return The {@link TestProcessDefinition}-implementing class.
     */
    @AliasFor("value")
    Class<? extends TestProcessDefinition> beanClass() default VoidTestProcessDefinition.class;

    /**
     * The name of the bean to be used. The bean with this name needs to exist
     * in the test context and implement {@link TestProcessDefinition}.
     * Specifying this is mutually exclusive with specifying
     * {@link #beanClass()}.
     * 
     * @return The bean name.
     */
    String beanName() default "";

    /**
     * The start strategy to use. Default: {@link StartStrategy#USE_EXISTING}.
     * 
     * @return The {@link StartStrategy} to use.
     */
    StartStrategy startStrategy() default StartStrategy.USE_EXISTING;

    /**
     * The stop strategy to use. Default: {@link StopStrategy#LEAVE_RUNNING}.
     * 
     * @return The {@link StopStrategy} to use.
     */
    StopStrategy stopStrategy() default StopStrategy.LEAVE_RUNNING;
}
