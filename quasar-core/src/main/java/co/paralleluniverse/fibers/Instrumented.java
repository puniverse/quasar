package co.paralleluniverse.fibers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as instrumented - for internal use only!
 * It must never be used in Java source code.
 * 
 * @author Matthias Mann
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented {
}
