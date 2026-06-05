param(
    [ValidateSet("PreRelease", "PostRelease")]
    [string] $Mode = "PreRelease",
    [string] $Repo = "TnTBass/ModStatusKit"
)

$ErrorActionPreference = "Stop"

function Get-VersionParts {
    param([string] $Version)

    $match = [regex]::Match($Version, '^(\d+)\.(\d+)\.(\d+)$')
    if (-not $match.Success) {
        throw "VERSION must be a plain semantic version like 0.1.6; found '$Version'"
    }

    [int[]]@(
        [int]$match.Groups[1].Value,
        [int]$match.Groups[2].Value,
        [int]$match.Groups[3].Value
    )
}

function Compare-VersionText {
    param(
        [string] $Left,
        [string] $Right
    )

    $leftParts = Get-VersionParts $Left
    $rightParts = Get-VersionParts $Right

    for ($index = 0; $index -lt 3; $index++) {
        if ($leftParts[$index] -lt $rightParts[$index]) {
            return -1
        }
        if ($leftParts[$index] -gt $rightParts[$index]) {
            return 1
        }
    }

    return 0
}

function Get-VersionFromTag {
    param([string] $Tag)

    $match = [regex]::Match($Tag, '^v(\d+\.\d+\.\d+)$')
    if ($match.Success) {
        return $match.Groups[1].Value
    }

    return $null
}

function Assert-RepositoryIdentity {
    param(
        [string] $RemoteUrl,
        [string] $Repo
    )

    $normalizedUrl = $RemoteUrl.Trim().ToLowerInvariant()
    $normalizedRepo = $Repo.Trim().ToLowerInvariant()
    if ($normalizedRepo.EndsWith(".git")) {
        $normalizedRepo = $normalizedRepo.Substring(0, $normalizedRepo.Length - 4)
    }

    $httpsSuffix = "github.com/$normalizedRepo"
    $sshSuffix = "github.com:$normalizedRepo"

    if ($normalizedUrl.EndsWith(".git")) {
        $normalizedUrl = $normalizedUrl.Substring(0, $normalizedUrl.Length - 4)
    }

    if (-not ($normalizedUrl.EndsWith($httpsSuffix) -or $normalizedUrl.EndsWith($sshSuffix))) {
        throw "origin remote '$RemoteUrl' does not match expected GitHub repo '$Repo'"
    }
}

function Get-LatestReleasedVersion {
    param([string[]] $Tags)

    $latest = $null
    foreach ($tag in $Tags) {
        $version = Get-VersionFromTag $tag
        if ($null -eq $version) {
            continue
        }
        if ($null -eq $latest -or (Compare-VersionText $version $latest) -gt 0) {
            $latest = $version
        }
    }

    return $latest
}

function Assert-ContainsTag {
    param(
        [string[]] $Tags,
        [string] $Tag,
        [string] $Where
    )

    if ($Tags -notcontains $Tag) {
        throw "$Tag does not exist $Where"
    }
}

function Test-ReleaseGateState {
    param(
        [ValidateSet("PreRelease", "PostRelease")]
        [string] $Mode,
        [pscustomobject] $State
    )

    $version = $State.Version
    Get-VersionParts $version | Out-Null

    $tag = "v$version"
    $publishedTags = @($State.RemoteTags) + @($State.ReleaseTags)

    if ($Mode -eq "PreRelease") {
        if ($State.LocalTags -contains $tag) {
            throw "$tag already exists locally"
        }
        if ($State.RemoteTags -contains $tag) {
            throw "$tag already exists on origin"
        }
        if ($State.ReleaseTags -contains $tag) {
            throw "$tag already exists as a GitHub release"
        }
        $latest = Get-LatestReleasedVersion $publishedTags
        if ($null -ne $latest -and (Compare-VersionText $version $latest) -le 0) {
            throw "VERSION $version must be greater than latest released version $latest before release"
        }
        return
    }

    Assert-ContainsTag $State.LocalTags $tag "locally"
    Assert-ContainsTag $State.RemoteTags $tag "on origin"
    Assert-ContainsTag $State.ReleaseTags $tag "as a GitHub release"

    $target = $State.TagTargets[$tag]
    if ($target -ne $State.Head) {
        throw "$tag does not point at HEAD $($State.Head)"
    }
}

function Invoke-Git {
    param([string[]] $Arguments)

    $output = & git @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed"
    }

    return $output
}

function Get-RemoteTags {
    $refs = Invoke-Git @("ls-remote", "--tags", "origin")
    $tags = @()
    foreach ($ref in $refs) {
        $match = [regex]::Match($ref, 'refs/tags/(v\d+\.\d+\.\d+)(\^\{\})?$')
        if ($match.Success) {
            $tags += $match.Groups[1].Value
        }
    }

    return @($tags | Select-Object -Unique)
}

function Get-GitHubReleaseTags {
    param([string] $Repo)

    $json = & gh release list --repo $Repo --limit 1000 --json tagName
    if ($LASTEXITCODE -ne 0) {
        throw "gh release list failed for $Repo"
    }

    $releases = $json | ConvertFrom-Json
    return @($releases | ForEach-Object { $_.tagName })
}

function Get-ReleaseGateState {
    param([string] $Repo)

    $repoRoot = (Invoke-Git @("rev-parse", "--show-toplevel")).Trim()
    $remoteUrl = (Invoke-Git @("remote", "get-url", "origin")).Trim()
    Assert-RepositoryIdentity -RemoteUrl $remoteUrl -Repo $Repo

    $version = (Get-Content (Join-Path $repoRoot "VERSION") -Raw).Trim()
    $head = (Invoke-Git @("rev-parse", "HEAD")).Trim()
    $localTags = @(Invoke-Git @("tag", "--list"))
    $remoteTags = @(Get-RemoteTags)
    $releaseTags = @(Get-GitHubReleaseTags $Repo)
    $tagTargets = @{}

    foreach ($tag in $localTags) {
        if (Get-VersionFromTag $tag) {
            $target = (Invoke-Git @("rev-list", "-n", "1", $tag)).Trim()
            $tagTargets[$tag] = $target
        }
    }

    [pscustomobject]@{
        Version = $version
        Head = $head
        LocalTags = $localTags
        RemoteTags = $remoteTags
        ReleaseTags = $releaseTags
        TagTargets = $tagTargets
    }
}

function Invoke-ReleaseGate {
    param(
        [ValidateSet("PreRelease", "PostRelease")]
        [string] $Mode,
        [string] $Repo
    )

    $state = Get-ReleaseGateState $Repo
    Test-ReleaseGateState -Mode $Mode -State $state

    Write-Host "$Mode release gate passed for v$($state.Version)."
}

if ($MyInvocation.InvocationName -ne ".") {
    Invoke-ReleaseGate -Mode $Mode -Repo $Repo
}
