package org.opensearch.commons.exp.action.util;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.commons.exp.ReaderWriter;

public abstract class AbstractActionRequest<T extends AbstractActionRequest<T>> extends ActionRequest implements ReaderWriter<T> {

    protected Class<T> actionClass;

    protected AbstractActionRequest(final Class<T> actionClass) {
        this.actionClass = actionClass;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public Class<T> objectClass() {
        return this.actionClass;
    }
}