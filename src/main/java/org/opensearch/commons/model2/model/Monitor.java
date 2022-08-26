/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.model2.model;

import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.commons.model2.AbstractModel;
import org.opensearch.commons.model2.ToXContentModel;

import java.util.List;

public class Monitor extends AbstractModel {

    public static NamedXContentRegistry.Entry XCONTENT_REGISTRY = ToXContentModel.createRegistryEntry(Monitor.class);

    public String id;
    public String monitor_type;
    public long version;
    public Schedule schedule;
    public String name;
    public long interval;
    public String unit;
    public List<Input> inputs;

    public Monitor() {
        // for serialization
    }

    public Monitor(final String id, final String monitor_type, final long version, final Schedule schedule, final String name, final long interval, final String unit, final List<Input> inputs) {
        this.id = id;
        this.monitor_type = monitor_type;
        this.version = version;
        this.schedule = schedule;
        this.name = name;
        this.interval = interval;
        this.unit = unit;
        this.inputs = inputs;
    }
}