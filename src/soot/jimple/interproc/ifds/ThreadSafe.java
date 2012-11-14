package soot.jimple.interproc.ifds;

/**
 * This annotation tells that the class was designed to be used by multiple threads, with concurrent updates. 
 */
public @interface ThreadSafe {
	
	String value() default "";

}
