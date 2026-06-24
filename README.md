# storeray

A lightweight CLI for App Store Connect & Google Play metadata management.

## Installation

### Homebrew (macOS & Linux)

```bash
brew tap idleray/tap
brew install storeray
```

### Manual (any OS with JDK 17+)

Download the latest `storeray-v*.zip` from [releases](https://github.com/idleray/storeray/releases), unzip and run:

```bash
unzip storeray-v*.zip
./storeray-v*/bin/storeray --help
```

## Usage

```
storeray [OPTIONS] COMMAND [args]...
```

### Global options

| Option | Description |
|---|---|
| `-d, --dir` | Workspace directory (default: `./storeray`) |

### Commands

| Command | Alias | Description |
|---|---|---|
| `init` | | Initialize storeray workspace with templates |
| `iap` | | IAP (In-App Purchase) management tools |
| `iap sync` | | Sync IAP localization metadata |
| `iap inspect` | | Inspect a single IAP product |
| `release-notes` | `rn` | Release Notes management tools |
| `release-notes update` | `rn update` | Update Release Notes for the pending version |
| `release-notes playstore-tracks` | `rn playstore-tracks` | Print Google Play tracks and releases |
| `appinfo` | | App Info metadata management tools |
| `appinfo fetch` | | Fetch app info metadata from store to local |
| `appinfo sync` | | Sync app info metadata to store |

### Quick start

```bash
# Initialize workspace
storeray init

# Fetch current app info from App Store
storeray appinfo fetch --platform appstore

# Update release notes (dry-run first)
storeray rn update --platform both
storeray rn update --platform both --apply
```

### Requirements

- **Java**: JDK 17+ (JRE only is sufficient at runtime)

## License

MIT
