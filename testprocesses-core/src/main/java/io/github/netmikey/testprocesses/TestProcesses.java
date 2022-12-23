package io.github.netmikey.testprocesses;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for the repeatable {@link TestProcess} annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE, METHOD })
public @interface TestProcesses {
    /**
     * The list of contained {@link TestProcess} annotations.
     * 
     * @return The annotation list.
     */
    TestProcess[] value();
}
