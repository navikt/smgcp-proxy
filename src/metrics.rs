use std::sync::LazyLock;

use prometheus::{register_histogram_vec, Encoder, HistogramVec, TextEncoder};

pub static HTTP_HISTOGRAM: LazyLock<HistogramVec> = LazyLock::new(|| {
    register_histogram_vec!(
        "smgcpproxy_requests_duration_seconds",
        "http requests durations for incoming requests in seconds",
        &["path"]
    )
    .expect("Failed to register HTTP histogram")
});

/// Register all custom metrics with the default Prometheus registry.
/// Must be called once at startup before any metrics are recorded.
pub fn register_metrics() {
    LazyLock::force(&HTTP_HISTOGRAM);
}

/// Render all collected metrics in Prometheus text format (004).
pub fn metrics_text() -> String {
    let encoder = TextEncoder::new();
    let families = prometheus::gather();
    let mut buf = Vec::new();
    encoder
        .encode(&families, &mut buf)
        .expect("Failed to encode metrics");
    String::from_utf8(buf).expect("Metrics contained invalid UTF-8")
}
