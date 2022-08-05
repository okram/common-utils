/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.alerting.triggercondition.resolvers

import org.opensearch.commons.core.model.DocLevelQuery


interface TriggerExpressionResolver {
    fun evaluate(queryToDocIds: Map<DocLevelQuery, Set<String>>): Set<String>
}
