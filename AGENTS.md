# AGENTS.md

## Project Overview

This is a WildFly Galleon Feature Pack that integrates Apache MyFaces 4.1.x as an alternative Jakarta Faces implementation for WildFly. When provisioned, it configures MyFaces as the default JSF implementation, replacing the standard Mojarra implementation.

**Key Technologies:**
- Java 17 (`maven.compiler.release=17` in the root pom)
- Maven 3.9+ (project ships the Maven wrapper `./mvnw`, currently 3.9.16)
- `org.jboss:jboss-parent` 52 (parent POM)
- WildFly 41.x / WildFly Core 33.x
- Galleon (WildFly's provisioning system)
- Apache MyFaces 4.1.x

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

All three modes run during `mvn test` as three separate `maven-surefire-plugin`
executions (`default-test`, `bootable-jar-test`, `preview-bootable-jar-test`) in
the `testsuite/subsystem` module.

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

Releases are driven entirely by the `maven-release-plugin`; JReleaser (configured
in the `release` profile in `myfaces-feature-pack/pom.xml`) creates the GitHub
release. There is no wrapper script.

```bash
# Standard release
./mvnw release:clean release:prepare release:perform \
    -DreleaseVersion=X.Y.Z.Final -DdevelopmentVersion=X.Y.Z+1-SNAPSHOT

# Dry run (nothing committed, tagged, pushed, deployed, or released)
./mvnw release:clean release:prepare release:perform \
    -DreleaseVersion=X.Y.Z.Final -DdevelopmentVersion=X.Y.Z+1-SNAPSHOT \
    -DdryRun=true -DpushChanges=false -Djreleaser.dryrun=true
```

`releaseVersion`/`developmentVersion` may be omitted to be prompted interactively.
The git tag is derived automatically from `tagNameFormat` (`@{project.version}`,
e.g. `3.0.0.Final`), so `-Dtag` is not needed.

**How it wires together (all configured in the POMs — see `preparationProfiles` /
`releaseProfiles` / `arguments` in the root pom):**
1. `release:prepare` runs `clean install`, bumps to the release version, creates
   and pushes the signed tag, then bumps to the next `-SNAPSHOT`.
2. `release:perform` checks out the tag and runs `deploy` with profiles
   `release,central-release`:
   - `central-release` (from jboss-parent): GPG-signs the Maven artifacts,
     builds javadoc, and publishes to Maven Central via
     `central-publishing-maven-plugin`.
   - `release` (this project): JReleaser `full-release` creates the GitHub
     release with a generated changelog and attaches + signs the feature pack ZIP.
3. Prerelease status is detected automatically from the version qualifier
   (`Alpha`/`Beta`/`CR`/`RC`/`Milestone`/`Preview`) — no manual flag.

**Prerequisites (all sourced from the environment / `settings.xml`, nothing in the POM):**
| What | Where |
|------|-------|
| Maven Central credentials | `settings.xml` `<server id="central">` |
| GPG key for Maven artifact signing | local GPG agent / `-Dgpg.passphrase` |
| GitHub release token | `JRELEASER_GITHUB_TOKEN` env var |
| GPG key for JReleaser asset signing | `JRELEASER_GPG_PASSPHRASE`, `JRELEASER_GPG_PUBLIC_KEY`, `JRELEASER_GPG_SECRET_KEY` env vars |

> CI/clean-room note: to build against an isolated local repo, add
> `-Dmaven.repo.local=/some/path`. (The old `release.sh` did this against
> `/tmp/m2/repository/<project>/`; it is not required for a correct release.)

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

- **master**: Main development branch
- **2.0.x**: Maintenance branch (if needed)

Tags follow the format: `X.Y.Z.Final` (e.g., `3.0.0.Final`).
