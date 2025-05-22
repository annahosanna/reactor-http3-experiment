package example

import spock.lang.Specification
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Retry
import spock.lang.Timeout
import spock.lang.IgnoreIf
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

// file:///Users/gk.meier/Desktop/Software-Projects/reactor-http3-experiment/build/reports/tests/test/index.html

class LittleFunctionSpec extends Specification{
	Context context
	Scriptable scope

	/**
	 * Setup, prior to every spec test
	 */
	void setup(){
		 context = Context.enter()

		// Set version to JavaScript1.2 so that we get object-literal style
		// printing instead of "[object Object]"
		context.setLanguageVersion(Context.VERSION_1_8)

		// Initialize the standard objects (Object, Function, etc.)
		// This must be done before scripts can be executed.
		scope = context.initStandardObjects()
	}

	/**
	 * Teardown method, run after each test. This just ensures we've left the Rhino context.
	 */
	void cleanup(){
		Context.exit();
	}

	/**
	 * Load a JavaScript file into the Rhino engine. For resources held within the project you will probably want a filename like:
	 * 		"src/main/js/componentX/script.js"
	 * @param fileName The name of the file to be loaded.
	 */
	void loadJSIntoContext(String fileName) {
		File emulatorFile = fileName as File
		context.evaluateString(scope, emulatorFile.text, emulatorFile.name, 1, null)
	}

	@Unroll
	def "check addTogether behaves for #a, #b, #c"(){
	  given: "I have littleFunction.js loaded"
			loadJSIntoContext("src/test/js/littleFunction.js")

			when: "I run the addTogether function for 1, 2, and 3"
			String jsExercise = "var result = addTogether("+a+","+b+","+c+");"
			context.evaluateString(scope, jsExercise, "TestScript", 1, null)

			then: "The result is #c"
			scope.get("result", scope) == (a + b + c)

			where:
			a   | b   | c
			0   | 0   | 0
			9   | 1   | 0
			5   | 0   | 5
			1   | 1   | 1
			0   | 4   | 24
			1231| 0   | 0
			9999| 0   | 4325
			0   | 035 | 230
		}
}
