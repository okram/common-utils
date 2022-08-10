package org.opensearch.commons.ext;

import org.opensearch.commons.exp.model.Input;
import org.opensearch.commons.exp.model.Monitor;
import org.opensearch.commons.exp.model.MonitorRunResult;
import org.opensearch.commons.exp.model.Query;

import java.util.List;

public class ModelTestRegistry {

    // TODO: make this dynamically loaded via META-INF/services where entires packages can be specified
    public static final List<Class<?>> TEST_MODELS = List.of(
            Query.class,
            Input.class,
            Monitor.class,
            MonitorRunResult.class);
}
