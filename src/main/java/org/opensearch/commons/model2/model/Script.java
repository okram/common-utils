/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.model2.model;

import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.commons.model2.AbstractModel;
import org.opensearch.commons.model2.ToXContentModel;

public class Script extends AbstractModel {

    public static NamedXContentRegistry.Entry XCONTENT_REGISTRY = ToXContentModel.createRegistryEntry(Script.class);

    public String source;
    public String lang;

    public Script() {
        // for serialization
    }

    public Script(final String source, final String lang) {
        this.source = source;
        this.lang = lang;
    }
}

