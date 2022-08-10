package org.opensearch.commons.exp;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.commons.exp.model.util.ModelSerializer;

import java.io.IOException;

public interface ReaderWriter<T> extends Writeable.Writer<T>, Writeable.Reader<T>, Writeable {

    Class<T> objectClass();

    @Override
    default void writeTo(final StreamOutput output) throws IOException {
        ModelSerializer.write(output, this);
    }

    @Override
    default void write(final StreamOutput output, final T model) throws IOException {
        ModelSerializer.write(output, model);
    }

    @Override
    default T read(final StreamInput input) throws IOException {
        return ModelSerializer.read(input, this.objectClass());
    }
}
