package au.gov.api.definitions

import au.gov.api.MockDataSource
import au.gov.api.repository.definitions.Direction
import au.gov.api.repository.definitions.Meta
import au.gov.api.repository.definitions.RelationshipRepository
import au.gov.api.repository.definitions.Result
import org.junit.Assert
import org.junit.Test

class RelationshipServiceTest {
    private var service = RelationshipRepository(MockDataSource())
    constructor(){
        service.addMetas(Meta("skos:member", true, mapOf(Direction.TO to "is member of", Direction.FROM to "has member")))
        service.addMetas(Meta("rdfs:seeAlso", false, mapOf(Direction.UNDIRECTED to "see also")))

        service.saveRelationship(RelationshipRepository.NewRelationship("rdfs:seeAlso", Direction.UNDIRECTED, arrayOf("Blah","something")))
        service.saveRelationship(RelationshipRepository.NewRelationship("rdfs:seeAlso", Direction.FROM, arrayOf("Blah","something")))
        service.saveRelationship(RelationshipRepository.NewRelationship("rdfs:seeAlso", Direction.FROM, arrayOf("Blah","something")))
        service.saveRelationship(RelationshipRepository.NewRelationship("skos:member", Direction.TO, arrayOf("Blah","something")))
    }
    @Test
    fun test_can_get_relationships() {
        val input = "Blah"
        val result = service.getRelationshipFor(input)
        Assert.assertEquals(2, result.size)
        Assert.assertTrue(result.containsKey("rdfs:seeAlso"))
        val seeAlso = result["rdfs:seeAlso"]
        Assert.assertEquals(3, seeAlso!!.size)
        Assert.assertEquals(Direction.TO, seeAlso[0].direction)

        Assert.assertTrue(result.containsKey("skos:member"))
        val member = result["skos:member"]
        Assert.assertEquals(1, member!!.size)
        Assert.assertEquals(Direction.TO, member[0].direction)
        Assert.assertEquals("is member of", member[0].meta.verbs[member[0].direction])
    }


}