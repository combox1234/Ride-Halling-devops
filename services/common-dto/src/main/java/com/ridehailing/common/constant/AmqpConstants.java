package com.ridehailing.common.constant;

public final class AmqpConstants {

    private AmqpConstants() {
    }

    // Exchanges
    public static final String RIDE_TOPIC_EXCHANGE = "ride.topic.exchange";
    public static final String BILLING_DIRECT_EXCHANGE = "billing.direct.exchange";
    public static final String DLX_TOPIC_EXCHANGE = "dlx.topic.exchange";

    // Queues
    public static final String QUEUE_RIDE_MATCHING = "q.ride.matching";
    public static final String QUEUE_RIDE_NOTIFICATION = "q.ride.notification";
    public static final String QUEUE_RIDE_BILLING = "q.ride.billing";
    public static final String QUEUE_RIDE_REPORTING = "q.ride.reporting";
    public static final String QUEUE_BILLING_REPORTING = "q.billing.reporting";
    public static final String QUEUE_DEAD_LETTER = "q.dead-letter";

    // Routing Keys
    public static final String ROUTING_KEY_RIDE_REQUESTED = "ride.requested";
    public static final String ROUTING_KEY_RIDE_MATCHED = "ride.matched";
    public static final String ROUTING_KEY_RIDE_STARTED = "ride.started";
    public static final String ROUTING_KEY_RIDE_COMPLETED = "ride.completed";
    public static final String ROUTING_KEY_RIDE_CANCELLED = "ride.cancelled";
    public static final String ROUTING_KEY_RIDE_ALL = "ride.*";
    public static final String ROUTING_KEY_BILLING_PROCESSED = "billing.processed";
    public static final String ROUTING_KEY_BILLING_ALL = "billing.*";
    public static final String ROUTING_KEY_DLX_FAILED = "dlx.failed";
    public static final String ROUTING_KEY_DLX_ALL = "dlx.#";
}
