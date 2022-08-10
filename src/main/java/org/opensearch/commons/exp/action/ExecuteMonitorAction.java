package org.opensearch.commons.exp.action;

import org.opensearch.action.ActionType;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.commons.exp.action.util.AbstractActionRequest;
import org.opensearch.commons.exp.action.util.AbstractActionResponse;
import org.opensearch.commons.exp.model.Monitor;
import org.opensearch.commons.exp.model.MonitorRunResult;

import java.io.IOException;

public class ExecuteMonitorAction extends ActionType<ExecuteMonitorAction.ExecuteMonitorResponse> {

    public static ExecuteMonitorAction INSTANCE = new ExecuteMonitorAction();
    public static String NAME = "cluster:admin/opendistro/alerting/monitor/execute";

    private ExecuteMonitorAction() {
        super(NAME, ExecuteMonitorResponse::new);
    }

    public static class ExecuteMonitorRequest extends AbstractActionRequest<ExecuteMonitorRequest> {

        public boolean dryRun;
        public TimeValue requestEnd;
        public String monitorId;
        public Monitor monitor;

        public ExecuteMonitorRequest() {
            super(ExecuteMonitorRequest.class);
        }

        public ExecuteMonitorRequest(final boolean dryRun, final TimeValue requestEnd, final String monitorId, final Monitor monitor) {
            this();
            this.dryRun = dryRun;
            this.requestEnd = requestEnd;
            this.monitorId = monitorId;
            this.monitor = monitor;
        }

    }

    public static class ExecuteMonitorResponse extends AbstractActionResponse<ExecuteMonitorResponse> {

        public MonitorRunResult monitorRunResult;

        public ExecuteMonitorResponse(final MonitorRunResult monitorRunResult) {
            super(ExecuteMonitorResponse.class);
            this.monitorRunResult = monitorRunResult;
        }

        public ExecuteMonitorResponse(final StreamInput input) throws IOException {
            super(input);
        }

    }


}
