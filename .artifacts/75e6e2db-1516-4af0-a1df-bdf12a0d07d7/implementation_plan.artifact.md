# Publish AdsNextGen v3 to GitHub Packages

This plan covers updating the library version to `3.0.0` and preparing the project for publishing to GitHub Packages.

## User Review Required

> [!IMPORTANT]
> The current version is `1.0.3`. I will update it to `3.0.0`. Please confirm if you prefer a different versioning scheme.
> The `artifactId` is currently set to `ads-nextgen` in the build script, but the README uses `AdsNextGen`. I will align them to `ads-nextgen` for consistency with the build script, or you can let me know if you prefer `AdsNextGen`.

## Proposed Changes

### AdsNextGen Library

#### [MODIFY] [build.gradle.kts](file:///Users/kazmi/AndroidStudioProjects/AdsNextGen/AdsNextGen/build.gradle.kts)
- Update `version` to `3.0.0`.

#### [MODIFY] [README.md](file:///Users/kazmi/AndroidStudioProjects/AdsNextGen/README.md)
- Update installation instructions to point to GitHub Packages instead of JitPack (or include both).
- Update the version to `3.0.0`.
- Correct the `artifactId` to `ads-nextgen`.

---

## Verification Plan

### Automated Tests
- Run `./gradlew :AdsNextGen:assembleRelease` to ensure the library still builds correctly.
- Run `./gradlew :AdsNextGen:generateMetadataFileForReleasePublication` to verify publication metadata.

### Manual Verification
- After the code is pushed to the `kazmi-V3` branch, the GitHub Action will automatically trigger the publication.
- Verify the package appears in the GitHub repository's "Packages" section.
