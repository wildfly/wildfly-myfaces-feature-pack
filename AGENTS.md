# AGENTS.md

## Project Overview

This is a WildFly Galleon Feature Pack that integrates Apache MyFaces 4.x as an alternative Jakarta Faces implementation for WildFly. When provisioned, it configures MyFaces as the default JSF implementation, replacing the standard Mojarra implementation.

**Key Technologies:**
- Java 17
- Maven 3.3.9+ (project uses Maven wrapper `./mvnw`)
- WildFly 41.x / WildFly Core 33.x
- Galleon (WildFly's provisioning system)
- Apache MyFaces 4.0.x

## Module Architecture

The project is a multi-module Maven build with four main modules:

### myfaces-injection
Contains CDI injection support that bridges MyFaces to WildFly's dependency injection framework.

**Key Classes:**
- `MyFacesInjectionProvider`: Implements MyFaces SPI for WildFly-managed injection
- `MyFacesAnnotationProvider`: Provides annotation scanning integration
- `MyFacesContainerInitializer`: ServletContainerInitializer for bootstrapping

### galleon-content
Defines the Galleon packaging artifacts:
- **Layer spec** (`layers/standalone/myfaces/layer-spec.xml`): Defines the "myfaces" layer with dependencies on CDI and JSF layers
- **Module descriptors** (`modules/`): JBoss Modules XML files defining MyFaces API, implementation, and injection modules
- **License metadata**: Tracks dependency licenses for compliance

### myfaces-feature-pack
The main feature pack assembly that combines galleon-content with WildFly's EE Galleon Pack. Uses `wildfly-galleon-maven-plugin` to build the final feature pack ZIP.

### testsuite
Integration tests that provision WildFly with the MyFaces feature pack and validate functionality.

**Test execution modes:**
- Standard WildFly server installation
- Bootable JAR (regular)
- Bootable JAR (preview)

All three modes are tested in parallel during `mvn test`.

## Build Commands

```bash
# Full build with tests
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=SubsystemSanityTestCase -pl testsuite/subsystem

# Build only feature pack (skip testsuite)
mvn clean install -DskipTests -pl \!testsuite
```

## Galleon Layer Architecture

The feature pack defines a single layer: `myfaces`

**Layer Dependencies:**
- `cdi` (from WildFly)
- `jsf` (from WildFly)

**Layer Effect:**
Sets `default-jsf-impl-slot=myfaces` on the JSF subsystem, switching from Mojarra to MyFaces.

**Packages Installed:**
- `jakarta.faces.api.myfaces` - MyFaces API module
- `jakarta.faces.impl.myfaces` - MyFaces implementation module
- `org.jboss.as.jsf-injection.myfaces` - WildFly injection bridge

## Release Process

Uses `release.sh` script which wraps `mvn release:prepare release:perform`:

```bash
# Standard release
./release.sh --release X.Y.Z.Final --development X.Y.Z+1-SNAPSHOT

# Dry run
./release.sh --release X.Y.Z.Final --development X.Y.Z+1-SNAPSHOT --dry-run

# Prerelease (marks GitHub release accordingly)
./release.sh --release X.Y.Z.Beta1 --development X.Y.Z-SNAPSHOT --prerelease

# Force non-standard version patterns (e.g., SNAPSHOT release version)
./release.sh --release X.Y.Z-SNAPSHOT --development X.Y.Z+1-SNAPSHOT --force
```

**All flags:**
| Flag | Description |
|------|-------------|
| `-r`, `--release` | Release version (required). Also used for the tag. |
| `-d`, `--development` | Next development version (required). |
| `--dry-run` | Nothing is updated or pushed. |
| `-f`, `--force` | Allows SNAPSHOT in release version / non-SNAPSHOT in dev version. |
| `-p`, `--prerelease` | Marks the GitHub release as a prerelease. |
| `--notes-start-tag` | Starting tag for generating GitHub release notes. |
| `-v`, `--verbose` | Verbose output. |
| Any other args | Passed through to Maven. |

**Release workflow:**
1. Validates release/dev versions (rejects SNAPSHOT release versions and non-SNAPSHOT dev versions unless `--force`)
2. Resolves `central.serverId` from Maven config and verifies a matching `<server>` entry exists in `settings.xml`
3. Cleans up the local Maven repo at `/tmp/m2/repository/<project>/` — removes directories older than 5 days (configurable via `DAYS` env var) and any SNAPSHOT directories
4. Runs `./mvnw clean release:clean release:prepare release:perform` with profiles `release,central-release,cloud-tests` and a project-specific local repo

**Tag format:** `${RELEASE_VERSION}` (e.g., `3.0.0.Final`).

## Version Updates

When updating dependency versions:
- **WildFly version**: Update `version.org.wildfly` in root pom.xml
- **MyFaces version**: Update `version.org.apache.myfaces` in root pom.xml
- **Galleon plugins**: Update `version.org.wildfly.galleon-plugins` in root pom.xml

Module versions in `galleon-content/src/main/resources/modules/` use Maven filtering - they reference `${version.org.apache.myfaces}` which gets replaced during build.

## Testing Strategy

Tests provision a full WildFly server with the MyFaces feature pack, then validate:
- Subsystem configuration is correct
- MyFaces modules are properly installed
- Layer provisioning works correctly

The test harness uses `wildfly-core-test-runner` to manage server lifecycle.

## Branch Strategy

- **master**: Main development branch for Faces 4.1 releases
- **2.0.x**: Maintenance branch for Faces 4.0 releases

Tags follow the format: `X.Y.Z.Final` (e.g., `2.0.0.Final`).
