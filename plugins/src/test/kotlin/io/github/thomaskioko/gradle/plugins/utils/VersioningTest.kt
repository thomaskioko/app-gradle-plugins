package io.github.thomaskioko.gradle.plugins.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VersioningTest {

  @Test
  fun `should return 100000 given version 0_1_0`() {
    assertEquals(100_000, Versioning.compute("0.1.0"))
  }

  @Test
  fun `should return 10203000 given version 1_2_3`() {
    assertEquals(10_203_000, Versioning.compute("1.2.3"))
  }

  @Test
  fun `should return 1000 given version 0_0_1`() {
    assertEquals(1_000, Versioning.compute("0.0.1"))
  }

  @Test
  fun `should stay under 2_1B given max version 209_99_99`() {
    val result = Versioning.compute("209.99.99")
    assertEquals(2_099_999_000, result)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when version format is invalid`() {
    Versioning.compute("1.2")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when major version is above 209`() {
    Versioning.compute("210.0.0")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when minor version is above 99`() {
    Versioning.compute("1.100.0")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when patch version is above 99`() {
    Versioning.compute("1.0.100")
  }

  @Test
  fun `should increment patch when bump type is patch`() {
    val result = Versioning.bump("0.1.0", "patch")
    assertEquals("0.1.1", result.versionName)
    assertEquals(101_000, result.buildNumber)
  }

  @Test
  fun `should increment minor and reset patch when bump type is minor`() {
    val result = Versioning.bump("0.1.0", "minor")
    assertEquals("0.2.0", result.versionName)
    assertEquals(200_000, result.buildNumber)
  }

  @Test
  fun `should increment major and reset minor and patch when bump type is major`() {
    val result = Versioning.bump("0.1.0", "major")
    assertEquals("1.0.0", result.versionName)
    assertEquals(10_000_000, result.buildNumber)
  }

  @Test
  fun `should increment patch given non-zero version values`() {
    val result = Versioning.bump("1.2.3", "patch")
    assertEquals("1.2.4", result.versionName)
    assertEquals(10_204_000, result.buildNumber)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when bump type is invalid`() {
    Versioning.bump("0.1.0", "hotfix")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when version contains non-numeric component`() {
    Versioning.compute("1.2.abc")
  }

  @Test
  fun `should include invalid component in error message`() {
    val e = assertThrows(IllegalArgumentException::class.java) {
      Versioning.compute("1.2.abc")
    }
    assert(e.message!!.contains("'abc'")) { "Expected error to mention 'abc', got: ${e.message}" }
    assert(e.message!!.contains("1.2.abc")) { "Expected error to mention '1.2.abc', got: ${e.message}" }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when version contains negative number`() {
    Versioning.compute("-1.0.0")
  }

  @Test
  fun `should handle patch bump at high patch value`() {
    val result = Versioning.bump("0.99.98", "patch")
    assertEquals("0.99.99", result.versionName)
    assertEquals(9_999_000, result.buildNumber)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when patch bump exceeds limit`() {
    Versioning.bump("0.99.99", "patch")
  }

  @Test
  fun `should add beta number to version code`() {
    assertEquals(100_003, Versioning.compute("0.1.0", betaNumber = 3))
  }

  @Test
  fun `should return base version code when beta number is zero`() {
    assertEquals(100_000, Versioning.compute("0.1.0", betaNumber = 0))
  }

  @Test
  fun `should support max beta number 999`() {
    assertEquals(100_999, Versioning.compute("0.1.0", betaNumber = 999))
  }

  @Test
  fun `should produce beta code less than next patch`() {
    val beta = Versioning.compute("0.1.0", betaNumber = 999)
    val nextPatch = Versioning.compute("0.1.1")
    assert(beta < nextPatch) { "Beta code $beta should be less than next patch code $nextPatch" }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when beta number exceeds 999`() {
    Versioning.compute("0.1.0", betaNumber = 1000)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when beta number is negative`() {
    Versioning.compute("0.1.0", betaNumber = -1)
  }
}
