package org.distributedlock;

import org.apache.commons.lang3.tuple.Pair;

public interface LockProvider {

    /**
     * Returns true if it's able to acquire the lock specified by lockConfig with updated details
     */
    Pair<Boolean, LockConfiguration> tryLock(LockConfiguration lockConfiguration);

    /**
     * Release the lock
     */
    void release(LockConfiguration lockConfiguration);
}