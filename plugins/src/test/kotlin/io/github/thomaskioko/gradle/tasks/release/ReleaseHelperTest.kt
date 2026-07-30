/*
 * Copyright 2026 Thomas Kioko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thomaskioko.gradle.tasks.release

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseHelperTest {

    @Test
    fun `buildTag production release`() {
        assertEquals("v0.2.0", ReleaseTask.buildTag("0.2.0", false, "main"))
    }

    @Test
    fun `buildTag beta with feature branch`() {
        assertEquals("v0.2.0-beta.feature-auth", ReleaseTask.buildTag("0.2.0", true, "feature/auth"))
    }

    @Test
    fun `buildTag beta with simple branch`() {
        assertEquals("v1.0.0-beta.develop", ReleaseTask.buildTag("1.0.0", true, "develop"))
    }

    @Test
    fun `sanitizeBranchForTag replaces special characters`() {
        assertEquals("fix-bug-1-test", ReleaseTask.sanitizeBranchForTag("fix/bug~1:test"))
    }

    @Test
    fun `sanitizeBranchForTag collapses consecutive dashes`() {
        assertEquals("a-b", ReleaseTask.sanitizeBranchForTag("a//b"))
    }

    @Test
    fun `sanitizeBranchForTag trims leading and trailing dashes`() {
        assertEquals("branch", ReleaseTask.sanitizeBranchForTag("/branch/"))
    }

    @Test
    fun `sanitizeBranchForTag preserves dots and underscores`() {
        assertEquals("release_1.0", ReleaseTask.sanitizeBranchForTag("release_1.0"))
    }

    @Test
    fun `sanitizeBranchForTag handles complex branch names`() {
        assertEquals("user-feat-add..thing", ReleaseTask.sanitizeBranchForTag("user/feat^add..thing"))
    }

    @Test
    fun `parsePorcelainPaths normal modified file`() {
        assertEquals(listOf("src/Foo.kt"), ReleaseTask.parsePorcelainPaths("M  src/Foo.kt"))
    }

    @Test
    fun `parsePorcelainPaths added file`() {
        assertEquals(listOf("new-file.txt"), ReleaseTask.parsePorcelainPaths("A  new-file.txt"))
    }

    @Test
    fun `parsePorcelainPaths rename`() {
        assertEquals(listOf("old.kt", "new.kt"), ReleaseTask.parsePorcelainPaths("R  old.kt -> new.kt"))
    }

    @Test
    fun `parsePorcelainPaths short line returns empty`() {
        assertEquals(emptyList<String>(), ReleaseTask.parsePorcelainPaths("M "))
    }

    @Test
    fun `parsePorcelainPaths untracked file`() {
        assertEquals(listOf("untracked.txt"), ReleaseTask.parsePorcelainPaths("?? untracked.txt"))
    }
}
