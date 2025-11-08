# Releasing

## Quick Release

1. Update `CHANGELOG.md` with release notes for the new version
2. Create and push a tag:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

The GitHub Actions workflow will automatically:
- Extract the version from the tag
- Extract release notes from CHANGELOG.md
- Publish to Maven Central
- Create a GitHub Release

## Release Checklist

- [ ] Update CHANGELOG.md with new version section (format: `## X.Y.Z`)
- [ ] Commit all changes to main branch
- [ ] Create annotated version tag (e.g., `v1.0.0`)
- [ ] Push tag to GitHub
- [ ] Verify GitHub Actions workflow completes successfully
- [ ] Verify GitHub Release was created with correct notes

## Version Format

We follow [Semantic Versioning](https://semver.org/):

- **Major**: `v2.0.0` - Breaking changes
- **Minor**: `v1.1.0` - New features, backward compatible
- **Patch**: `v1.0.1` - Bug fixes, backward compatible

## CHANGELOG Format

Ensure your CHANGELOG.md follows this format for automatic release note extraction:

```markdown
## 1.0.0

- Feature: Added support for XYZ
- Fix: Resolved issue with ABC
- Breaking: Removed deprecated method DEF

## 0.2.0

...
```

The workflow will extract everything between the version header and the next version header.

