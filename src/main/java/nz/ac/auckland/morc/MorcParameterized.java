package nz.ac.auckland.morc;

import nz.ac.auckland.morc.specification.OrchestratedTestSpecification;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A heavily modified version of JUnit's Parameterized class that
 * assumes there's a public static void configure() method available
 * to configure a set of test specifications which will then be retrieved
 * using the getSpecifications method of MorcTest
 * <p/>
 * Any copyright for similar code will be under the EPL license for JUnit
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class MorcParameterized extends Suite {

    private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {
        private final OrchestratedTestSpecification specification;
        private final String name;

        TestClassRunnerForParameters(Class<?> type,
                                     OrchestratedTestSpecification specification,
                                     String name) throws InitializationError {
            super(type);
            this.specification = specification;
            this.name = name;
        }

        @Override
        public Object createTest() throws Exception {
            Constructor co = getTestClass().getJavaClass().getConstructor();
            MorcTest test = (MorcTest) co.newInstance();
            test.setSpecification(specification);

            return test;
        }

        @Override
        protected String getName() {
            return this.name;
        }

        @Override
        protected String testName(FrameworkMethod method) {
            return method.getName() + getName();
        }

        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
        }

        @Override
        protected Statement classBlock(RunNotifier notifier) {
            return childrenInvoker(notifier);
        }

        @Override
        protected Annotation[] getRunnerAnnotations() {
            return new Annotation[0];
        }
    }

    private final ArrayList<Runner> runners = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public MorcParameterized(Class<? extends MorcTestBuilder> klass) throws Throwable {
        super(klass, Collections.<Runner>emptyList());

        Method getSpecifications = MorcTestBuilder.class.getDeclaredMethod("getSpecifications");
        getSpecifications.setAccessible(true);
        List<OrchestratedTestSpecification> specifications = (List) getSpecifications.invoke(klass.newInstance());

        createRunnersForParameters(specifications);
    }

    public MorcParameterized(MorcTestBuilder builder) throws Throwable {
        super(AnonymousMorc.class,Collections.<Runner>emptyList());
        createRunnersForParameters(builder.getSpecifications());
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    private void createRunnersForParameters(List<OrchestratedTestSpecification> specifications) throws InitializationError {
        int i = 0;
        for (OrchestratedTestSpecification specification : specifications) {
            String name = String.format("%d: %s", i, specification.getDescription());
            TestClassRunnerForParameters runner = new TestClassRunnerForParameters(
                    getTestClass().getJavaClass(), specification,
                    name);
            runners.add(runner);
            i++;
        }
    }
}

/*
JUnit sigh...
 */
class AnonymousMorc extends MorcTestBuilder {
    public AnonymousMorc() {

    }

    public void configure() {

    }
}
