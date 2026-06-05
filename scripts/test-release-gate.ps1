$ErrorActionPreference = "Stop"

. "$PSScriptRoot\check-release-gate.ps1"

function Assert-Passes {
    param(
        [string] $Name,
        [scriptblock] $Block
    )

    try {
        & $Block
    } catch {
        throw "$Name failed unexpectedly: $($_.Exception.Message)"
    }
}

function Assert-Fails {
    param(
        [string] $Name,
        [scriptblock] $Block,
        [string] $ExpectedMessage
    )

    try {
        & $Block
    } catch {
        if ($_.Exception.Message -notlike "*$ExpectedMessage*") {
            throw "$Name failed with unexpected message: $($_.Exception.Message)"
        }
        return
    }

    throw "$Name passed unexpectedly"
}

function New-TestState {
    param(
        [string] $Version = "0.1.6",
        [string] $Head = "abc123",
        [string[]] $LocalTags = @("v0.1.5"),
        [string[]] $RemoteTags = @("v0.1.5"),
        [string[]] $ReleaseTags = @("v0.1.5"),
        [hashtable] $TagTargets = @{ "v0.1.5" = "old111"; "v0.1.6" = "abc123" }
    )

    [pscustomobject]@{
        Version = $Version
        Head = $Head
        LocalTags = $LocalTags
        RemoteTags = $RemoteTags
        ReleaseTags = $ReleaseTags
        TagTargets = $TagTargets
    }
}

Assert-Passes "PreRelease allows an unreleased higher VERSION" {
    Test-ReleaseGateState -Mode PreRelease -State (New-TestState)
}

Assert-Fails "PreRelease rejects an existing GitHub release for VERSION" {
    Test-ReleaseGateState -Mode PreRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -LocalTags @() `
            -RemoteTags @() `
            -ReleaseTags @("v0.1.5")
    )
} "already exists as a GitHub release"

Assert-Fails "PreRelease rejects VERSION lower than the latest release" {
    Test-ReleaseGateState -Mode PreRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -LocalTags @() `
            -RemoteTags @() `
            -ReleaseTags @("v0.1.6")
    )
} "must be greater than latest released version 0.1.6"

Assert-Fails "PreRelease rejects VERSION equal to an existing GitHub release" {
    Test-ReleaseGateState -Mode PreRelease -State (
        New-TestState `
            -Version "0.1.6" `
            -LocalTags @() `
            -RemoteTags @() `
            -ReleaseTags @("v0.1.6")
    )
} "already exists as a GitHub release"

Assert-Fails "PreRelease rejects VERSION lower than the latest remote tag" {
    Test-ReleaseGateState -Mode PreRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -LocalTags @() `
            -RemoteTags @("v0.1.6") `
            -ReleaseTags @()
    )
} "must be greater than latest released version 0.1.6"

Assert-Passes "PreRelease ignores newer local-only tags when VERSION is unreleased" {
    Test-ReleaseGateState -Mode PreRelease -State (
        New-TestState `
            -Version "0.1.6" `
            -LocalTags @("v0.2.0") `
            -RemoteTags @("v0.1.5") `
            -ReleaseTags @("v0.1.5")
    )
}

Assert-Fails "PreRelease rejects an existing local tag for VERSION" {
    Test-ReleaseGateState -Mode PreRelease -State (New-TestState -LocalTags @("v0.1.5", "v0.1.6"))
} "already exists locally"

Assert-Fails "PreRelease rejects an existing remote tag for VERSION" {
    Test-ReleaseGateState -Mode PreRelease -State (New-TestState -RemoteTags @("v0.1.5", "v0.1.6"))
} "already exists on origin"

Assert-Passes "PostRelease accepts matching local tag remote tag and GitHub release at HEAD" {
    Test-ReleaseGateState -Mode PostRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -Head "abc123" `
            -LocalTags @("v0.1.5") `
            -RemoteTags @("v0.1.5") `
            -ReleaseTags @("v0.1.5") `
            -TagTargets @{ "v0.1.5" = "abc123" }
    )
}

Assert-Fails "PostRelease rejects a missing local tag" {
    Test-ReleaseGateState -Mode PostRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -Head "abc123" `
            -LocalTags @() `
            -RemoteTags @("v0.1.5") `
            -ReleaseTags @("v0.1.5") `
            -TagTargets @{ "v0.1.5" = "abc123" }
    )
} "does not exist locally"

Assert-Fails "PostRelease rejects a missing remote tag" {
    Test-ReleaseGateState -Mode PostRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -Head "abc123" `
            -LocalTags @("v0.1.5") `
            -RemoteTags @() `
            -ReleaseTags @("v0.1.5") `
            -TagTargets @{ "v0.1.5" = "abc123" }
    )
} "does not exist on origin"

Assert-Fails "PostRelease rejects a missing GitHub release" {
    Test-ReleaseGateState -Mode PostRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -Head "abc123" `
            -LocalTags @("v0.1.5") `
            -RemoteTags @("v0.1.5") `
            -ReleaseTags @() `
            -TagTargets @{ "v0.1.5" = "abc123" }
    )
} "does not exist as a GitHub release"

Assert-Fails "PostRelease rejects a version tag that is not HEAD" {
    Test-ReleaseGateState -Mode PostRelease -State (
        New-TestState `
            -Version "0.1.5" `
            -Head "abc123" `
            -LocalTags @("v0.1.5") `
            -RemoteTags @("v0.1.5") `
            -ReleaseTags @("v0.1.5") `
            -TagTargets @{ "v0.1.5" = "old111" }
    )
} "does not point at HEAD"

Assert-Passes "Repository identity accepts HTTPS origin URL" {
    Assert-RepositoryIdentity -RemoteUrl "https://github.com/TnTBass/ModStatusKit.git" -Repo "TnTBass/ModStatusKit"
}

Assert-Passes "Repository identity accepts SSH origin URL" {
    Assert-RepositoryIdentity -RemoteUrl "git@github.com:TnTBass/ModStatusKit.git" -Repo "TnTBass/ModStatusKit"
}

Assert-Fails "Repository identity rejects a different origin URL" {
    Assert-RepositoryIdentity -RemoteUrl "https://github.com/TnTBass/OtherRepo.git" -Repo "TnTBass/ModStatusKit"
} "origin remote"

Assert-Fails "Repository identity rejects a different SSH origin URL" {
    Assert-RepositoryIdentity -RemoteUrl "git@github.com:TnTBass/OtherRepo.git" -Repo "TnTBass/ModStatusKit"
} "origin remote"

Write-Host "Release gate tests passed."
