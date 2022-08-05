/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.alerting.model

import org.opensearch.commons.core.model.DocLevelQuery

data class DocumentExecutionContext(
        val queries: List<DocLevelQuery>,
        val lastRunContext: Map<String, Any>,
        val updatedLastRunContext: Map<String, Any>
)
