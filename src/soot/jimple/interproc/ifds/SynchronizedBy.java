package soot.jimple.interproc.ifds;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Target;

/**	Semantic annotation that the annotated field is synchronized.
 *  This annotation is meant as a structured comment only, and has no immediate effect. */
@Target(FIELD)
public @interface SynchronizedBy{ String value() default ""; }