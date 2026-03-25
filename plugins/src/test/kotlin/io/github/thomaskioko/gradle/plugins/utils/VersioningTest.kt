package io.github.thomaskioko.gradle.plugins.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals(2_099_999_000, Versioning.compute("209.99.99"))
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

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when version contains non-numeric component`() {
    Versioning.compute("1.2.abc")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when version contains negative number`() {
    Versioning.compute("-1.0.0")
  }

  @Test
  fun `should increment patch when bump type is patch`() {
    assertEquals("0.1.1", Versioning.bump("0.1.0", "patch"))
  }

  @Test
  fun `should increment minor and reset patch when bump type is minor`() {
    assertEquals("0.2.0", Versioning.bump("0.1.0", "minor"))
  }

  @Test
  fun `should increment major and reset minor and patch when bump type is major`() {
    assertEquals("1.0.0", Versioning.bump("0.1.0", "major"))
  }

  @Test
  fun `should increment patch given non-zero version values`() {
    assertEquals("1.2.4", Versioning.bump("1.2.3", "patch"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when bump type is invalid`() {
    Versioning.bump("0.1.0", "hotfix")
  }

  @Test
  fun `should handle patch bump at high patch value`() {
    assertEquals("0.99.99", Versioning.bump("0.99.98", "patch"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `should throw when patch bump exceeds limit`() {
    Versioning.bump("0.99.99", "patch")
  }

  @Test
  fun `should produce higher build number after patch bump`() {
    val before = Versioning.compute("0.1.2")
    val newVersion = Versioning.bump("0.1.2", "patch")
    val after = Versioning.compute(newVersion)
    assertTrue("Expected $after > $before", after > before)
  }
}
