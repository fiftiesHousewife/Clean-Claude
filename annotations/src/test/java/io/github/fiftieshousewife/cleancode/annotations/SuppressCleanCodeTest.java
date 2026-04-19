package io.github.fiftieshousewife.cleancode.annotations;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.*;

class SuppressCleanCodeTest {

    @Test
    void annotationIsRetentionSource() {
        Retention retention = SuppressCleanCode.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.SOURCE, retention.value());
    }

    @Test
    void annotationTargetsMethodTypeConstructorAndPackage() {
        final Target target = SuppressCleanCode.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(
                new ElementType[]{ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.PACKAGE},
                target.value());
    }

    @Test
    void annotationIsRepeatable() {
        Repeatable repeatable = SuppressCleanCode.class.getAnnotation(Repeatable.class);
        assertNotNull(repeatable);
        assertEquals(SuppressCleanCode.List.class, repeatable.value());
    }

    @Test
    void reasonHasNoDefault() throws NoSuchMethodException {
        Object defaultValue = SuppressCleanCode.class.getMethod("reason").getDefaultValue();
        assertNull(defaultValue, "reason() must not have a default — it is mandatory");
    }

    @Test
    void untilDefaultsToEmpty() throws NoSuchMethodException {
        Object defaultValue = SuppressCleanCode.class.getMethod("until").getDefaultValue();
        assertEquals("", defaultValue);
    }

    @Test
    void valueAcceptsHeuristicCodeArray() throws NoSuchMethodException {
        Class<?> returnType = SuppressCleanCode.class.getMethod("value").getReturnType();
        assertEquals(HeuristicCode[].class, returnType);
    }
}
