package org.opensearch.commons.alerting.util

enum class DestinationType(val value: String) {
    CHIME("chime"),
    SLACK("slack"),
    CUSTOM_WEBHOOK("custom_webhook"),
    EMAIL("email"),
    TEST_ACTION("test_action");

    override fun toString(): String {
        return value
    }
}
