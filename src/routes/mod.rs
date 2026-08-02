pub mod emottak;
pub mod nais;

use std::sync::Arc;

use axum::{
    extract::Request,
    middleware::{self, Next},
    response::Response,
    routing::{get, post},
    Router,
};
use std::time::Instant;

use crate::{
    auth::require_auth,
    metrics::HTTP_HISTOGRAM,
    state::AppState,
};

static UUID_PATTERN: std::sync::LazyLock<regex::Regex> = std::sync::LazyLock::new(|| {
    regex::Regex::new(
        r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    )
    .unwrap()
});

async fn metrics_middleware(req: Request, next: Next) -> Response {
    let path = req.uri().path().to_owned();
    let label = UUID_PATTERN.replace_all(&path, ":id").into_owned();
    let start = Instant::now();
    let response = next.run(req).await;
    HTTP_HISTOGRAM
        .with_label_values(&[&label])
        .observe(start.elapsed().as_secs_f64());
    response
}

pub fn create_router(state: Arc<AppState>) -> Router {
    let authenticated = Router::new()
        .route("/emottak/startsubscription", post(emottak::start_subscription))
        .layer(middleware::from_fn_with_state(state.clone(), require_auth));

    Router::new()
        .route("/internal/is_alive", get(nais::is_alive))
        .route("/internal/is_ready", get(nais::is_ready))
        .route("/internal/prometheus", get(nais::prometheus_metrics))
        .merge(authenticated)
        .layer(middleware::from_fn(metrics_middleware))
        .with_state(state)
}
