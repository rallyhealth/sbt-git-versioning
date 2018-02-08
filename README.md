**git-versioning-sbt-plugin** is an sbt plugin designed to make maintaining a simple, consistent, and accurate
 [semantic versioning](http://semver.org/spec/v2.0.0.html) scheme with as little manual labor as possible.

There are two sbt plugins in this one plugin library:
* [GitVersioningPlugin](src/main/scala/com/rallyhealth/sbt/versioning/GitVersioningPlugin.scala)
* [SemVerPlugin](src/main/scala/com/rallyhealth/sbt/semver/SemVerPlugin.scala)

# Compatibility

Tested and supported for sbt version 0.13.x and above.

# Install

1. Remove the `version := ...` directive from the project's `build.sbt` file
2. Add the following code to `project/plugins.sbt` file (if you want to use Ivy style resolution):
```scala
  resolvers += Resolver.url(
    "Rally Plugin Releases",
    url("https://dl.bintray.com/rallyhealth/sbt-plugins"))(Resolver.ivyStylePatterns)
```
Alternatively, you can add the following (if you want to use Maven style resolution):
```scala
  Resolver.bintrayRepo("rallyhealth", "sbt-plugins")
```
In either case, you should now be able to add the plugin dependency to `project/plugins.sbt`:
```scala
  addSbtPlugin("com.rallyhealth.sbt" % "git-versioning-sbt-plugin" % "x.y.z")
```
3. Enable the plugin and set `semVerLimit` in `build.sbt` (see
 [below](https://github.com/rallyhealth/git-versioning-sbt-plugin#semverlimit)
 for details)

```scala
val example = project
  .enablePlugins(SemVerPlugin)
  // See https://github.com/rallyhealth/git-versioning-sbt-plugin#semverlimit
  .settings(semVerLimit := "x.y.z")
```

# GitVersioningPlugin

This [plugin](src/main/scala/com/rallyhealth/sbt/versioning/GitVersioningPlugin.scala)
focuses on automatically determining the value of the `version` setting. The `version` is determined by looking at git history (for
previous tags) and the state of the working directly. Read on for the exact details.

## Usage

This plugin will automatically determine and set the `version` setting on startup. There is nothing developers need to
explicitly do. You can use `sbt publishLocal` to create a local snapshot artifact or, if you have a snapshot repository,
you can `sbt publish` at the end of a continuous integration pipeline that is triggered on pull requests, so that you
can easily pull one snapshot into another for testing.

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

### Release arg Property

The release arg bumps the version up by a major, minor, or patch increment.
```
sbt -Drelease=major ...
```

The release arg alters the value of `versionFromGit`, but is still bounded by `gitVersioningSnapshotLowerBound`. For example:

| versionFromGit | -Drelease | gitVersioningSnapshotLowerBound | Final Version |
| -------------- | --------- | --------------------------------- | ------------- |
| 1.0.0          | patch     |                                   | 1.0.1         |
| 1.0.0          | minor     |                                   | 1.1.0         |
| 1.0.0          | major     |                                   | 2.0.0         |
| 1.0.0          | patch     | 2.0.0                             | 2.0.0-n-0123abc-SNAPSHOT |
| 1.0.0          | minor     | 2.0.0                             | 2.0.0-n-0123abc-SNAPSHOT |
| 1.0.0          | major     | 2.0.0                             | 2.0.0         |

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
commits.
* The hash does **not** have a 'g' prefix like the output of `git describe`
* A build will be flagged as not clean (and will have a `-dirty-SNAPSHOT` identifier applied) if
`git status --porcelain` returns a non-empty result.
* If there is no tag in the commit history, the number appended after the version number will reflect the number of commits
since the creation of the repository.
* This plugin is intentionally different than something like [sbt-release](https://github.com/sbt/sbt-release) which stores
the version in a `version.sbt` file. Those types of plugins require more manual effort on the part of the developer.

*Warning:* Git Versioning may misbehave with shallow clones. If the incorrect version is being returned and tags are
accessible, you may be using a shallow clone, in which case `git fetch --unshallow` will fix the issue. On CI systems,
ensure that any git plugins are configured to not use shallow clones.

## Creating a Release

Creating a release is done by tagging and then publishing a release...
 ```
git tag v1.2.3
sbt publish[Local]
```

...or by passing a [release arg](#release-arg-property).
```
sbt -Drelease=patch publish[Local]
```

...or by overriding the version that will be applied to a specific build, using the
`version.override` setting. Typically this is done by at the command line to avoid changing `build.sbt`.
```
sbt -Dversion.override=1.2.3 publish[Local]
```

## Extra Identifiers

To add an extra identifier like "-alpha" or "-rc1" or "-rally" it must be included it in the version directly
by overriding the "version" setting directly. (There was a feature to add those separately but it has been
removed because it was never used. Feel free to re-add it.)

# SemVerPlugin

This [plugin](src/main/scala/com/rallyhealth/sbt/semver/SemVerPlugin.scala)
will halt the build (compile, publish, etc.) if your changes would not make a valid [semantic version](http://semver.org/spec/v2.0.0.html).
The plugin checks the previous release, your changes, and the `semVerLimitRelease` to ensure your changes are really
patch/minor/major changes if you want to release a patch/minor/major.

## Usage

You can run the check manually using `semVerCheck`. The check and also be run automatically:

* *Compile* - If `semVerCheckOnCompile` is set to true (the default) it will check after you compile
* *Test* - If `semVerCheckOnTest` is set to true (the default) it will check after you test
* *Publish* - If `semVerCheckOnPublish` is set to true (the default) it will check **before** you publish
* *PublishLocal* - If `semVerCheckOnPublish` is set to true (the default) it will check **before** you publishLocal

The output will look like this:
```
[info] [SemVer] Checking ENABLED with LIMIT target semVerLimit=3.0.0
[info] [SemVer] Check starting (prev=2.0.0 vs curr=3.0.0) ...
[warn] [SemVer] Errors total=4, backward=0, forward=4, required diffType=minor
[warn] [SemVer] (1/4) Forward -> method calamity()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[warn] [SemVer] (2/4) Forward -> method princessLuna()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[warn] [SemVer] (3/4) Forward -> method starlightGlimmer()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[warn] [SemVer] (4/4) Forward -> method velvetRemedy()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[warn] [SemVer] version=3.0.0 PASSED: required diffType=Minor achieved
[success] Total time: 2 s, completed Nov 15, 2016 3:38:48 PM
```

When the SemVerPlugin halts the build it will look like:
```
[info] [SemVer] Checking ENABLED with LIMIT target semVerLimit=2.0.1
[info] [SemVer] Check starting (prev=2.0.0 vs curr=2.0.1) ...
[error] [SemVer] Errors total=4, backward=0, forward=4, required diffType=minor
[error] [SemVer] (1/4) Forward -> method calamity()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[error] [SemVer] (2/4) Forward -> method princessLuna()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[error] [SemVer] (3/4) Forward -> method starlightGlimmer()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[error] [SemVer] (4/4) Forward -> method velvetRemedy()Double in class com.rallyhealth.semver.Equestria does not have a correspondent in previous version
[error] [SemVer] version=2.0.1 FAILED: required diffType=minor NOT achieved
java.lang.IllegalStateException: Proposed RELEASE version=2.0.1 FAILED SemVer rules, failing build
	at com.rallyhealth.semver.SemVerHalter.haltBuild(SemVerHalter.scala:86)
    ...
[error] (*:compile) java.lang.IllegalStateException: Proposed RELEASE version=2.0.1 FAILED SemVer rules, failing build
[error] Total time: 2 s, completed Nov 15, 2016 3:27:36 PM
```

### semVerLimit

Before using the check (manually or automatically), you *must* set the `semVerLimit` key in your `build.sbt`.
This is a version (e.g. "1.2.3") that tells SemVerPlugin when to halt the build. It prevents you from making any
compatibility changes that would *exceed* that version.

This is best explained with an example. Let's assume the latest tag is `1.2.3`:

* `semVerLimit := 1.2.3` - you won't be able to make *any* changes
* `semVerLimit := 1.2.**999**` - you will be allowed to make *patch* changes.
* `semVerLimit := 1.**999.999**` - you will be allowed to make *minor or patch* changes.
* `semVerLimit := ""` - you will be allowed to make *major, minor, or patch* changes (`semVerLimit` is disabled)

Here are a few stories to illustrate how `semVerLimit` works.

#### Patch Change

1. Version is `1.2.3` and `semVerLimit := 1.9.9`:
2. Fluttershy needs to fix a bug (patch change).
3. Fluttershy fixes, sees no backward or forward errors from SemVerPlugin.
4. Fluttershy commits, PRs, and merges.
5. Fluttershy successfully runs the release job for `1.2.4`.

#### Minor Change (with a human error)

1. Version is `1.2.4` (from above) and `semVerLimit := 1.9.9`:
2. Applejack wants to add a new method (minor change).
3. Applejack adds the new method, commits, PRs, and merges.
4. Applejack runs the release job for `1.2.4` but the release job fails.
5. SemVerPlugin's errors in the logs say that she needs to make a `minor` release (e.g. `1.3.0`).
6. Applejack successfully runs the release job for `1.3.0`.

#### A 'Big' Patch Change

1. Version is `1.3.0` (from above) and `semVerLimit := 1.9.9`:
2. Twilight Sparkle found a potential bug for an edge case, e.g. some
'undefined' behavior. She only needs to make a *patch* change, but
someone could be relying on that 'undefined' behavior, she wants to release
a *minor* change
3. Twilight Sparkle fixes, sees no backward or forward errors from SemVerPlugin.
4. Twilight Sparkle commits, PRs, and merges.
5. Twilight Sparkle successfully runs the release job for `1.**4**.0` (*minor*
change even though only *patch* was required)

#### Hot Fix (patch change)

1. Version is `1.4.0` (from above) and `semVerLimit := 1.9.9`:
2. Rainbow Dash needs to make a hot fix.
3. Rainbow Dash changes `semVerLimit := 1.4.1` so she's absolutely sure no minor changes sneak in.
4. Rainbow Dash makes her fix, commits, PRs, and merges
5. Rainbow Dash successfully runs the release job for `1.4.1`.
6. *Unfortunately the next committer will need to reset `semVerLimit := 1.9.9`*
7. Rainbow Dash helpfully merges a PR to revert `semVerLimit := 1.9.9`

#### Hot Fix Redux (patch change)

1. Version is `1.4.1` (from above) and `semVerLimit := 1.9.9`:
2. Pinkie Pie needs to make another hot fix.
3. Pinkie Pie does not want to do a clean up commit like Rainbow Dash.
4. Pinkie Pie runs SBT and executes ```> set semVerLimit := 1.4.2``` before any other command.
5. Pinkie Pie makes her fix, commits, PRs
6. Pinkie Pie successfully runs the release job for `1.4.2`.

#### Major Change

1. Version is `1.4.2` (from above) and `semVerLimit := 1.9.9`:
2. Rarity wants to make a major breaking API change.
3. Rarity changes `semVerLimit := 2.0.0` so she can release `2.9.9` when she is done.
4. Rarity makes her API change, commits, PRs, and merges.
5. Rarity successfully runs the release job for `2.0.0`.

#### What should you choose for `semVerLimit`?

* First, talk to the team working on the service, or the users of the library. What do they want?
* If you want to allow any backward compatible changes might choose a limit like `semVerLimit := 1.999.999`
* If you want to avoid changes you might choose a limit like `semVerLimit := 1.0.999`
* If you don't care about *any* changes you can disable the limt using `semVerLimit := ""`.

## Information

SemverPlugin is built on top of [Typesafe's migration-manager tool](https://github.com/typesafehub/migration-manager).
First it finds the previous version by looking at git history to find a previous tag (like GitVersioningPlugin).
Then it uses that artifact as the baseline and compares against your changes.

## Notes

* [Semantic versioning](http://semver.org/#spec-item-4) is disabled for pre-releases (i.e. 0.x.y). You can enable
 checking if you want by setting `semVerPreRelease := true`
