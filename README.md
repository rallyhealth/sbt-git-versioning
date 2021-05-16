**sbt-git-versioning** is a suite of sbt plugins designed to make maintaining a simple, consistent, and accurate
 [semantic versioning](http://semver.org/spec/v2.0.0.html) scheme with as little manual labor as possible.

There are two sbt plugins in this one plugin library:
* [GitVersioningPlugin](#GitVersioningPlugin)
* [SemVerPlugin](#SemVerPlugin)

# Compatibility

Tested and supported for sbt versions: 0.13.18 and 1.2.8. We don't currently support SBT 1.3.x because [there isn't a
version of MiMa 0.3.0 built for SBT 1.3.x, only for 1.2.x and 0.13.x](https://github.com/lightbend/mima#usage).

# Install

1. Remove the `version := ...` directive from the project's `build.sbt` file
2. Add the following code to `project/plugins.sbt` file (if you want to use Ivy style resolution):
```scala
  resolvers += Resolver.bintrayIvyRepo("rallyhealth", "sbt-plugins")
```
Alternatively, you can add the following (if you want to use Maven style resolution):
```scala
  Resolver.bintrayRepo("rallyhealth", "sbt-plugins")
```
In either case, you should now be able to add the plugin dependency to `project/plugins.sbt`:
```scala
  addSbtPlugin("com.rallyhealth.sbt" % "sbt-git-versioning" % "x.y.z")
```
3. Enable the SemVer plugin to enforce SemVer with MiMa. (Important for shared libraries, not so much for personal apps)

```scala
val example = project
  .enablePlugins(SemVerPlugin)
```
4. Add a `gitVersioningSnapshotLowerBound` placeholder in build.sbt.
```sbt
// Uncomment when you're ready to start building 1.0.0-...-SNAPSHOT versions.
// gitVersioningSnapshotLowerBound in ThisBuild := "1.0.0"
```

# GitVersioningPlugin

[GitVersioningPlugin](blob/master/src/main/scala/com/rallyhealth/versioning/GitVersioningPlugin.scala)
focuses on automatically determining the value of the `version` setting. The `version` is determined by looking at git history (for
previous tags) and the state of the working directly. Read on for the exact details.

## Usage

This plugin will automatically determine and set the `version` setting on startup. There is nothing developers need to
explicitly do. You can `publishLocal` and PRs and use the created snapshot artifacts anywhere you would a release artifact.

You will see additional log statements like:
```
[info] Skipping fetching tags from git remotes; to enable, set the system property version.autoFetch=true
[info] GitVersioningPlugin set versionFromGit=2.0.0-dirty-SNAPSHOT
[info] GitVersioningPlugin set version=2.0.0-dirty-SNAPSHOT
[info] GitVersioningPlugin set isCleanRelease=false
```

## Information

The version format is:

* Leading `v`
* Major.Minor.Patch, e.g `1.2.3`
* (Optional) Commits since last tag
* (Optional) Short hash of the current commit
* (Optional) `-dirty-SNAPSHOT` if uncommitted changes

Putting it all together (including all optionals) gives you `v1.2.3-<commit_count>-<hash>-dirty-SNAPSHOT`.

| If HEAD is...         | ...and commits have a tag (ex. `v1.0.3`) ... | ...and is "clean"  | Then the version is                |
|-----------------------|----------------------------------------------|--------------------|------------------------------------|
| tagged (ex. `v1.1.0`) | :heavy_minus_sign:                           | :white_check_mark: | `v1.1.0`                           |
| tagged (ex. `v1.1.0`) | :heavy_minus_sign:                           | :x:                | `v1.1.0-dirty-SNAPSHOT`            |
| not tagged            | :white_check_mark:                           | :white_check_mark: | `v1.0.4-1-0123abc-SNAPSHOT`        |
| not tagged            | :white_check_mark:                           | :x:                | `v1.0.4-1-0123abc-dirty-SNAPSHOT`  |
| not tagged            | :x:                                          | :white_check_mark: | `v0.0.1-1-0123abc-SNAPSHOT`        |
| not tagged            | :x:                                          | :x:                | `v0.0.1-1-0123abc-dirty-SNAPSHOT`  |
| non-existent          | :heavy_minus_sign:                           | :white_check_mark: | `v0.0.1-dirty-SNAPSHOT`            |
| non-existent          | :heavy_minus_sign:                           | :x:                | `v0.0.1-dirty-SNAPSHOT`            |

## Nudging the version

The version is generally derived from git, though there are a couple ways to change that.

### Version Override

The version.override arg sets the version and overrides all other sources.

```
sbt -Dversion.override=1.2.3 ...
```

### gitVersioningSnapshotLowerBound

The `gitVersioningSnapshotLowerBound` settingKey can push the version to a higher version snapshot than the current git state.

This is useful for preparing major releases with breaking changes (esp. when combined with shading -- stay tuned for more features here).

| versionFromGit           | gitVersioningSnapshotLowerBound | Final Version            |
| --------------           | --------------------------------- | -------------            |
| 1.0.0                    |                                   | 1.0.0                    |
| 1.0.0-n-0123abc-SNAPSHOT |                                   | 1.0.0-n-0123abc-SNAPSHOT |
| 1.0.0                    | 2.0.0                             | 2.0.0-n-0123abc-SNAPSHOT |

### abbrevLength

The `abbrevLength` settingKey can be used to manually force a certain length used for the hash in the version description. This is normally set by Git, which sets a minimum length based on the number of commits in your repository (with a minimum of 7). Setting this number manually can be useful if your CI server is working with shallow repositories (which is not recommended - see "Notes") or you want to ensure a consistent length of snapshot versions as your repository grows. This parameter matches the behavior of the `--abbrev` parameter of `git describe`.

### Release arg Property

The release arg bumps the version up by a major, minor, or patch increment.
```
sbt -Drelease=<major|minor|patch> ...
```

The release arg alters the value of `versionFromGit`, but is still bounded by `gitVersioningSnapshotLowerBound`. For example:

| versionFromGit | -Drelease | gitVersioningSnapshotLowerBound | Final Version |
| -------------- | --------- | ------------------------------- | ------------- |
| 1.0.0          | patch     |                                 | 1.0.1         |
| 1.0.0          | minor     |                                 | 1.1.0         |
| 1.0.0          | major     |                                 | 2.0.0         |
| 1.0.0          | patch     | 2.0.0                           | 2.0.0-n-0123abc-SNAPSHOT |
| 1.0.0          | minor     | 2.0.0                           | 2.0.0-n-0123abc-SNAPSHOT |
| 1.0.0          | major     | 2.0.0                           | 2.0.0         |

#### Example
With most recent git tag at `v1.4.2` and a `gitVersioningSnapshotLowerBound` setting of:
```
gitVersioningSnapshotLowerBound in ThisBuild := "2.0.0"
```

```
$ sbt
[info] GitVersioningPlugin set versionFromGit=1.4.3-1-400b9ac-SNAPSHOT
[info] GitVersioningPlugin set version=2.0.0-1-400b9ac-SNAPSHOT
>
```

### Notes

* The patch version is incremented if there are commits, dirty or not. But it is not incremented if there are no
commits. (I'm not clear on why this is, but it is legacy behavior.)
* The hash does **not** have a 'g' prefix like the output of `git describe`
* A build will be flagged as not clean (and will have a `-dirty-SNAPSHOT` identifier applied) if
`git status --porcelain` returns a non-empty result.
* If there is no tag in the commit history, the number appended after the version number will reflect the number of commits
since the creation of the repository.
* This plugin is intentionally different than something like [sbt-release](https://github.com/sbt/sbt-release) which stores
the version in a `version.sbt` file. Those types of plugins require more manual effort on the part of the developer.

*Warning:* Git Versioning may misbehave with shallow clones, as it cannot be guaranteed that a shallow clone includes all tags and commits necessary to determine the version in a consistent If the incorrect version is being returned and tags are
accessible, you may be using a shallow clone, in which case `git fetch --unshallow` will fix the issue. On CI systems,
ensure that any git plugins are configured to not use shallow clones. If you are unable to use deep clones, you can set the `abbrevLength` parameter in order to ensure snapshot versions are consistently named.

## Creating a Release

### Recommended: -Drelease

Creating a release is done by passing a [release arg](#release-arg-property).
```
sbt -Drelease=patch publish[Local]
```

You can extract the version for other purposes (e.g. git tagging after successful publish) using the `writeVersion <file>` input task.

```bash
export VERSIONFILE=$(mktemp) # avoid creating untracked files so version doesn't become -dirty.
sbt "writeVersion $VERSIONFILE"
export VERSION=$(cat $VERSIONFILE)
git tag "v${VERSION}""
git push origin "v${VERSION}"
# ...
```

### Possible: tag + sbt-git-versioning

...or by tagging and then publishing a release...
 ```
git tag v1.2.3
sbt publish[Local]
```

### Not recommended (unless have good reasons): version.override

...or by overriding the version that will be applied to a specific build, using the
`version.override` setting. Typically this is done by at the command line to avoid changing `build.sbt`.
```
sbt -Dversion.override=1.2.3 publish[Local]
```

You shouldn't do this without good reason. Version determination can be complicated, because git can be complicated.

## Extra Identifiers

To add an extra identifier like "-alpha" or "-rc1" or "-rally" it must be included it in the version directly
by overriding the "version" setting directly. (There was a feature to add those separately but it has been
removed because it was never used. Feel free to re-add it.)

# SemVerPlugin

[SemVerPlugin](blob/master/src/main/scala/com/rallyhealth/sbt/semver/SemVerPlugin.scala)
will halt the build (compile, publish, etc.) if your changes would not make a valid [semantic version](http://semver.org/spec/v2.0.0.html).
The plugin checks the previous release, your changes, and the `semVerLimitRelease` to ensure your changes are really
patch/minor/major changes if you want to release a patch/minor/major.

## Usage

You can run the check manually using `semVerCheck`. The check and also be run automatically:

* *Compile* - If `semVerCheckOnCompile` is set to true (the default) it will check after you compile
* *Test* - If `semVerCheckOnTest` is set to true (the default) it will check after you test
* *Publish* - If `semVerCheckOnPublish` is set to true (the default) it will check **before** you publish
* *PublishLocal* - If `semVerCheckOnPublish` is set to true (the default) it will check **before** you publishLocal

When the SemVerPlugin halts the build it will look like:
```
[error] (api/*:semVerCheck) com.rallyhealth.sbt.semver.SemVerVersionTooLowException: Your changes have new functionality and binary incompatibilites which violates the http://semver.org rules for a Minor release.
[error]
[error] Specifically, MiMa detected the following binary incompatibilities:
[error] (1/1) Backward -> method url(java.lang.String)com.rallyhealth.rq.v1.RqRequest in object com.rallyhealth.rq.v1.Rq does not have a correspondent in current version
[error]
[error] These changes would require a Major release instead (1.9.0 => 2.0.0).
[error] You can adjust the version by adding the following setting:
[error]   gitVersioningSnapshotLowerBound in ThisBuild := "2.0.0"
[error] Total time: 0 s, completed Jul 13, 2018 3:49:57 PM
```

## Information

SemverPlugin is built on top of [Typesafe's migration-manager tool](https://github.com/typesafehub/migration-manager).
First it finds the previous version by looking at git history to find a previous tag (like GitVersioningPlugin).
Then it uses that artifact as the baseline and compares against your changes.

## Notes

* [Semantic versioning](http://semver.org/#spec-item-4) is disabled for initial development versions (i.e. 0.x.y).

* When adding a new sub-module within an existing module, be sure to add `semVerEnforceAfterVersion` in the sbt settings
 and that version is a minor update.

    Example: Current tag is 1.3.4. Then the sbt sub-module settings should have `semVerEnforceAfterVersion := Some("1.4.0")`
