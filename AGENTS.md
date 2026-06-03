## Worktree Safety

All edits must stay inside this repository root:
`C:\Users\tyler\AI Projects\ModStatusKit`

Before editing in a worktree, verify:
`git rev-parse --show-toplevel`

If a subagent is assigned a worktree, it must not edit the parent checkout. If edits appear outside the assigned worktree, fix and report the issue before continuing.

## Scope Boundaries

This project is a reusable Fabric mod status library. Do not edit CarryBabyAnimals, SignPort, MultiGolem, Minecraft-Docker, or any other repository from this workspace.

Do not publish, release, tag, or push unless explicitly asked.
