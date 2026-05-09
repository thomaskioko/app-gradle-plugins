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
