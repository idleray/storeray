# Release Process

## Overview

Releasing a new version of storeray is fully automated via GitHub Actions. You only need to:

1. Update the version in `build.gradle.kts`
2. Commit and push a git tag

The CI pipeline will:
- Build the `distZip` distribution
- Upload the zip to GitHub Releases
- Automatically update the Homebrew formula in `idleray/homebrew-tap`

## Step-by-step

### 1. Update version

Edit `build.gradle.kts` and set the new version:

```kotlin
version = "1.1.0"
```

Commit the change:

```bash
git add build.gradle.kts
git commit -m "Bump version to 1.1.0"
```

### 2. Tag and push

```bash
git tag v1.1.0
git push origin main
git push origin v1.1.0
```

### 3. Wait for CI

Two workflows run automatically:

| Workflow | Trigger | What it does |
|---|---|---|
| `release.yml` | Tag push `v*` | Builds distZip and creates GitHub Release |
| `update-homebrew.yml` | Release published | Updates formula in `idleray/homebrew-tap` |

Check their status:

```bash
gh run list -L 5
```

## Workflow files

- `.github/workflows/release.yml` — Build and upload release artifacts
- `.github/workflows/update-homebrew.yml` — Auto-update Homebrew formula

## Manual steps (only needed for the first release)

These have already been done:

- [x] Create GitHub repo `idleray/storeray`
- [x] Create tap repo `idleray/homebrew-tap`
- [x] Add formula `Formula/storeray.rb` to tap repo
- [x] Set `HOMEBREW_TAP_TOKEN` secret in storeray repo
- [x] Create GitHub Actions workflow files

## Requirements

- `HOMEBREW_TAP_TOKEN` secret must exist in `idleray/storeray` repo settings
  (created once via `gh repo secret set HOMEBREW_TAP_TOKEN --repo idleray/storeray`)
