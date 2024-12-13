/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused")

package dk.tgtg.kotlin.dsl

import org.gradle.api.tasks.testing.Test
import dk.tgtg.testretry.TestRetryTaskExtension
import org.gradle.kotlin.dsl.the

val Test.retry: TestRetryTaskExtension
    get() = the()

fun Test.retry(configure: TestRetryTaskExtension.() -> Unit) =
    extensions.configure(TestRetryTaskExtension.NAME, configure)
