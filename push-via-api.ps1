$ErrorActionPreference = "Stop"
$repo = "chheangheng18/app"
$branch = "main"

# Get current commit SHA on remote
$ref = gh api "repos/$repo/git/refs/heads/$branch" | ConvertFrom-Json
$parentSha = $ref.object.sha
Write-Host "Parent commit: $parentSha"

# Get all tracked files (text + binary)
$files = git ls-files
$treeEntries = @()

foreach ($f in $files) {
    $fullPath = Join-Path $PWD $f
    $isBinary = $false
    
    # Check if file is binary (gradle-wrapper.jar, etc.)
    if ($f -match '\.(jar|png|ico|class)$') {
        $isBinary = $true
    }
    
    if ($isBinary) {
        $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($fullPath))
        $body = @{ content = $base64; encoding = "base64" } | ConvertTo-Json
    } else {
        $content = [IO.File]::ReadAllText($fullPath) -replace "`r`n", "`n"
        $body = @{ content = $content; encoding = "utf-8" } | ConvertTo-Json -Depth 5
    }
    
    Write-Host "Creating blob: $f"
    $blob = $body | gh api "repos/$repo/git/blobs" --input - | ConvertFrom-Json
    
    # Determine file mode (executable for gradlew)
    $mode = "100644"
    if ($f -eq "gradlew") { $mode = "100755" }
    
    $treeEntries += @{
        path = $f
        mode = $mode
        type = "blob"
        sha  = $blob.sha
    }
}

Write-Host "`nCreating tree with $($treeEntries.Count) entries..."
$treeBody = @{ tree = $treeEntries } | ConvertTo-Json -Depth 5
$tree = $treeBody | gh api "repos/$repo/git/trees" --input - | ConvertFrom-Json
Write-Host "Tree SHA: $($tree.sha)"

# Create commit
Write-Host "Creating commit..."
$commitBody = @{
    message = "Cambodian Lottery Results Android app with privacy policy"
    tree    = $tree.sha
    parents = @($parentSha)
} | ConvertTo-Json -Depth 5
$commit = $commitBody | gh api "repos/$repo/git/commits" --input - | ConvertFrom-Json
Write-Host "Commit SHA: $($commit.sha)"

# Update ref
Write-Host "Updating branch ref..."
$updateBody = @{ sha = $commit.sha; force = $true } | ConvertTo-Json
$updateBody | gh api "repos/$repo/git/refs/heads/$branch" --method PATCH --input - | Out-Null

Write-Host "`nDone! Code pushed to https://github.com/$repo"
