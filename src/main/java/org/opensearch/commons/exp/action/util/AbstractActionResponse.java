package org.opensearch.commons.exp.action.util;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.commons.exp.ReaderWriter;
import org.opensearch.commons.exp.model.util.ToXContentModel;

import java.io.IOException;

public abstract class AbstractActionResponse<T extends AbstractActionResponse<T>> extends ActionResponse implements ReaderWriter<T>, ToXContentModel {

    protected Class<T> actionClass;

    public AbstractActionResponse(final StreamInput input) throws IOException {
        super(input);
    }

    public AbstractActionResponse(final Class<T> actionClass) {
        this.actionClass = actionClass;
    }

    @Override
    public Class<T> objectClass() {
        return this.actionClass;
    }

}
