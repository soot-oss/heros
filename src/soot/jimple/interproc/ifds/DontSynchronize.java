package soot.jimple.interproc.ifds;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Target;

/**	Semantic annotation stating that the annotated field can remain unsynchronized.
 *  This annotation is meant as a structured comment only, and has no immediate effect. */
@Target(FIELD)
public @interface DontSynchronize{ String value() default ""; }