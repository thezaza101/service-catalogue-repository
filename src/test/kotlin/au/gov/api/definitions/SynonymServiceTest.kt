package au.gov.api.definitions

import au.gov.api.MockDataSource
import au.gov.api.servicecatalogue.repository.definitions.SynonymRepository
import org.junit.Assert
import org.junit.Test


class SynonymServiceTest {
    var repository = SynonymRepository(MockDataSource())
    constructor(){
        var abnSyn = listOf<String>("australian business number","abn")
        var costSyn = listOf<String>("cost","debt")
        repository.saveSynonym(abnSyn)
        repository.saveSynonym(costSyn)
    }

    @Test
    fun Test_can_expand_simple_synonym() {
        val input = """"australian business number" not quoted"""
        val expected = """("australian business number"^1.2 abn) not quoted"""
        val expansionResults = repository.expand(input)
        Assert.assertEquals(expected, expansionResults.expandedQuery)
        Assert.assertEquals(mapOf("australian business number" to listOf("abn")), expansionResults.usedSynonyms)
    }

    @Test
    fun Test_token_modifiers() {
        val input = """"australian business number" not quoted"""
        val expected = """("australian business number"^1.2 abn) not quoted"""
        val expansionResults = repository.expand(input)
        Assert.assertEquals(expected, expansionResults.expandedQuery)
        Assert.assertEquals(mapOf("australian business number" to listOf("abn")), expansionResults.usedSynonyms)
    }

    @Test
    fun Test_right_synonym_gets_weighted() {
        val input = """abn not quoted"""
        val expected = """("australian business number" abn^1.2) not quoted"""
        val expansionResults = repository.expand(input)
        Assert.assertEquals(expected, expansionResults.expandedQuery)
        Assert.assertEquals(mapOf("abn" to listOf("australian business number")), expansionResults.usedSynonyms)

    }


    @Test
    fun Test_disjoint_synonyms() {
        val input = """"australian business number" not quoted cost"""
        val expected = """("australian business number"^1.2 abn) not quoted (cost^1.2 debt)"""
        val expansionResults = repository.expand(input)
        Assert.assertEquals(expected, expansionResults.expandedQuery)
        Assert.assertEquals(mapOf("australian business number" to listOf("abn"), "cost" to listOf("debt")), expansionResults.usedSynonyms)
    }

    @Test
    fun Test_unquoted_synonyms() {
        val input = "australian business number not quoted cost"
        val expected = """australian business number not quoted (cost^1.2 debt) ("australian business number"^1.2 abn)"""
        val expansionResults = repository.expand(input)
        Assert.assertEquals(expected, expansionResults.expandedQuery)
        Assert.assertEquals(mapOf("australian business number" to listOf("abn"), "cost" to listOf("debt")), expansionResults.usedSynonyms)
    }

    @Test
    fun Test_no_synonyms() {
        val input = "acn not quoted"
        val expected = "acn not quoted"
        val expansionResults = repository.expand(input)
        Assert.assertEquals(expected, expansionResults.expandedQuery)
        Assert.assertEquals(mapOf<String,List<String>>(), expansionResults.usedSynonyms)

    }

}
