package org.fiftieshousewife.cleancode.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Target({METHOD, TYPE, CONSTRUCTOR, PACKAGE})
@Retention(SOURCE)
@Documented
@Repeatable(SuppressCleanCode.List.class)
public @interface SuppressCleanCode {
    HeuristicCode[] value();
    String reason();
    String until() default "";

    @Target({METHOD, TYPE, CONSTRUCTOR, PACKAGE})
    @Retention(SOURCE)
    @Documented
    @interface List {
        SuppressCleanCode[] value();
    }
}
