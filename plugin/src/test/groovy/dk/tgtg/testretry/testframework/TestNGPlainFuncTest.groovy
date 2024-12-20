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
package dk.tgtg.testretry.testframework

import dk.tgtg.testretry.AbstractPluginFuncTest

import javax.annotation.Nullable

class TestNGPlainFuncTest extends BaseTestNGFuncTest {
    @Override
    String reportedLifecycleMethodName(String gradleVersion, TestNGLifecycleType lifecycleType, String methodName) {
        methodName
    }

    @Override
    String reportedParameterizedMethodName(String gradleVersion, String methodName, String paramType, int invocationNumber, @Nullable String paramValueRepresentation) {
        "${methodName}[${invocationNumber}]${paramValueRepresentation ? "(${paramValueRepresentation})" : ""}"
    }

    @Override
    boolean reportsSuccessfulLifecycleExecutions(TestNGLifecycleType lifecycleType) {
        true
    }

    /**
     * If JUnit's TestNG engine is used, then tests won't even run and the failure is silently swallowed.
     */
    def "does not handle flaky static initializers (gradle version #gradleVersion)"() {
        given:
        buildFile << """
            test.retry.maxRetries = 1
        """

        writeJavaTestSource """
            package acme;

            public class SomeTests {

                static {
                    ${AbstractPluginFuncTest.flakyAssert()}
                }

                @org.testng.annotations.Test
                public void someTest() {}
            }
        """

        when:
        def result = gradleRunner(gradleVersion as String).buildAndFail()

        then:
        with(result.output) {
            it.contains('There were failing tests. See the report')
            !it.contains('The following test methods could not be retried')
        }

        where:
        gradleVersion << AbstractPluginFuncTest.GRADLE_VERSIONS_UNDER_TEST
    }
}
