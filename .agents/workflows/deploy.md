---
description: Build, version bump, git push, GitHub tag and release creation for CordSync
---

# CordSync Deploy Workflow

This workflow bumps the version, builds, pushes to GitHub, and creates a release with changelog.

// turbo-all

## Steps

1. Read current version from `pom.xml` and increment the patch number in both `pom.xml` and `plugin.yml`

2. Build with Maven:
```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -q
```

3. Stage all files:
```powershell
git add -A
```

4. Commit with version and changelog description (describe what was changed):
```powershell
git commit -m "v{VERSION} - {CHANGELOG_DESCRIPTION}"
```

5. Create git tag:
```powershell
git tag v{VERSION}
```

6. Push to GitHub (commit + tag):
```powershell
git push origin main --tags
```

7. Create GitHub Release with the JAR file attached and the changelog as body:
```powershell
& "C:\Program Files\GitHub CLI\gh.exe" release create v{VERSION} "target/CordSync-{VERSION}.jar" --title "CordSync v{VERSION}" --notes "{CHANGELOG_DESCRIPTION}"
```
