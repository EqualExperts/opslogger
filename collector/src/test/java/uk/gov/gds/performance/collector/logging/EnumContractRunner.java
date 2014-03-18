package uk.gov.gds.performance.collector.logging;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumContractRunner extends Suite {

    private static final List<Runner> NO_RUNNERS = Collections.emptyList();

    private final List<Runner> runners = new ArrayList<>();

    public EnumContractRunner(Class<?> klass) throws Throwable {
        super(klass, NO_RUNNERS);

        Class<? extends Enum<?>> enumClass = getEnumType(klass);
        for(Enum<?> e : enumClass.getEnumConstants()) {
            runners.add(new TestClassRunnerForEnum(klass, e));
        }
    }

    <T extends java.lang.Enum<T>> Class<T> getEnumType(Class<?> klass) {
        EnumToTest annotation = klass.getAnnotation(EnumToTest.class);

        if (annotation == null) {
            throw new AssertionError("Test class must be annotated with @EnumToTest");
        }
        Class<?> potentialEnumClass = annotation.value();
        if (!potentialEnumClass.isEnum()) {
            throw new AssertionError("@EnumToTest value() must be an Enum");
        }

        @SuppressWarnings("unchecked") //checked above
        Class<T> enumClass = (Class<T>) potentialEnumClass;
        return enumClass;
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    private class TestClassRunnerForEnum extends BlockJUnit4ClassRunner {
        private final Enum<?> enumValue;

        public TestClassRunnerForEnum(Class<?> klass, Enum<?> enumValue) throws InitializationError {
            super(klass);
            this.enumValue = enumValue;
        }

        @Override
        protected Object createTest() throws Exception {
            Object test = getTestClass().getOnlyConstructor().newInstance();
            getTestClass().getAnnotatedFields(EnumField.class).get(0).getField().set(test, enumValue);
            return test;
        }

        @Override
        protected String getName() {
            return enumValue.name();
        }

        @Override
        protected String testName(FrameworkMethod method) {
            return getName() + "_" + method.getName();
        }

        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateZeroArgConstructor(errors);
        }

        @Override
        protected void validateFields(List<Throwable> errors) {
            super.validateFields(errors);
            List<FrameworkField> fields = getTestClass().getAnnotatedFields(EnumField.class);
            if (fields.size() != 1) {
                errors.add(new Exception("Need exactly one field annotated with @EnumField"));
            }
            if (!fields.get(0).isPublic()) {
                fields.get(0).getField().setAccessible(true);
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface EnumToTest {
        public Class value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface EnumField {

    }
}