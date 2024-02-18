package io.amanawa.cache


import spock.lang.Specification

class FixedSizeLruMapSpec extends Specification {

    def "should remove"() {
        given:
        FixedSizeLruMap<Long, String> lru = new FixedSizeLruMap<>(2)
        when:
        lru.put(1, "One")
        lru.put(2, "Two")
        lru.put(3, "Tree")

        then:
        lru.entrySet().containsAll([2: "Two", 3: "Tree"].entrySet())
        lru.size() == 2

    }
}
