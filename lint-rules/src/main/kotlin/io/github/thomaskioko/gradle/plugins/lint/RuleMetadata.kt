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
package io.github.thomaskioko.gradle.plugins.lint

import com.pinterest.ktlint.rule.engine.core.api.Rule

/**
 * Shared [Rule.About] metadata that every rule in this set carries. ktlint surfaces this in
 * documentation links and in the help text printed when a rule fires, so a contributor can find
 * the source repository and issue tracker without leaving the terminal.
 */
internal val RULE_ABOUT: Rule.About = Rule.About(
    maintainer = "Thomas Kioko",
    repositoryUrl = "https://github.com/thomaskioko/app-gradle-plugins",
    issueTrackerUrl = "https://github.com/thomaskioko/app-gradle-plugins/issues",
)
