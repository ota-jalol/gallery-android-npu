# GitHub Actions Workflows

This directory contains GitHub Actions workflows for automated building and releasing of the Gallery Android NPU app.

## Available Workflows

### 1. Build APK on Production Push (`production-build.yaml`)

**Triggers**: Automatically runs when code is pushed to the `production` branch

**What it does**:
- Builds the release APK
- Uploads the APK as a GitHub Actions artifact
- Creates a build summary with version information

**Use case**: Continuous integration for production branch - every push creates a build artifact that can be downloaded and tested.

### 2. Build and Release APK (`release.yaml`)

**Triggers**: 
- When a version tag is pushed (e.g., `v1.0.9`)
- When code is pushed to `production` or `main` branch

**What it does**:
- Builds the release APK
- Creates a GitHub Release with the APK attached
- Generates release notes with version information and NPU features
- Uploads the APK as an artifact

**Use case**: Creating official releases with downloadable APKs for distribution.

### 3. Build Android APK (`build_android.yaml`)

**Triggers**:
- Pull requests to `main`
- Pushes to `main`
- Manual workflow dispatch

**What it does**:
- Builds the APK to verify code compiles
- No artifacts uploaded

**Use case**: CI checks for pull requests and main branch changes.

## Usage Guide

### For Regular Development

When you push changes to the `production` branch:
```bash
git checkout production
git merge your-feature-branch
git push origin production
```

This will:
1. ✅ Trigger the production build workflow
2. ✅ Create a build artifact available for download
3. ❌ NOT create a release (just a build)

### For Creating Official Releases

To create a new release with APK download:

**Option 1: Push a version tag**
```bash
# Make sure you're on the production branch with latest changes
git checkout production
git pull

# Create and push a version tag
git tag v1.0.9
git push origin v1.0.9
```

**Option 2: Create tag from GitHub UI**
1. Go to Releases page
2. Click "Draft a new release"
3. Click "Choose a tag" → Create new tag (e.g., `v1.0.9`)
4. Target: `production` branch
5. Publish release

Both options will:
1. ✅ Trigger the release workflow
2. ✅ Build the APK
3. ✅ Create a GitHub Release with auto-generated release notes
4. ✅ Attach the APK to the release for public download

### Version Information

The workflows automatically extract version information from `Android/src/app/build.gradle.kts`:
- `versionCode` - Build number (e.g., 15)
- `versionName` - Version string (e.g., "1.0.9")

**Important**: Update these values in `build.gradle.kts` before creating a release.

### APK Naming Convention

APKs are automatically named:
- **Production builds**: `gallery-npu-v{version}-build-{run_number}.apk`
  - Example: `gallery-npu-v1.0.9-build-42.apk`
- **Releases**: `gallery-npu-v{version}-{version_code}.apk`
  - Example: `gallery-npu-v1.0.9-15.apk`

## Workflow Requirements

### Permissions

The workflows require the following GitHub permissions:
- `contents: write` - To create releases and upload artifacts

These are configured in the workflow files.

### Secrets

No secrets are required! The workflows use:
- `GITHUB_TOKEN` - Automatically provided by GitHub Actions

### Branch Protection (Recommended)

Consider setting up branch protection for `production`:
1. Go to Settings → Branches
2. Add rule for `production` branch
3. Enable:
   - ✅ Require pull request reviews
   - ✅ Require status checks to pass
   - ✅ Include administrators

## Troubleshooting

### Build Fails

1. Check the workflow logs in Actions tab
2. Verify `build.gradle.kts` syntax is correct
3. Ensure Android SDK dependencies are available
4. Check Java version (currently using JDK 21)

### Release Not Created

1. Verify you pushed to `production` branch or created a tag
2. Check workflow permissions in repo settings
3. Ensure `GITHUB_TOKEN` has write access

### APK Not Attached to Release

1. Check if the build step succeeded
2. Verify APK was found in `app/build/outputs/apk/release`
3. Check release workflow logs for file path issues

## Customization

### Change Target Branch

Edit the workflow files to change trigger branches:
```yaml
on:
  push:
    branches:
      - your-branch-name  # Change this
```

### Modify Release Notes

Edit `release.yaml` → `Create Release` step → `body` section to customize release notes template.

### Change APK Signing

To sign APKs with your keystore:
1. Add keystore file as GitHub secret
2. Add signing configuration to workflow
3. Update `build.gradle.kts` with signing config

## Example Workflow

Complete release process:

```bash
# 1. Update version in build.gradle.kts
# Edit: versionCode = 16
# Edit: versionName = "1.1.0"

# 2. Commit and push to production
git checkout production
git add Android/src/app/build.gradle.kts
git commit -m "Bump version to 1.1.0"
git push origin production

# 3. Create and push release tag
git tag v1.1.0
git push origin v1.1.0

# 4. Wait for workflow to complete
# 5. Release with APK will be available at:
#    https://github.com/ota-jalol/gallery-android-npu/releases/tag/v1.1.0
```

## Support

For issues with workflows:
- Check [GitHub Actions documentation](https://docs.github.com/en/actions)
- Review workflow run logs in the Actions tab
- Create an issue in the repository
