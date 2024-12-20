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
package dk.tgtg.testretry;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.testing.Test;

import javax.inject.Inject;

import static dk.tgtg.testretry.internal.config.TestTaskConfigurer.configureTestTask;

public class TestRetryPlugin implements Plugin<Project> {

    private final ObjectFactory objectFactory;
    private final ProviderFactory providerFactory;

    @Inject
    TestRetryPlugin(ObjectFactory objectFactory, ProviderFactory providerFactory) {
        this.objectFactory = objectFactory;
        this.providerFactory = providerFactory;
    }

    @Override
    public void apply(Project project) {
        if (pluginAlreadyApplied(project)) {
            return;
        }

        project.getTasks()
            .withType(Test.class)
            .configureEach(task -> configureTestTask(task, objectFactory, providerFactory));
    }

    private static boolean pluginAlreadyApplied(Project project) {
        return project.getPlugins().stream().anyMatch(plugin -> plugin.getClass().getName().equals(TestRetryPlugin.class.getName()));
    }
}
