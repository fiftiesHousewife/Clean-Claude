package org.fiftieshousewife.cleancode.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(SuppressCleanCode.List.class)
public @interface SuppressCleanCode {
    HeuristicCode[] value();
    String reason();
    String until() default "";

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface List {
        SuppressCleanCode[] value();
    }
}
