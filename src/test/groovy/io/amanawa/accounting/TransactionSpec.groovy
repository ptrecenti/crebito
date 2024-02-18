package io.amanawa.accounting


import com.fasterxml.jackson.jr.ob.JSON
import spock.lang.Specification
import spock.lang.Unroll

class TransactionSpec extends Specification {

    @Unroll
    def "Should Serialize"() {
        given:
        Bank.Transaction transaction = Bank.Transaction.fromMap(JSON.std.mapFrom(new ByteArrayInputStream(payload.bytes)))

        when:
        def valid = transaction.valid()


        then:
        valid == expected

        where:
        payload                                                                   | expected
        """{"valor": 1.2, "tipo": "d", "descricao": "devolve"}"""                 | false
        """{"valor": 1, "tipo": "x", "descricao": "devolve"}"""                   | false
        """{"valor": 1, "tipo": "c", "descricao": "123456789 e mais um pouco"}""" | false
        """{"valor": 1, "tipo": "c", "descricao": ""}"""                          | false
        """{"valor": 1, "tipo": "c", "descricao": null}"""                        | false
        """{"valor": 1, "tipo": "c", "descricao": "descricai"}"""                 | true
    }
}
