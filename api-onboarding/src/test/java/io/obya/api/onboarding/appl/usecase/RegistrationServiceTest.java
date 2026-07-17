package io.obya.api.onboarding.appl.usecase;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 This test validates the conformance of the Registration use case implementation against the functional contract.
 The scope is the application layer.
 The SUT is the RegistrationService.
 The setup includes Cucumber playing the functional scenarios against the SUT backed by both a mocked Registry
 and a mocked Scorer engine.
 The test verifies that the SUT behaves as expected during acceptance scenarios examples.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("feature/registration")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.obya.api.onboarding.appl.usecase")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class RegistrationServiceTest {
}
