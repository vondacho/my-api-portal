package io.obya.api.onboarding.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * JUnit Platform suite that discovers and runs the Cucumber feature files describing
 * the behaviour of {@link io.obya.api.onboarding.appl.usecase.RegistrationService}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("io/obya/api/onboarding/bdd")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.obya.api.onboarding.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class RunRegistrationCucumberTest {
}
