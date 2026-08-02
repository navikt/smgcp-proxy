use std::sync::Arc;

use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    response::{IntoResponse, Response},
    Json,
};
use base64::{engine::general_purpose::STANDARD as B64, Engine};
use serde::Deserialize;
use tracing::{error, info, warn};

use crate::state::AppState;

#[derive(Debug, Deserialize)]
pub struct StartSubscriptionRequest {
    #[serde(rename = "tssIdent")]
    pub tss_ident: String,
    pub sender: String,
    #[serde(rename = "partnerreferanse")]
    pub partnerreferanse: i32,
}

pub async fn start_subscription(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    body: Option<Json<StartSubscriptionRequest>>,
) -> Response {
    let body = match body {
        Some(Json(b)) => b,
        None => {
            warn!("Mottatt request uten body");
            return (StatusCode::BAD_REQUEST, "Body mangler").into_response();
        }
    };

    let call_id = match headers
        .get("Nav-Call-Id")
        .and_then(|v| v.to_str().ok())
    {
        Some(id) => id.to_owned(),
        None => {
            warn!("Mangler Nav-Call-Id header");
            return (StatusCode::BAD_REQUEST, "Mangler Nav-Call-Id").into_response();
        }
    };

    info!(
        "Mottatt proxy-request for emottak start subscription for callId {call_id}"
    );

    let sender_bytes = match B64.decode(&body.sender) {
        Ok(b) => b,
        Err(e) => {
            warn!("Ugyldig base64-verdi for sender: {e}");
            return (StatusCode::BAD_REQUEST, "Ugyldig base64-verdi for sender").into_response();
        }
    };

    match state
        .emottak_client
        .start_subscription(&body.tss_ident, &sender_bytes, body.partnerreferanse)
        .await
    {
        Ok(()) => {
            info!("Sender http OK status for callId {call_id}");
            StatusCode::OK.into_response()
        }
        Err(e) => {
            error!("Noe gikk galt ved kall til emottak: {e} for callId {call_id}");
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                format!("Noe gikk galt ved proxykall til emottak: {e}"),
            )
                .into_response()
        }
    }
}
