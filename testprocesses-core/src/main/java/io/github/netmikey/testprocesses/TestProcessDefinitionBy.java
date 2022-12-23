package io.github.netmikey.testprocesses;

import java.util.Optional;

/**
 * Some parts of the TestProcesses API allow to target a TestProcess by multiple
 * means. This class allows to express how a test process should be addressed.
 * <p>
 * It can be addressed by:
 * <ul>
 * <li><b>its concrete {@link TestProcessDefinition} type</b>
 * <p>
 * In this case, a single Spring Bean of this type must exist in the Spring test
 * context.</li>
 * <li><b>its {@link TestProcessDefinition} instance</b>
 * <p>
 * This can be any {@link TestProcessDefinition} instance, including Spring
 * beans and instances defined locally on the test. The
 * {@link TestProcessesRegistry} will identify Spring beans by object
 * identity.</li>
 * <li><b>the name of its {@link TestProcessDefinition} instance's Spring
 * bean</b>
 * <p>
 * A bean with this name must exist in the Spring test context.</li>
 * </ul>
 * 
 * @param <T>
 *            The concrete {@link TestProcessDefinition} subtype.
 */
public class TestProcessDefinitionBy<T extends TestProcessDefinition> {

    private Optional<Class<T>> clazz = Optional.empty();

    private Optional<T> instance = Optional.empty();

    private Optional<String> beanName = Optional.empty();

    /**
     * Address a test process definition by its concrete type.
     * 
     * @param testProcessDefinitionClass
     *            The concrete {@link TestProcessDefinition} implementation
     *            class.
     * @param <C>
     *            The concrete {@link TestProcessDefinition} type.
     * @return The {@link TestProcessDefinitionBy} reference to the clazz.
     */
    public static <C extends TestProcessDefinition> TestProcessDefinitionBy<C> clazz(
        Class<C> testProcessDefinitionClass) {
        TestProcessDefinitionBy<C> result = new TestProcessDefinitionBy<>();
        result.clazz = Optional.of(testProcessDefinitionClass);
        return result;
    }

    /**
     * Provide the target {@link TestProcessDefinition} instance directly.
     * 
     * @param testProcessDefinition
     *            The {@link TestProcessDefinition} instance.
     * @param <I>
     *            The concrete {@link TestProcessDefinition} type.
     * @return The {@link TestProcessDefinitionBy} reference to the instance.
     */
    public static <I extends TestProcessDefinition> TestProcessDefinitionBy<I> instance(I testProcessDefinition) {
        TestProcessDefinitionBy<I> result = new TestProcessDefinitionBy<>();
        result.instance = Optional.of(testProcessDefinition);
        return result;
    }

    /**
     * Address a test process definition by its Spring bean name.
     * 
     * @param testProcessDefinitionBeanName
     *            The Spring bean name of the {@link TestProcessDefinition}
     *            bean.
     * @return The {@link TestProcessDefinitionBy} reference to the Spring bean.
     */
    public static TestProcessDefinitionBy<? extends TestProcessDefinition> beanName(
        String testProcessDefinitionBeanName) {
        TestProcessDefinitionBy<? extends TestProcessDefinition> result = new TestProcessDefinitionBy<>();
        result.beanName = Optional.of(testProcessDefinitionBeanName);
        return result;
    }

    /**
     * Get the clazz.
     * 
     * @return Returns the clazz.
     */
    public Optional<Class<T>> getClazz() {
        return clazz;
    }

    /**
     * Get the instance.
     * 
     * @return Returns the instance.
     */
    public Optional<T> getInstance() {
        return instance;
    }

    /**
     * Get the beanName.
     * 
     * @return Returns the beanName.
     */
    public Optional<String> getBeanName() {
        return beanName;
    }

}
