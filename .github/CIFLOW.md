# Development Version and Branch Handling

This document describes the Git branching strategy, version numbering policy, and CI/CD workflows used by the JUDO Generator Commons project. The branching model is based on [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).

## Branches

The repository uses the following branch types:

| Branch Pattern | Purpose | Base Branch |
|---------------|---------|-------------|
| `develop` | Active development; contains the latest sources for the current version | -- |
| `feature/JNG-NUMBER_short_summary` | New features to be included in the current version | `develop` |
| `release/X.Y.Z` (or `X_Y_Z`) | Release stabilization and testing | `develop` |
| `bugfix/JNG-NUMBER_short_summary` | Bug fixes applied during release testing | release branch |
| `support/JNG-NUMBER_short_summary` | Minor changes for a previous release | release branch |
| `master` | Latest released sources | release branch (via merge) |
| `hotfix/JNG-NUMBER_short_summary` | Urgent fixes applied to both master and develop | `master` |

### Branch Lifecycle

```mermaid
gitGraph
    commit id: "initial"
    branch develop
    checkout develop
    commit id: "dev-1"
    branch feature/JNG-1
    checkout feature/JNG-1
    commit id: "feat-1"
    commit id: "feat-2"
    checkout develop
    merge feature/JNG-1 id: "merge-feat-1"
    branch feature/JNG-2
    checkout feature/JNG-2
    commit id: "feat-3"
    checkout develop
    merge feature/JNG-2 id: "merge-feat-2"
    branch release/1.0-beta1
    checkout release/1.0-beta1
    commit id: "stabilize"
    branch bugfix/JNG-4
    checkout bugfix/JNG-4
    commit id: "fix-1"
    checkout release/1.0-beta1
    merge bugfix/JNG-4 id: "merge-fix"
    checkout master
    merge release/1.0-beta1 id: "release-1.0"
    checkout develop
    merge release/1.0-beta1 id: "back-merge"
```

## Version Numbers

Version numbers follow semantic versioning with these rules:

| Event | Version Change | Example |
|-------|---------------|---------|
| Start a **feature** branch | No change | `1.0.0-SNAPSHOT` stays |
| Start a **release** branch | Increment 2nd number on `develop` | `develop` becomes `1.1.0-SNAPSHOT` |
| Start a **bugfix** branch | No change | Applied on release branch as-is |
| Start a **support** branch | Increment 3rd number | `1.0.1-SNAPSHOT` |
| Start a **hotfix** branch | Increment 4th number | `1.0.0.1-SNAPSHOT` |

## GitHub Action Workflows

The project uses several GitHub Actions workflows that automate building, testing, releasing, and merging.

### build.yml

Triggered on pushes to `develop` and pull requests targeting `develop`, `master`, `increment/*`, or `release/*` branches.

```mermaid
flowchart TD
    A[Push on develop or PR on develop/master/increment/release] --> B{Base branch?}
    B -->|"master, release/*"| C[Set version from pom.xml<br/>without -SNAPSHOT]
    B -->|"develop, increment/*"| D[Set version as<br/>major.minor.qualifier.date_commitId_branch]
    C --> E[Build and deploy to Nexus]
    D --> E
    E --> F[Create git tag v-version]
    F --> G{Base branch?}
    G -->|"increment/*, release/*"| H[Create merge-pr/version tag]
    H --> I[Trigger merge-pr-tagged.yml]
    G -->|develop| J[Build changelog]
    J --> K[Create GitHub pre-release]
```

### merge-pr-tagged.yml

Triggered when a `merge-pr/*` tag is pushed. Handles merging PRs to the correct branch based on version format.

```mermaid
flowchart TD
    A["Push on merge-pr/* tag"] --> B[Extract version from tag]
    B --> C{Version format?}
    C -->|major.minor.qualifier| D[Merge PR to master]
    D --> E[Trigger create-release-on-master.yml]
    C -->|other format| F[Squash PR to develop]
    F --> G[Trigger build.yml]
    D --> H[Delete merge-pr tag]
    F --> H
```

### create-release-on-master.yml

Triggered on pushes to `master`. Creates a final GitHub release with a changelog.

```mermaid
flowchart TD
    A[Push on master] --> B[Get version from tag]
    B --> C[Build changelog]
    C --> D[Create GitHub release as latest]
```

### release.yml

Manually triggered with a version parameter. Creates release and version-bump pull requests.

```mermaid
flowchart TD
    A["Manual trigger with version"] --> B{Version?}
    B -->|auto| C[Read version from pom.xml<br/>strip -SNAPSHOT]
    B -->|"X.Y.Z"| D[Use given version]
    C --> E[Set next version = qualifier + 1]
    D --> E
    E --> F[Create PR on master with release version]
    E --> G[Create PR on develop with next version]
    F --> H[Trigger build.yml]
    G --> I[Trigger build.yml]
```

## Development Rules

> **Important:** There is no commit without a JIRA ticket number. Every pull request and commit must reference a `JNG-xxx` ticket.

For issue tracking, the project uses [JIRA](https://blackbelt.atlassian.net/jira/dashboards).
