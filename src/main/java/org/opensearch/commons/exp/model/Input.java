/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.exp.model;

import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.commons.exp.model.util.AbstractModel;
import org.opensearch.commons.exp.model.util.ToXContentModel;

import java.util.List;

public class Input extends AbstractModel<Input> {

    // TODO: this doesn't need to be a static field, simply use ToXContentModel.createRegistryEntry() at reference location
    public static NamedXContentRegistry.Entry XCONTENT_REGISTRY = ToXContentModel.createRegistryEntry(Input.class);

    public String description;
    public List<String> indices;
    public List<Query> queries;

    public Input() {
        super(Input.class);
    }

    public Input(final String description, final List<String> indices, final List<Query> queries) {
        this();
        this.description = description;
        this.indices = indices;
        this.queries = queries;
    }
}
