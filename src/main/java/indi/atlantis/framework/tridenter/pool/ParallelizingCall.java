package indi.atlantis.framework.tridenter.pool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * ParallelizingCall
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParallelizingCall {

	String value();

	Class<? extends Parallelization> usingParallelization() default DefaultParallelization.class;

	Class<? extends Throwable>[] ignoreFor() default Exception.class;

}
