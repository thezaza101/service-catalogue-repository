package au.gov.api.repository.definitions

import au.gov.api.repository.QueryLogger
import au.gov.api.repository.RepositoryException
import com.beust.klaxon.Debug
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.RAMDirectory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

data class SearchResults(val results: List<Definition>, val howManyResults: Int, val usedSynonyms: Map<String, List<String>>? = null)

class Domain(val name: String, @JsonIgnore val _acronym: String, val version: String) {
    var acronym: String = _acronym.toLowerCase()
}

class Definition {

    constructor() {}
    constructor(def: NewDefinition) {
        name = def.name
        domain = def.domain
        status = def.status
        definition = def.definition
        guidance = def.guidance
        identifier = def.identifier
        usage = def.usage
        type = def.datatype.type
        values = def.values
        facets = def.datatype.facets
        domainAcronym = def.domainAcronym
        sourceURL = def.sourceURL
    }

    constructor(iname: String, idomain: String, istatus: String, idefinition: String, iguidance: String,
                iidentifier: String, iusage: Array<String>, itype: String, ivalues: Array<String>,
                ifacets: Map<String, String>, idomainAcronym: String, isourceURL: String = "") {
        name = iname
        domain = idomain
        status = istatus
        definition = idefinition
        guidance = iguidance
        identifier = iidentifier
        usage = iusage
        type = itype
        values = ivalues
        facets = ifacets
        domainAcronym = idomainAcronym
        sourceURL = isourceURL
    }

    var name: String = ""
    var domain: String = ""
    var status: String = ""
    var definition: String = ""
    var guidance: String = ""
    var identifier: String = ""
    var usage: Array<String> = arrayOf()
    var type: String = ""
    var values: Array<String> = arrayOf()
    var facets: Map<String, String> = mapOf()
    var domainAcronym: String = ""
    var sourceURL: String = ""
}

data class DataType(var type: String = "", var facets: Map<String, String> = mapOf())

data class NewDefinition(
        var name: String = "",
        var domain: String = "",
        var status: String = "",
        var definition: String = "",
        var guidance: String = "",
        var identifier: String = "",
        var usage: Array<String> = arrayOf(),
        var values: Array<String> = arrayOf(),
        var datatype: DataType = DataType("", mapOf()),
        var domainAcronym: String = "",
        var sourceURL: String = "",
        var version: String = ""
)

@ResponseStatus(HttpStatus.NOT_FOUND)
class DefinitionNotFoundException(id: String?) : Exception(id)

@Component
class DefinitionRepository {


    @Autowired
    lateinit var dataSource: DataSource

    data class DBDef(val definition: JsonObject, val source: String)

    private var definitions: MutableList<Definition> = mutableListOf()
    private var id2definitions: MutableMap<String, Definition> = mutableMapOf()
    private var domains: MutableMap<String, Domain> = mutableMapOf()

    @Autowired
    private lateinit var synonymService: SynonymRepository

    @Autowired
    private lateinit var queryLog: QueryLogger

    // lucene
    private var analyzer = StandardAnalyzer()
    private var index = RAMDirectory()
    private var config = IndexWriterConfig(analyzer)
    private var indexWriter = IndexWriter(index, config)

    constructor() {}

    constructor(theDataSource: DataSource) {
        dataSource = theDataSource
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initialise() {
        addJsonDefinitions()
        addJsonLDDefinitions()
    }

    private fun capPageSize(size: Int): Int = when (size < 100) {
        true -> size
        false -> 100
    }

    fun domainExists(acronym: String): Boolean = domains.containsKey(acronym)

    private fun addJsonLDDefinitions() {
        /*for(jsonld in listOf<String>()){//"agift"
            val json = JsonLd("/definitions/jsonld/$jsonld.json")
            val domain = json.domain
            domains[domain.acronym] = domain
            json.definitions.map{ addDefinitionToIndexes(it, domain.acronym) }
            indexWriter.commit()
        }*/
    }

    private fun addJsonDefinitions() {
        var allDomains = getDomainsFromDb()

        for (domain in allDomains) {
            if (domain.acronym != "dims") domains[domain.acronym] = domain

            val defs = getDefinitionsForDomain(domain._acronym)
            for (definition in defs) {
                val newDefinition = extractJSONDefinition(definition, domain._acronym)
                addDefinitionToIndexes(newDefinition, newDefinition.domainAcronym)
            }
        }
        indexWriter.commit()
    }

    fun addDomainToMemory(domain: Domain) {
        domains[domain.acronym] = domain
    }

    private fun addDefinitionToIndexes(newDefinition: Definition, domainAcronym: String?) {
        definitions.add(newDefinition)
        id2definitions[newDefinition.identifier] = newDefinition

        val doc = Document()
        doc.add(TextField("name", newDefinition.name, Field.Store.YES))
        doc.add(TextField("domain", domainAcronym, Field.Store.YES))
        doc.add(TextField("identifier", newDefinition.identifier.split("/").last(), Field.Store.YES))
        doc.add(StoredField("uri", newDefinition.identifier))
        doc.add(TextField("definition", newDefinition.definition, Field.Store.YES))
        doc.add(TextField("guidance", newDefinition.guidance, Field.Store.YES))
        indexWriter.addDocument(doc)
    }

    private fun extractJSONDefinition(dbDef: DBDef, domainAcronym: String): Definition {
        val jsonDefinition = dbDef.definition
        val name = jsonDefinition.string("name") ?: ""
        val domain = jsonDefinition.string("domain") ?: ""
        val status = jsonDefinition.string("status") ?: ""
        val definition = jsonDefinition.string("definition") ?: ""
        val guidance = jsonDefinition.string("guidance") ?: ""
        val identifier = jsonDefinition.string("identifier") ?: ""
        val sourceURL = jsonDefinition.string("sourceURL") ?: dbDef.source
        var usage = arrayOf<String>()
        val jsonUsage = jsonDefinition.array<String>("usage")
        if (jsonUsage != null) {
            for (agencyUsage in jsonUsage) {
                usage = usage.plus(agencyUsage)
            }
        }
        val datatype = jsonDefinition.obj("datatype")
        val type = datatype?.string("type") ?: ""
        val typeValues = datatype?.array<String>("values")
        var values = arrayOf<String>()
        if (typeValues != null) {
            for (typeValue in typeValues.iterator()) {
                values = values.plus(typeValue)
            }
        }

        val typeFacets = datatype?.obj("facets")
        val facets = mutableMapOf<String, String>()
        if (typeFacets != null) {
            for ((typeFacet, typeFacetValue) in typeFacets.iterator()) {
                facets[typeFacet] = typeFacetValue.toString()
            }
        }

        return Definition(
                name,
                domain,
                status,
                definition,
                guidance,
                identifier,
                usage,
                type,
                values,
                facets,
                domainAcronym,
                sourceURL
        )
    }

    fun search(query: String, domain: String, page: Int, size: Int, raw: Boolean, ignoreSynonym: Boolean): SearchResults {
        val maxSearch = 500


        val reader = DirectoryReader.open(index)
        val searcher = IndexSearcher(reader)
        val results = mutableListOf<Definition>()
        var usedSynonyms = mapOf<String, List<String>>()
        var queryString = query
        if (!raw && !ignoreSynonym) {
            val synonymExpansion = synonymService.expand(query.toLowerCase())
            queryString = LuceneQueryParser.parse(synonymExpansion.expandedQuery, domain)
            usedSynonyms = synonymExpansion.usedSynonyms
        } else if (!raw) {
            queryString = LuceneQueryParser.parse(queryString, domain)
        }
        val queryParser = QueryParser("name", analyzer)

        val searchQuery = queryParser.parse(queryString)

        val docs = searcher.search(searchQuery, maxSearch)
        val hits = docs.scoreDocs


        // record query, hits.size + now()
        queryLog.logQuery(query, queryString, hits.size)


        var offset = (page - 1) * size
        if (offset < 0) offset = 0

        var upperBound = offset + size
        if (upperBound > hits.size) {
            upperBound = hits.size
        }

        for (i in offset until upperBound) {
            val hit = hits[i]
            val docId = hit.doc
            val d = searcher.doc(docId)
            val uri = d.getField("uri").stringValue()
            val definition = id2definitions[uri]!!
            results.add(definition)
        }

        reader.close()
        return SearchResults(results, hits.size, usedSynonyms)
    }

    fun getDefinitionById(id: String): Definition = id2definitions[id]!!

    fun getDomainByAcronym(acronym: String): Domain? = domains[acronym]

    fun getDomainByName(name: String): Domain? = domains.values.filter { it.name == name }.firstOrNull()

    fun getDomains(): List<Domain> = domains.values.toList()

    private fun getDefinitionsForDomain(domain: String): List<DBDef> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS definitions (acronym VARCHAR(5), defdomain VARCHAR(100), version VARCHAR(100),sourceURL VARCHAR(2000),ident VARCHAR(200),definition JSON)")
            val q = connection.prepareStatement("SELECT definition, sourceurl FROM definitions WHERE acronym = '$domain'")
            var rs = q.executeQuery()
            val rv: MutableList<DBDef> = mutableListOf()
            while (rs.next()) {
                rv.add(DBDef(Parser().parse(StringBuilder(rs.getString("definition"))) as JsonObject, rs.getString("sourceurl")))
            }
            return rv.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun getAllDefinitions(): MutableList<Definition> {
        return definitions
    }

    fun getAllDefinitionsInDomain(domain: String): MutableList<Definition> {
        return definitions.filter { it.identifier.matches(""".*/$domain/.*""".toRegex()) }.toMutableList()
    }

    fun findAll(pageNumber: Int, pageSize: Int): List<Definition> {
        var offset = (pageNumber - 1) * capPageSize(pageSize)
        if (offset < 0) offset = 0

        var upperBound = offset + capPageSize(pageSize)
        if (upperBound > howManyDefinitions()) {
            upperBound = howManyDefinitions()
        }

        if (offset > upperBound) offset = upperBound
        return definitions.subList(offset, upperBound)
    }

    fun findAllInDomain(pageNumber: Int, pageSize: Int, domain: String): List<Definition> {
        var offset = (pageNumber - 1) * capPageSize(pageSize)
        if (offset < 0) offset = 0

        var upperBound = offset + capPageSize(pageSize)
        if (upperBound > howManyDefinitionsInDomain(domain)) {
            upperBound = howManyDefinitionsInDomain(domain)
        }

        if (offset > upperBound) offset = upperBound
        return definitions.filter { it.identifier.matches(""".*/$domain/.*""".toRegex()) }.subList(offset, upperBound)
    }

    fun findOne(id: String?): Definition {
        val results = definitions.filter { it.identifier == id }
        if (results.isEmpty()) throw DefinitionNotFoundException(id)
        return results.first()
    }

    fun howManyDefinitions(): Int {
        return definitions.size
    }

    fun howManyDefinitionsInDomain(domain: String): Int {
        return definitions.filter { it.identifier.matches(""".*/$domain/.*""".toRegex()) }.size
    }

    fun deleteByTerm(field: String, termText: String) {
        var term = Term(field, termText)
        indexWriter.deleteDocuments(term)
    }

    // database access functions

    fun saveDefinition(input: NewDefinition) {
        var stringToSave = ObjectMapper().writeValueAsString(input)
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS definitions (acronym VARCHAR(5), defdomain VARCHAR(100), version VARCHAR(100),sourceURL VARCHAR(2000),ident VARCHAR(200),definition JSON)")
            var upsert = connection.prepareStatement("INSERT INTO definitions(acronym, defdomain, version, sourceURL,ident, definition) VALUES(?, ?, ?, ?, ?, ?::jsonb)")
            //val upsert = connection.prepareStatement("INSERT INTO synonyms(synonym) VALUES(?::jsonb)")
            upsert.setString(1, input.domainAcronym)
            upsert.setString(2, input.domain)
            upsert.setString(3, input.version)
            upsert.setString(4, input.sourceURL)
            upsert.setString(5, input.identifier)
            upsert.setString(6, stringToSave)
            upsert.executeUpdate()
            addDefinitionToIndexes(Definition(input), input.domainAcronym)
            indexWriter.commit()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    private fun getDomainsFromDb(): List<Domain> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            var stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS definitions (acronym VARCHAR(5), defdomain VARCHAR(100), version VARCHAR(100),sourceURL VARCHAR(2000),ident VARCHAR(200),definition JSON)")
            stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT DISTINCT acronym, defdomain, version FROM definitions")
            val rv: MutableList<Domain> = mutableListOf()
            while (rs.next()) {
                var acr = rs.getString("acronym")
                var def = rs.getString("defdomain")
                var ver = rs.getString("version")
                rv.add(Domain(def, acr, ver))
            }
            return rv.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun removeDefinitions(ident: String) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS definitions (acronym VARCHAR(5), defdomain VARCHAR(100), version VARCHAR(100),sourceURL VARCHAR(2000),ident VARCHAR(200),definition JSON)")
            val q = connection.prepareStatement("DELETE FROM definitions WHERE ident = ?")
            q.setString(1, ident)
            q.executeUpdate()
            deleteByTerm("identifier", ident.split("/").last())
            indexWriter.commit()
            definitions.remove(definitions.first { it.identifier ==  ident})
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

}
