package com.rallyhealth.sbt.semver

import com.rallyhealth.sbt.util.NullSbtLogger
import com.rallyhealth.sbt.versioning.{ReleaseVersion, SemVerIdentifierList, SnapshotVersion}
import org.scalatest.FunSpec

class SemVerPluginUtilsSpec extends FunSpec {

  describe("calcTargetVersion") {

    describe("errors") {

      it("invalid versionOverride") {
        val versionFromGit = ReleaseVersion.initialVersion
        intercept[IllegalArgumentException] {
          SemVerPluginUtils.calcTargetVersion(Some("whatever"), "", versionFromGit, NullSbtLogger)
        }
      }

      it("dirty versionOverride") {
        val versionOverride = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
        val versionFromGit = ReleaseVersion.initialVersion
        intercept[IllegalArgumentException] {
          SemVerPluginUtils.calcTargetVersion(
            Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
        }
      }

      it("invalid semVerLimit") {
        val versionFromGit = ReleaseVersion.initialVersion
        intercept[IllegalArgumentException] {
          SemVerPluginUtils.calcTargetVersion(None, "whatever", versionFromGit, NullSbtLogger)
        }
      }

      it("dirty semVerLimit") {
        val versionFromGit = ReleaseVersion.initialVersion
        val semVerLimit = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = true)
        intercept[IllegalArgumentException] {
          SemVerPluginUtils.calcTargetVersion(
            None, semVerLimit.toString, versionFromGit, NullSbtLogger)
        }
      }
    }

    describe("precedence") {

      describe("versionFromGit") {

        it("only clean release") {
          val versionFromGit = ReleaseVersion.initialVersion
          val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
          assert(target.get.version === versionFromGit)
        }

        it("only dirty release") {
          val versionFromGit = ReleaseVersion.initialVersion.copy(isDirty = true)
          val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
          assert(target.isEmpty)
        }

        it("only clean snapshot") {
          val versionFromGit = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
          val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
          assert(target.isEmpty)
        }

        it("only dirty snapshot") {
          val versionFromGit = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
          val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
          assert(target.isEmpty)
        }
      }

      describe("versionOverride has first precedence") {

        it("versionOverride with semVerLimit") {
          val versionOverride = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
          val versionFromGit = ReleaseVersion.initialVersion
          val target = SemVerPluginUtils.calcTargetVersion(
            Some(versionOverride.toString), semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
          assert(target.get.version === versionOverride)
        }

        it("versionOverride without semVerLimit") {
          val versionOverride = ReleaseVersion(1, 2, 3, SemVerIdentifierList.empty, isDirty = false)
          val versionFromGit = ReleaseVersion.initialVersion
          val target = SemVerPluginUtils.calcTargetVersion(
            Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
          assert(target.get.version === versionOverride)
        }
      }

      it("clean release versionFromGit has second precedence") {
        val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val versionFromGit = ReleaseVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val target = SemVerPluginUtils.calcTargetVersion(
          None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
        assert(target.get.version === versionFromGit)
      }

      it("semVerLimitVersion has third precedence") {
        val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val versionFromGit = SnapshotVersion(1, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
          .nextVersion()
        val target = SemVerPluginUtils.calcTargetVersion(
          None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
        assert(target.get.version === semVerLimitVersion)
      }
    }

    describe("versionOverride vs semVerLimit") {

      it("versionOverride == semVerLimit (limit will updating next time)") {
        val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val versionFromGit = ReleaseVersion.initialVersion
        val target = SemVerPluginUtils.calcTargetVersion(
          Some(versionOverride.toString), semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
        assert(target.get.version === versionOverride)
      }

      it("versionOverride < semVerLimit") {
        val versionOverride = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
        val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val versionFromGit = ReleaseVersion.initialVersion
        val target = SemVerPluginUtils.calcTargetVersion(
          Some(versionOverride.toString), semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
        assert(target.get.version === versionOverride)
      }

      it("versionOverride > semVerLimit (limit needs updating)") {
        val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
        val semVerLimitVersion = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
        val versionFromGit = ReleaseVersion.initialVersion
        intercept[IllegalArgumentException] {
          SemVerPluginUtils.calcTargetVersion(
            Some(versionOverride.toString), semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
        }
      }
    }

    describe("versionFromGit") {

      describe("is release") {

        describe("clean") {

          describe("versionOverride") {

            it("only versionFromGit") {
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.get.version == versionFromGit)
            }

            it("== (tagged before publish, or re-publishing an existing tag)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val target = SemVerPluginUtils
                .calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              assert(target.get.version === versionOverride)
            }

            it("< (downgrade attempt)") {
              val versionOverride = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("> (common scenario)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val target = SemVerPluginUtils
                .calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              assert(target.get.version === versionOverride)
            }
          }

          describe("semVerLimit") {

            it("only versionFromGit") {
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.get.version == versionFromGit)
            }

            it("== (tagged before updating limit)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val target = SemVerPluginUtils.calcTargetVersion(
                None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              assert(target.get.version === versionFromGit)
            }

            it("< (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("> (common scenario)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val target = SemVerPluginUtils.calcTargetVersion(
                None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              assert(target.get.version === versionFromGit)
            }
          }
        }

        describe("dirty") {

          describe("versionOverride") {

            it("only versionFromGit") {
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = true)
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.isEmpty)
            }

            it("== (attempt to publish a different commit with the same version)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("< (downgrade attempt)") {
              val versionOverride = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("> (publishing dirty commits using versionOverride is a bad idea)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = true)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }
          }

          describe("semVerLimit") {

            it("only versionFromGit") {
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = true)
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.isEmpty)
            }

            it("== (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("< (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true)
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("> (common scenario)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = true)
              val target = SemVerPluginUtils.calcTargetVersion(
                None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              assert(target.get.version === semVerLimitVersion)
            }
          }
        }
      }

      describe("is snapshot") {

        // these tests call SnapshotVersion.nextVersion() because GitDriver.calcCurrentVersion() calls that on
        // snapshots before returning them

        describe("clean") {

          describe("versionOverride") {

            it("only versionFromGit") {
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.isEmpty)
            }

            it("== (attempt to publish a different commit with the same version)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("< (downgrade attempt)") {
              val versionOverride = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("> (common scenario)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils
                .calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              assert(target.get.version === versionOverride)
            }
          }

          describe("semVerLimit") {

            it("only versionFromGit") {
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.isEmpty)
            }

            it("== (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("< (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("> (common scenario)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils.calcTargetVersion(
                None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              assert(target.get.version === semVerLimitVersion)
            }
          }
        }

        describe("dirty") {

          describe("versionOverride") {

            it("only versionFromGit") {
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.isEmpty)
            }

            it("== (attempt to publish a different commit with the same version)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("< (downgrade attempt)") {
              val versionOverride = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }

            it("> (publishing dirty SNAPSHOTs using versionOverride a typical local development practice)") {
              val versionOverride = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(Some(versionOverride.toString), "", versionFromGit, NullSbtLogger)
              }
            }
          }

          describe("semVerLimit") {

            it("only versionFromGit") {
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils.calcTargetVersion(None, "", versionFromGit, NullSbtLogger)
              assert(target.isEmpty)
            }

            it("== (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("< (limit needs updating)") {
              val semVerLimitVersion = ReleaseVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              intercept[IllegalArgumentException] {
                SemVerPluginUtils.calcTargetVersion(None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              }
            }

            it("> (common scenario)") {
              val semVerLimitVersion = ReleaseVersion(2, 0, 0, SemVerIdentifierList.empty, isDirty = false)
              val versionFromGit = SnapshotVersion(1, 9, 9, SemVerIdentifierList.empty, isDirty = true, "0123abc", 1)
                .nextVersion()
              val target = SemVerPluginUtils.calcTargetVersion(
                None, semVerLimitVersion.toString, versionFromGit, NullSbtLogger)
              assert(target.get.version === semVerLimitVersion)
            }
          }
        }
      }
    }
  }
}
