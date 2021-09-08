package org.distributedlock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method with DistributedLock annotation would only be executed if and only if the lock is acquired. If the lock
 * isn't acquired then the method won't be called. Moreover, the method won't be executed anywhere until the duration
 * for the lock expires.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * Returns the name of the lock, it should be unique and will be treated as id
     */
    String name();

    /**
     * Returns the owner of the lock
     */
    String owner() default "";

    /**
     * Returns the duration for the lock in seconds, should be a positive number.
     */
    long lockFor();
}