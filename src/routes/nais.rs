use std::sync::Arc;

use axum::{
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
};

use crate::{metrics::metrics_text, state::AppState};

pub async fn is_alive(State(state): State<Arc<AppState>>) -> Response {
    if state.is_alive() {
        (StatusCode::OK, "I'm alive! :)").into_response()
    } else {
        (StatusCode::INTERNAL_SERVER_ERROR, "I'm dead x_x").into_response()
    }
}

pub async fn is_ready(State(state): State<Arc<AppState>>) -> Response {
    if state.is_ready() {
        (StatusCode::OK, "I'm ready! :)").into_response()
    } else {
        (StatusCode::INTERNAL_SERVER_ERROR, "Please wait! I'm not ready :(").into_response()
    }
}

pub async fn prometheus_metrics() -> Response {
    let body = metrics_text();
    (
        StatusCode::OK,
        [("Content-Type", "text/plain; version=0.0.4")],
        body,
    )
        .into_response()
}
