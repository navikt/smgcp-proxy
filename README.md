[![Deploy app to dev and prod](https://github.com/navikt/smgcp-proxy/actions/workflows/deploy.yml/badge.svg)](https://github.com/navikt/smgcp-proxy/actions/workflows/deploy.yml)

# smgcp-proxy
Proxy application for teamsykmelding for exposing internal services to GCP.

# Technologies used
* Rust
* Axum
* Tokio
* Prometheus
* Docker (distroless runtime image)

#### Requirements
* Rust (stable toolchain)

## Flowchart
This is an overview of the flow in the application
```mermaid
  flowchart LR
      smgcp-proxy <---> vault
      smgcp-proxy <--> syfosmmottak
      smgcp-proxy <--> eMottak;
 ```

## Getting started
### Building the application
#### Compile and run locally
``` bash
cargo build --release
```

Run tests:
``` bash
cargo test
```

### Contact

This project is maintained by [navikt/teamsykmelding](CODEOWNERS)

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/smgcp-proxy/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)
