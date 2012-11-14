package soot.jimple.interproc.ifds;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Target;

/**	Semantic annotation stating that the annotated field must be synchronized.
 *  This annotation is meant as a structured comment only, and has no immediate effect. */
@Target(FIELD)
public @interface MustSynchronize{ String value() default ""; }