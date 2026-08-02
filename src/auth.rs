use std::collections::HashMap;
use std::sync::Arc;

use axum::{
    extract::{Request, State},
    http::StatusCode,
    middleware::Next,
    response::Response,
};
use jsonwebtoken::{
    decode, decode_header, jwk::JwkSet, Algorithm, DecodingKey, Validation,
};
use serde::Deserialize;
use tracing::warn;

use crate::state::AppState;

/// Wrapper around JwkSet with a key-by-kid index for fast lookups.
pub struct Jwks {
    set: JwkSet,
    index: HashMap<String, usize>,
}

impl Jwks {
    pub fn new(set: JwkSet) -> Self {
        let index = set
            .keys
            .iter()
            .enumerate()
            .filter_map(|(i, k)| k.common.key_id.as_ref().map(|kid| (kid.clone(), i)))
            .collect();
        Jwks { set, index }
    }

    fn find_key(&self, kid: &str) -> Option<&jsonwebtoken::jwk::Jwk> {
        self.index.get(kid).map(|&i| &self.set.keys[i])
    }
}

#[derive(Debug, Deserialize)]
#[allow(dead_code)]
pub struct Claims {
    pub iss: String,
    pub aud: serde_json::Value,
    pub azp: Option<String>,
}

pub async fn fetch_jwks(url: &str) -> Result<Jwks, reqwest::Error> {
    let set: JwkSet = reqwest::get(url).await?.json().await?;
    Ok(Jwks::new(set))
}

/// Axum middleware that validates the ****** using the cached JWKS.
pub async fn require_auth(
    State(state): State<Arc<AppState>>,
    req: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    let token = extract_bearer(&req).ok_or_else(|| {
        warn!("Request missing Authorization header");
        StatusCode::UNAUTHORIZED
    })?;

    validate_token(token, &state).map_err(|err| {
        warn!("JWT validation failed: {err}");
        StatusCode::UNAUTHORIZED
    })?;

    Ok(next.run(req).await)
}

fn extract_bearer(req: &Request) -> Option<&str> {
    req.headers()
        .get(axum::http::header::AUTHORIZATION)
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.strip_prefix("Bearer "))
}

fn validate_token(
    token: &str,
    state: &AppState,
) -> Result<Claims, jsonwebtoken::errors::Error> {
    let header = decode_header(token)?;
    let kid = header
        .kid
        .ok_or_else(|| jsonwebtoken::errors::Error::from(jsonwebtoken::errors::ErrorKind::InvalidKeyFormat))?;

    let jwk = state.jwks.find_key(&kid).ok_or_else(|| {
        jsonwebtoken::errors::Error::from(jsonwebtoken::errors::ErrorKind::InvalidKeyFormat)
    })?;

    let key = DecodingKey::from_jwk(jwk)?;

    let mut validation = Validation::new(Algorithm::RS256);
    validation.set_issuer(&[&state.config.jwt_issuer]);
    validation.set_audience(&[&state.config.client_id]);

    let token_data = decode::<Claims>(token, &key, &validation)?;
    Ok(token_data.claims)
}
