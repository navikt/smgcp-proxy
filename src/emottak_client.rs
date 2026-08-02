use base64::{engine::general_purpose::STANDARD as B64, Engine};
use reqwest::Client;
use thiserror::Error;
use tracing::debug;
use uuid::Uuid;

use crate::config::ServiceUser;

#[derive(Debug, Error)]
pub enum EmottakError {
    #[error("HTTP request failed: {0}")]
    Http(#[from] reqwest::Error),
    #[error("SOAP fault or unexpected response (status {status}): {body}")]
    SoapFault { status: u16, body: String },
}

pub struct EmottakClient {
    http: Client,
    endpoint: String,
    username: String,
    password: String,
}

impl EmottakClient {
    pub fn new(endpoint: String, service_user: ServiceUser) -> Self {
        let http = Client::builder()
            .use_rustls_tls()
            .build()
            .expect("Failed to build HTTP client");
        EmottakClient {
            http,
            endpoint,
            username: service_user.username,
            password: service_user.password,
        }
    }

    pub async fn start_subscription(
        &self,
        tss_ident: &str,
        sender: &[u8],
        partnerid: i32,
    ) -> Result<(), EmottakError> {
        let body = build_start_subscription_envelope(&self.endpoint, tss_ident, sender, partnerid);
        debug!("Sending StartSubscription SOAP request to {}", self.endpoint);

        let response = self
            .http
            .post(&self.endpoint)
            .basic_auth(&self.username, Some(&self.password))
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "")
            .body(body)
            .send()
            .await?;

        if !response.status().is_success() {
            let status = response.status().as_u16();
            let body = response.text().await.unwrap_or_default();
            return Err(EmottakError::SoapFault { status, body });
        }

        Ok(())
    }
}

fn build_start_subscription_envelope(
    endpoint: &str,
    tss_ident: &str,
    sender: &[u8],
    partnerid: i32,
) -> String {
    let data_b64 = B64.encode(sender);
    let msg_id = format!("urn:uuid:{}", Uuid::new_v4());
    let key_escaped = xml_escape(tss_ident);

    format!(
        r#"<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:sub="http://www.nav.no/emottak/subscription/"
    xmlns:wsa="http://www.w3.org/2005/08/addressing">
  <soapenv:Header>
    <wsa:Action>http://www.nav.no/emottak/subscription/SubscriptionPort/StartSubscriptionRequest</wsa:Action>
    <wsa:MessageID>{msg_id}</wsa:MessageID>
    <wsa:To>{endpoint}</wsa:To>
    <wsa:ReplyTo>
      <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>
    </wsa:ReplyTo>
  </soapenv:Header>
  <soapenv:Body>
    <sub:StartSubscriptionRequest>
      <sub:key>{key_escaped}</sub:key>
      <sub:data>{data_b64}</sub:data>
      <sub:partnerid>{partnerid}</sub:partnerid>
    </sub:StartSubscriptionRequest>
  </soapenv:Body>
</soapenv:Envelope>"#
    )
}

fn xml_escape(s: &str) -> String {
    s.chars()
        .flat_map(|c| match c {
            '&' => "&amp;".chars().collect::<Vec<_>>(),
            '<' => "&lt;".chars().collect(),
            '>' => "&gt;".chars().collect(),
            '"' => "&quot;".chars().collect(),
            '\'' => "&apos;".chars().collect(),
            other => vec![other],
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn xml_escape_special_chars() {
        assert_eq!(xml_escape("a&b<c>d\"e'f"), "a&amp;b&lt;c&gt;d&quot;e&apos;f");
    }

    #[test]
    fn xml_escape_plain() {
        assert_eq!(xml_escape("hello123"), "hello123");
    }
}
