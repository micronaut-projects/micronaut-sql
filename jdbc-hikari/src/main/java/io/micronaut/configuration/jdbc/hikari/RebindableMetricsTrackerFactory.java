/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.jdbc.hikari;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

/**
 * The metrics tracker factory, and the tracker, of a Hikari pool in development mode, which retains the pool across
 * a restart. Hikari accepts a meter registry, or a tracker factory, once per pool, so the pool is given this tracker,
 * which reports to the registry of the application context that currently uses the pool: each context binds its
 * registry as it is served the pool, and unbinds it as it stops. Binding or unbinding a registry removes the meters
 * of the pool from the previous registry, so the pool neither keeps that registry nor reports to it.
 * <p>
 * Outside development mode a pool is given the registry itself, with no indirection on the path of a connection.
 *
 * @since 7.3.0
 */
@Internal
final class RebindableMetricsTrackerFactory implements MetricsTrackerFactory, IMetricsTracker {

    private static final IMetricsTracker NONE = new IMetricsTracker() { };

    private volatile IMetricsTracker tracker = NONE;
    private @Nullable MeterRegistry registry;
    private @Nullable String poolName;
    private @Nullable PoolStats poolStats;

    /**
     * @param registry The registry the pool reports to first
     */
    RebindableMetricsTrackerFactory(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public synchronized IMetricsTracker create(String poolName, PoolStats poolStats) {
        this.poolName = poolName;
        this.poolStats = poolStats;
        replaceTracker();
        return this;
    }

    /**
     * Reports to the given registry from now on, and removes the meters of the pool from the previous one.
     *
     * @param registry The registry of the application context that uses the pool
     */
    synchronized void bind(MeterRegistry registry) {
        if (this.registry != registry) {
            this.registry = registry;
            replaceTracker();
        }
    }

    /**
     * Stops reporting to the given registry, and removes the meters of the pool from it, unless the pool already
     * reports to another.
     *
     * @param registry The registry of an application context that stops
     */
    synchronized void unbind(MeterRegistry registry) {
        if (this.registry == registry) {
            this.registry = null;
            replaceTracker();
        }
    }

    /**
     * @return The registry the pool reports to, if any
     */
    synchronized @Nullable MeterRegistry registry() {
        return registry;
    }

    private void replaceTracker() {
        IMetricsTracker previous = tracker;
        tracker = NONE;
        previous.close();
        if (registry != null && poolStats != null) {
            tracker = new MicrometerMetricsTrackerFactory(registry).create(poolName, poolStats);
        }
    }

    @Override
    public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
        tracker.recordConnectionCreatedMillis(connectionCreatedMillis);
    }

    @Override
    public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
        tracker.recordConnectionAcquiredNanos(elapsedAcquiredNanos);
    }

    @Override
    public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
        tracker.recordConnectionUsageMillis(elapsedBorrowedMillis);
    }

    @Override
    public void recordConnectionTimeout() {
        tracker.recordConnectionTimeout();
    }

    /**
     * Called as the pool shuts down: removes its meters from the registry it reports to.
     */
    @Override
    public synchronized void close() {
        registry = null;
        replaceTracker();
    }
}
