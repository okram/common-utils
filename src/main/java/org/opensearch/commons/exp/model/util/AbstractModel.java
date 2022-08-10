/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.exp.model.util;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.commons.exp.ReaderWriter;

import java.io.IOException;

public abstract class AbstractModel<T extends AbstractModel<T>> implements ToXContentModel, ReaderWriter<T> {

    protected Class<T> modelClass;

    public AbstractModel(final Class<T> modelClass) {
        this.modelClass = modelClass;
    }

    @Override
    public int hashCode() {
        return ModelSerializer.getHashCode(this);
    }

    @Override
    public boolean equals(final Object other) {
        return ModelSerializer.areEquals(this, other);
    }

    @Override
    public String toString() {
        return ModelSerializer.getString(this);
    }

    @Override
    public Class<T> objectClass() {
        return this.modelClass;
    }

    @Override
    public void writeTo(final StreamOutput output) throws IOException {
        ModelSerializer.write(output, this);
    }

    @Override
    public void write(final StreamOutput output, final T model) throws IOException {
        ModelSerializer.write(output, model);
    }

    @Override
    public T read(final StreamInput input) throws IOException {
        return ModelSerializer.read(input, this.modelClass);
    }
}
