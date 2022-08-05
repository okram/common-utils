package org.opensearch.commons.alerting.action

import org.opensearch.action.ActionType

class ExecuteMonitorAction private constructor() : ActionType<ExecuteMonitorResponse>(NAME, ::ExecuteMonitorResponse) {
    companion object {
        val INSTANCE = ExecuteMonitorAction()
        const val NAME = "cluster:admin/opendistro/alerting/monitor/execute"
    }
}