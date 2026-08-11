package planning.steps

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber test runner for the planner-gradle borough (PLN-VIBE-5).
 *
 * Glue lives in [planning.steps] so the cucumber convention plugin can
 * exclude `*.scenarios.*` from the unit `test` task. Tags excluded from
 * the normal check:
 * - @integration: scenarios requiring a real LLM (Ollama) — run manually
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "planning.steps")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber.html, json:build/reports/cucumber.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @integration")
class CucumberTestRunner