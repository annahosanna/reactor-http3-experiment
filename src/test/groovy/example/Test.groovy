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


class Test extends Specification{
    def "should be a simple assertion"() {
        expect:
        1 == 1
    }
}
