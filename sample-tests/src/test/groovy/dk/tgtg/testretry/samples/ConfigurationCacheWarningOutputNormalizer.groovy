package dk.tgtg.testretry.samples

import org.gradle.exemplar.executor.ExecutionMetadata
import org.gradle.exemplar.test.normalizer.OutputNormalizer

class ConfigurationCacheWarningOutputNormalizer implements OutputNormalizer {
    @Override
    String normalize(String commandOutput, ExecutionMetadata executionMetadata) {
        return commandOutput.replaceAll(".*Test.getClassLoaderCache\\(\\) method has been deprecated.*\\R", "")
    }
}
