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
package io.github.thomaskioko.gradle.plugins.properties

internal object PropertyKeys {
    const val APP_DEBUG_ONLY: String = "app.debugOnly"
    const val APP_ENABLE_IOS: String = "app.enableIos"
    const val APP_VERSION_SUFFIX: String = "app.versionSuffix"

    const val COMPOSE_REPORTS: String = "compose.enableCompilerReports"

    const val PACKAGE_NAME: String = "package.name"

    const val JAVA_TOOLCHAINS_STRICT: String = "java.toolchains.strict"

    const val RELEASE_TYPE: String = "type"
    const val RELEASE_BETA: String = "beta"
    const val RELEASE_DRY_RUN: String = "dryRun"

    const val RELEASE_STORE_FILE: String = "releaseStoreFile"
    const val RELEASE_STORE_PASSWORD: String = "releaseStorePassword"
    const val RELEASE_KEY_ALIAS: String = "releaseKeyAlias"
    const val RELEASE_KEY_PASSWORD: String = "releaseKeyPassword"
}
