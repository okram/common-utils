/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.exp.model;

import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.commons.exp.model.util.AbstractModel;
import org.opensearch.commons.exp.model.util.ToXContentModel;

import java.util.List;

public class Query extends AbstractModel<Query> {

    // TODO: this doesn't need to be a static field, simply use ToXContentModel.createRegistryEntry() at reference location
    public static NamedXContentRegistry.Entry XCONTENT_REGISTRY = ToXContentModel.createRegistryEntry(Query.class);

    public String id;
    public String name;
    public String query;
    public List<String> tags;

    public Query() {
        super(Query.class);
    }

    public Query(final String id, final String name, final String query, final List<String> tags) {
        this();
        this.id = id;
        this.name = name;
        this.query = query;
        this.tags = tags;
    }

}
