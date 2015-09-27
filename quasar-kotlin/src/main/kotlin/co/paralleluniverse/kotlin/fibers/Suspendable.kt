package co.paralleluniverse.kotlin.fibers

import kotlin.annotation.Retention
import kotlin.annotation.Target

@Retention // Defaults to RUNTIME
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CLASS,
        AnnotationTarget.FILE, AnnotationTarget.EXPRESSION)

annotation public class Suspendable