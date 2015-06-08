package co.paralleluniverse.fibers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or a method as instrumented - for internal use only!
 * It must never be used in Java source code.
 * 
 * It optionally contains the coordinates within a method of instrumented
 * call sites (for verification during development, if enabled).
 *
 * @author Matthias Mann
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented {
    // Relevant only for methods
    int[] suspendableCallsites() default {};
}
