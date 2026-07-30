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
package io.github.thomaskioko.gradle.plugins.extensions

/**
 * DSL marker for the `scaffold {}` extension hierarchy.
 *
 * Prevents nested DSL blocks (e.g. `scaffold { android { … } }`) from accidentally calling
 * methods on an outer-scope receiver.
 */
@DslMarker
public annotation class ScaffoldDsl
