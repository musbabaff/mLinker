---
description: Build, version bump, git push and GitHub tag creation for mLinker
---

# mLinker Deploy Workflow

Bu workflow her güncelleme sonrası sürümü artırır, derler, GitHub'a push eder ve tag oluşturur.

// turbo-all

## Adımlar

1. Mevcut sürümü oku ve patch numarasını 1 artır (`pom.xml` ve `plugin.yml` dosyalarında)

2. Maven ile derle:
```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -q
```

3. Tüm dosyaları stage'le:
```powershell
git add -A
```

4. Commit at (sürüm numarası ile):
```powershell
git commit -m "v{VERSION} release"
```

5. Git tag oluştur:
```powershell
git tag v{VERSION}
```

6. GitHub'a push et (commit + tag):
```powershell
git push origin main --tags
```
