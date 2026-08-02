use std::net::SocketAddr;
use std::sync::Arc;
use tokio::signal;
use tracing::info;

mod auth;
mod config;
mod emottak_client;
mod metrics;
mod routes;
mod state;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt().json().with_current_span(false).init();

    let config = config::Config::from_env().expect("Failed to load configuration");
    let service_user = config::ServiceUser::from_files().expect("Failed to load service user credentials");

    info!("Fetching JWKS from {}", config.jwk_keys_url);
    let jwks = auth::fetch_jwks(&config.jwk_keys_url)
        .await
        .expect("Failed to fetch JWKS");

    let emottak_client =
        emottak_client::EmottakClient::new(config.emottak_endpoint_url.clone(), service_user);

    let app_state = Arc::new(state::AppState::new(config, jwks, emottak_client));

    metrics::register_metrics();

    let addr = SocketAddr::from(([0, 0, 0, 0], app_state.config.application_port));
    let app = routes::create_router(app_state.clone());

    info!("Starting server on {}", addr);
    let listener = tokio::net::TcpListener::bind(addr)
        .await
        .expect("Failed to bind TCP listener");

    axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal(app_state))
        .await
        .expect("Server error");
}

async fn shutdown_signal(state: Arc<state::AppState>) {
    signal::ctrl_c()
        .await
        .expect("Failed to install Ctrl+C handler");
    state.set_ready(false);
    info!("Received shutdown signal, draining connections for 10 seconds...");
    tokio::time::sleep(tokio::time::Duration::from_secs(10)).await;
}
