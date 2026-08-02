use std::fs;

#[derive(Clone, Debug)]
pub struct Config {
    pub application_port: u16,
    #[allow(dead_code)]
    pub application_name: String,
    pub client_id: String,
    pub jwk_keys_url: String,
    pub jwt_issuer: String,
    pub emottak_endpoint_url: String,
}

impl Config {
    pub fn from_env() -> Result<Self, String> {
        Ok(Config {
            application_port: env_var("APPLICATION_PORT", Some("8080"))?
                .parse::<u16>()
                .map_err(|e| format!("Invalid APPLICATION_PORT: {e}"))?,
            application_name: env_var("NAIS_APP_NAME", Some("smgcp-proxy"))?,
            client_id: env_var("AZURE_APP_CLIENT_ID", None)?,
            jwk_keys_url: env_var("AZURE_OPENID_CONFIG_JWKS_URI", None)?,
            jwt_issuer: env_var("AZURE_OPENID_CONFIG_ISSUER", None)?,
            emottak_endpoint_url: env_var("SUBSCRIPTION_ENDPOINT_URL", None)?,
        })
    }
}

pub struct ServiceUser {
    pub username: String,
    pub password: String,
}

impl ServiceUser {
    pub fn from_files() -> Result<Self, String> {
        Ok(ServiceUser {
            username: read_file("/secrets/serviceuser/username")?,
            password: read_file("/secrets/serviceuser/password")?,
        })
    }
}

fn env_var(name: &str, default: Option<&str>) -> Result<String, String> {
    std::env::var(name).or_else(|_| {
        default
            .map(str::to_owned)
            .ok_or_else(|| format!("Missing required environment variable: {name}"))
    })
}

fn read_file(path: &str) -> Result<String, String> {
    fs::read_to_string(path).map_err(|e| format!("Failed to read {path}: {e}"))
}
