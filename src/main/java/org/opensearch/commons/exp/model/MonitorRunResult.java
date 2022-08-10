package org.opensearch.commons.exp.model;

import org.opensearch.commons.exp.model.util.AbstractModel;

import java.time.Instant;

public class MonitorRunResult extends AbstractModel<MonitorRunResult> {

    public String monitorName;
    public Instant periodStart;
    public Instant periodEnd;
    public Exception error;

    public MonitorRunResult() {
        super(MonitorRunResult.class);
    }

    public MonitorRunResult(final String monitorName, final Instant periodStart, final Instant periodEnd, final Exception error) {
        this();
        this.monitorName = monitorName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.error = error;
    }


}
