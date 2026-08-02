use std::sync::atomic::{AtomicBool, Ordering};

use crate::{auth::Jwks, config::Config, emottak_client::EmottakClient};

pub struct AppState {
    pub config: Config,
    pub jwks: Jwks,
    pub emottak_client: EmottakClient,
    alive: AtomicBool,
    ready: AtomicBool,
}

impl AppState {
    pub fn new(config: Config, jwks: Jwks, emottak_client: EmottakClient) -> Self {
        AppState {
            config,
            jwks,
            emottak_client,
            alive: AtomicBool::new(true),
            ready: AtomicBool::new(true),
        }
    }

    pub fn is_alive(&self) -> bool {
        self.alive.load(Ordering::Relaxed)
    }

    pub fn is_ready(&self) -> bool {
        self.ready.load(Ordering::Relaxed)
    }

    pub fn set_ready(&self, value: bool) {
        self.ready.store(value, Ordering::Relaxed);
    }
}
