package com.dg.advxml.core.funcs

import org.scalatest.WordSpec

class ActionsTest extends WordSpec {

  import com.dg.advxml.AdvXml._

  "Append node action" when {
    "Applied with right data" should {
      "Append new node to XML element" +
        "" in {

        val xml =
          <Persons>
            <Person Name="David" />
          </Persons>

        val expected =
          <Persons>
            <Person Name="David" />
            <Person Name="Alessandro"/>
          </Persons>
        val action = append(<Person Name="Alessandro"/>)
        val result = action(xml \ "Person")

        assert(result == expected)
      }
    }
  }
}
