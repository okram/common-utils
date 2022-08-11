/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.alerting.model

import org.apache.logging.log4j.LogManager
import org.opensearch.action.ActionListener
import org.opensearch.action.admin.cluster.state.ClusterStateRequest
import org.opensearch.action.admin.cluster.state.ClusterStateResponse
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest
import org.opensearch.action.admin.indices.rollover.RolloverRequest
import org.opensearch.action.admin.indices.rollover.RolloverResponse
import org.opensearch.action.support.IndicesOptions
import org.opensearch.action.support.master.AcknowledgedResponse
import org.opensearch.client.Client
import org.opensearch.cluster.ClusterChangedEvent
import org.opensearch.cluster.ClusterStateListener
import org.opensearch.cluster.metadata.IndexMetadata
import org.opensearch.cluster.service.ClusterService
import org.opensearch.common.settings.Settings
import org.opensearch.common.unit.TimeValue
import org.opensearch.commons.alerting.model.AlertIndices.Companion.ALERT_HISTORY_WRITE_INDEX
import org.opensearch.commons.alerting.model.AlertIndices.Companion.ALERT_INDEX
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.ALERT_HISTORY_ENABLED
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.ALERT_HISTORY_INDEX_MAX_AGE
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.ALERT_HISTORY_MAX_DOCS
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.ALERT_HISTORY_RETENTION_PERIOD
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.ALERT_HISTORY_ROLLOVER_PERIOD
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.FINDING_HISTORY_ENABLED
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.FINDING_HISTORY_INDEX_MAX_AGE
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.FINDING_HISTORY_MAX_DOCS
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.FINDING_HISTORY_RETENTION_PERIOD
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.FINDING_HISTORY_ROLLOVER_PERIOD
import org.opensearch.commons.alerting.model.AlertingSettings.Companion.REQUEST_TIMEOUT
import org.opensearch.threadpool.Scheduler.Cancellable
import org.opensearch.threadpool.ThreadPool
import java.time.Instant

/**
 * Class to manage the creation and rollover of alert indices and alert history indices.  In progress alerts are stored
 * in [ALERT_INDEX].  Completed alerts are written to [ALERT_HISTORY_WRITE_INDEX] which is an alias that points at the
 * current index to which completed alerts are written. [ALERT_HISTORY_WRITE_INDEX] is periodically rolled over to a new
 * date based index. The frequency of rolling over indices is controlled by the `opendistro.alerting.alert_rollover_period` setting.
 *
 * These indexes are created when first used and are then rolled over every `alert_rollover_period`. The rollover is
 * initiated on the cluster manager node to ensure only a single node tries to roll it over.  Once we have a curator functionality
 * in Scheduled Jobs we can migrate to using that to rollover the index.
 */
// TODO: reafactor to make a generic version of this class for finding and alerts
class AlertIndices(
    settings: Settings,
    private val client: Client,
    private val threadPool: ThreadPool,
    private val clusterService: ClusterService
) : ClusterStateListener {

    init {
        clusterService.addListener(this)
        clusterService.clusterSettings.addSettingsUpdateConsumer(ALERT_HISTORY_ENABLED) { alertHistoryEnabled = it }
        clusterService.clusterSettings.addSettingsUpdateConsumer(ALERT_HISTORY_MAX_DOCS) { alertHistoryMaxDocs = it }
        clusterService.clusterSettings.addSettingsUpdateConsumer(ALERT_HISTORY_INDEX_MAX_AGE) {
            alertHistoryMaxAge = it
        }
        clusterService.clusterSettings.addSettingsUpdateConsumer(ALERT_HISTORY_ROLLOVER_PERIOD) {
            alertHistoryRolloverPeriod = it
            rescheduleAlertRollover()
        }
        clusterService.clusterSettings.addSettingsUpdateConsumer(ALERT_HISTORY_RETENTION_PERIOD) {
            alertHistoryRetentionPeriod = it
        }
        clusterService.clusterSettings.addSettingsUpdateConsumer(REQUEST_TIMEOUT) { requestTimeout = it }

        clusterService.clusterSettings.addSettingsUpdateConsumer(FINDING_HISTORY_ENABLED) { findingHistoryEnabled = it }
        clusterService.clusterSettings.addSettingsUpdateConsumer(FINDING_HISTORY_MAX_DOCS) {
            findingHistoryMaxDocs = it
        }
        clusterService.clusterSettings.addSettingsUpdateConsumer(FINDING_HISTORY_INDEX_MAX_AGE) {
            findingHistoryMaxAge = it
        }
        clusterService.clusterSettings.addSettingsUpdateConsumer(FINDING_HISTORY_ROLLOVER_PERIOD) {
            findingHistoryRolloverPeriod = it
            rescheduleFindingRollover()
        }
        clusterService.clusterSettings.addSettingsUpdateConsumer(FINDING_HISTORY_RETENTION_PERIOD) {
            findingHistoryRetentionPeriod = it
        }
    }

    companion object {

        /** The in progress alert history index. */
        const val ALERT_INDEX = ".opendistro-alerting-alerts"

        /** The alias of the index in which to write alert history */
        const val ALERT_HISTORY_WRITE_INDEX = ".opendistro-alerting-alert-history-write"

        /** The alias of the index in which to write alert finding */
        const val FINDING_HISTORY_WRITE_INDEX = ".opensearch-alerting-finding-history-write"

        /** The index name pattern referring to all alert history indices */
        const val ALERT_HISTORY_ALL = ".opendistro-alerting-alert-history*"

        /** The index name pattern referring to all alert history indices */
        const val FINDING_HISTORY_ALL = ".opensearch-alerting-finding-history*"

        /** The index name pattern to create alert history indices */
        const val ALERT_HISTORY_INDEX_PATTERN = "<.opendistro-alerting-alert-history-{now/d}-1>"

        /** The index name pattern to create finding history indices */
        const val FINDING_HISTORY_INDEX_PATTERN = "<.opensearch-alerting-finding-history-{now/d}-1>"

        @JvmStatic
        fun alertMapping() =
            AlertIndices::class.java.getResource("alert_mapping.json").readText()

        @JvmStatic
        fun findingMapping() =
            AlertIndices::class.java.getResource("finding_mapping.json").readText()

        private val logger = LogManager.getLogger(AlertIndices::class.java)
    }

    @Volatile
    private var alertHistoryEnabled = AlertingSettings.ALERT_HISTORY_ENABLED.get(settings)

    @Volatile
    private var findingHistoryEnabled = AlertingSettings.FINDING_HISTORY_ENABLED.get(settings)

    @Volatile
    private var alertHistoryMaxDocs = AlertingSettings.ALERT_HISTORY_MAX_DOCS.get(settings)

    @Volatile
    private var findingHistoryMaxDocs = AlertingSettings.FINDING_HISTORY_MAX_DOCS.get(settings)

    @Volatile
    private var alertHistoryMaxAge = AlertingSettings.ALERT_HISTORY_INDEX_MAX_AGE.get(settings)

    @Volatile
    private var findingHistoryMaxAge = AlertingSettings.FINDING_HISTORY_INDEX_MAX_AGE.get(settings)

    @Volatile
    private var alertHistoryRolloverPeriod = AlertingSettings.ALERT_HISTORY_ROLLOVER_PERIOD.get(settings)

    @Volatile
    private var findingHistoryRolloverPeriod = AlertingSettings.FINDING_HISTORY_ROLLOVER_PERIOD.get(settings)

    @Volatile
    private var alertHistoryRetentionPeriod = AlertingSettings.ALERT_HISTORY_RETENTION_PERIOD.get(settings)

    @Volatile
    private var findingHistoryRetentionPeriod = AlertingSettings.FINDING_HISTORY_RETENTION_PERIOD.get(settings)

    @Volatile
    private var requestTimeout = AlertingSettings.REQUEST_TIMEOUT.get(settings)

    @Volatile
    private var isMaster = false

    // for JobsMonitor to report
    var lastRolloverTime: TimeValue? = null

    private var alertHistoryIndexInitialized: Boolean = false

    private var findingHistoryIndexInitialized: Boolean = false

    private var alertIndexInitialized: Boolean = false

    private var scheduledRollover: Cancellable? = null

    fun onMaster() {
        try {
            // try to rollover immediately as we might be restarting the cluster
            rolloverAlertHistoryIndex()
            rolloverFindingHistoryIndex()
            // schedule the next rollover for approx MAX_AGE later
            scheduledRollover = threadPool
                .scheduleWithFixedDelay(
                    { rolloverAndDeleteAlertHistoryIndices() },
                    alertHistoryRolloverPeriod,
                    executorName()
                )
            scheduledRollover = threadPool
                .scheduleWithFixedDelay(
                    { rolloverAndDeleteFindingHistoryIndices() },
                    findingHistoryRolloverPeriod,
                    executorName()
                )
        } catch (e: Exception) {
            // This should be run on cluster startup
            logger.error(
                "Error creating alert/finding indices. " +
                        "Alerts/Findings can't be recorded until master node is restarted.",
                e
            )
        }
    }

    fun offMaster() {
        scheduledRollover?.cancel()
    }

    private fun executorName(): String {
        return ThreadPool.Names.MANAGEMENT
    }

    override fun clusterChanged(event: ClusterChangedEvent) {
        // Instead of using a LocalNodeMasterListener to track master changes, this service will
        // track them here to avoid conditions where master listener events run after other
        // listeners that depend on what happened in the master listener
        if (this.isMaster != event.localNodeMaster()) {
            this.isMaster = event.localNodeMaster()
            if (this.isMaster) {
                onMaster()
            } else {
                offMaster()
            }
        }

        // if the indexes have been deleted they need to be reinitialized
        alertIndexInitialized = event.state().routingTable().hasIndex(ALERT_INDEX)
        alertHistoryIndexInitialized = event.state().metadata().hasAlias(ALERT_HISTORY_WRITE_INDEX)
        findingHistoryIndexInitialized = event.state().metadata().hasAlias(FINDING_HISTORY_WRITE_INDEX)
    }

    private fun rescheduleAlertRollover() {
        if (clusterService.state().nodes.isLocalNodeElectedMaster) {
            scheduledRollover?.cancel()
            scheduledRollover = threadPool
                .scheduleWithFixedDelay(
                    { rolloverAndDeleteAlertHistoryIndices() },
                    alertHistoryRolloverPeriod,
                    executorName()
                )
        }
    }

    private fun rescheduleFindingRollover() {
        if (clusterService.state().nodes.isLocalNodeElectedMaster) {
            scheduledRollover?.cancel()
            scheduledRollover = threadPool
                .scheduleWithFixedDelay(
                    { rolloverAndDeleteFindingHistoryIndices() },
                    findingHistoryRolloverPeriod,
                    executorName()
                )
        }
    }

    private fun rolloverAndDeleteAlertHistoryIndices() {
        if (alertHistoryEnabled) rolloverAlertHistoryIndex()
        deleteOldIndices("History", ALERT_HISTORY_ALL)
    }

    private fun rolloverAndDeleteFindingHistoryIndices() {
        if (findingHistoryEnabled) rolloverFindingHistoryIndex()
        deleteOldIndices("Finding", FINDING_HISTORY_ALL)
    }

    private fun rolloverIndex(
        initialized: Boolean,
        index: String,
        pattern: String,
        map: String,
        docsCondition: Long,
        ageCondition: TimeValue,
        writeIndex: String
    ) {
        if (!initialized) {
            return
        }

        // We have to pass null for newIndexName in order to get Elastic to increment the index count.
        val request = RolloverRequest(index, null)
        request.createIndexRequest.index(pattern)
            .mapping(map)
            .settings(Settings.builder().put("index.hidden", true).build())
        request.addMaxIndexDocsCondition(docsCondition)
        request.addMaxIndexAgeCondition(ageCondition)
        client.admin().indices().rolloverIndex(
            request,
            object : ActionListener<RolloverResponse> {
                override fun onResponse(response: RolloverResponse) {
                    if (!response.isRolledOver) {
                        logger.info("$writeIndex not rolled over. Conditions were: ${response.conditionStatus}")
                    } else {
                        lastRolloverTime = TimeValue.timeValueMillis(threadPool.absoluteTimeInMillis())
                    }
                }

                override fun onFailure(e: Exception) {
                    logger.error("$writeIndex not roll over failed.")
                }
            }
        )
    }

    private fun rolloverAlertHistoryIndex() {
        rolloverIndex(
            alertHistoryIndexInitialized, ALERT_HISTORY_WRITE_INDEX,
            ALERT_HISTORY_INDEX_PATTERN, alertMapping(),
            alertHistoryMaxDocs, alertHistoryMaxAge, ALERT_HISTORY_WRITE_INDEX
        )
    }

    private fun rolloverFindingHistoryIndex() {
        rolloverIndex(
            findingHistoryIndexInitialized, FINDING_HISTORY_WRITE_INDEX,
            FINDING_HISTORY_INDEX_PATTERN, findingMapping(),
            findingHistoryMaxDocs, findingHistoryMaxAge, FINDING_HISTORY_WRITE_INDEX
        )
    }

    private fun deleteOldIndices(tag: String, indices: String) {
        logger.error("info deleteOldIndices")
        val clusterStateRequest = ClusterStateRequest()
            .clear()
            .indices(indices)
            .metadata(true)
            .local(true)
            .indicesOptions(IndicesOptions.strictExpand())
        client.admin().cluster().state(
            clusterStateRequest,
            object : ActionListener<ClusterStateResponse> {
                override fun onResponse(clusterStateResponse: ClusterStateResponse) {
                    if (!clusterStateResponse.state.metadata.indices.isEmpty) {
                        val indicesToDelete = getIndicesToDelete(clusterStateResponse)
                        logger.info("Deleting old $tag indices viz $indicesToDelete")
                        deleteAllOldHistoryIndices(indicesToDelete)
                    } else {
                        logger.info("No Old $tag Indices to delete")
                    }
                }

                override fun onFailure(e: Exception) {
                    logger.error("Error fetching cluster state")
                }
            }
        )
    }

    private fun getIndicesToDelete(clusterStateResponse: ClusterStateResponse): List<String> {
        val indicesToDelete = mutableListOf<String>()
        for (entry in clusterStateResponse.state.metadata.indices) {
            val indexMetaData = entry.value
            getHistoryIndexToDelete(
                indexMetaData,
                alertHistoryRetentionPeriod.millis,
                ALERT_HISTORY_WRITE_INDEX,
                alertHistoryEnabled
            )
                ?.let { indicesToDelete.add(it) }
            getHistoryIndexToDelete(
                indexMetaData,
                findingHistoryRetentionPeriod.millis,
                FINDING_HISTORY_WRITE_INDEX,
                findingHistoryEnabled
            )
                ?.let { indicesToDelete.add(it) }
        }
        return indicesToDelete
    }

    private fun getHistoryIndexToDelete(
        indexMetadata: IndexMetadata,
        retentionPeriodMillis: Long,
        writeIndex: String,
        historyEnabled: Boolean
    ): String? {
        val creationTime = indexMetadata.creationDate
        if ((Instant.now().toEpochMilli() - creationTime) > retentionPeriodMillis) {
            val alias = indexMetadata.aliases.firstOrNull { writeIndex == it.value.alias }
            if (alias != null) {
                if (historyEnabled) {
                    // If the index has the write alias and history is enabled, don't delete the index
                    return null
                } else if (writeIndex == ALERT_HISTORY_WRITE_INDEX) {
                    // Otherwise reset alertHistoryIndexInitialized since index will be deleted
                    alertHistoryIndexInitialized = false
                } else if (writeIndex == FINDING_HISTORY_WRITE_INDEX) {
                    // Otherwise reset findingHistoryIndexInitialized since index will be deleted
                    findingHistoryIndexInitialized = false
                }
            }

            return indexMetadata.index.name
        }
        return null
    }

    private fun deleteAllOldHistoryIndices(indicesToDelete: List<String>) {
        if (indicesToDelete.isNotEmpty()) {
            val deleteIndexRequest = DeleteIndexRequest(*indicesToDelete.toTypedArray())
            client.admin().indices().delete(
                deleteIndexRequest,
                object : ActionListener<AcknowledgedResponse> {
                    override fun onResponse(deleteIndicesResponse: AcknowledgedResponse) {
                        if (!deleteIndicesResponse.isAcknowledged) {
                            logger.error(
                                "Could not delete one or more Alerting/Finding history indices: $indicesToDelete. Retrying one by one."
                            )
                            deleteOldHistoryIndex(indicesToDelete)
                        }
                    }

                    override fun onFailure(e: Exception) {
                        logger.error("Delete for Alerting/Finding History Indices $indicesToDelete Failed. Retrying one By one.")
                        deleteOldHistoryIndex(indicesToDelete)
                    }
                }
            )
        }
    }

    private fun deleteOldHistoryIndex(indicesToDelete: List<String>) {
        for (index in indicesToDelete) {
            val singleDeleteRequest = DeleteIndexRequest(*indicesToDelete.toTypedArray())
            client.admin().indices().delete(
                singleDeleteRequest,
                object : ActionListener<AcknowledgedResponse> {
                    override fun onResponse(acknowledgedResponse: AcknowledgedResponse?) {
                        if (acknowledgedResponse != null) {
                            if (!acknowledgedResponse.isAcknowledged) {
                                logger.error("Could not delete one or more Alerting/Finding history indices: $index")
                            }
                        }
                    }

                    override fun onFailure(e: Exception) {
                        logger.debug("Exception ${e.message} while deleting the index $index")
                    }
                }
            )
        }
    }
}
