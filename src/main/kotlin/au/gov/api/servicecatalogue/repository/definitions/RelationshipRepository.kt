package au.gov.api.servicecatalogue.repository.definitions

import au.gov.api.servicecatalogue.repository.RepositoryException
import com.beust.klaxon.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.lang.IndexOutOfBoundsException
import java.lang.StringBuilder
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

enum class Direction {
    FROM, TO, UNDIRECTED
}

data class Result(@JsonIgnore var meta: Meta, val direction: Direction, val to: String, var toName: String = "")

data class Meta(val type: String, val directed: Boolean, val verbs: Map<Direction, String>)

@Service
class RelationshipRepository {

    @Value("\${spring.datasource.url}")
    var dbUrl: String? = null

    @Autowired
    lateinit var dataSource: DataSource

    data class NewRelationship(var type: String, var dir: Direction, var content: Pair<String, String>)

    class Relations {
        var type: String = ""
        var content: MutableList<String> = mutableListOf()

        constructor (itype: String, icontent: MutableList<String>) {
            type = itype
            content = icontent
        }
    }

    private var relationships: MutableMap<String, MutableList<Result>> = mutableMapOf()
    private var metas: MutableMap<String, Meta> = mutableMapOf()

    constructor() {}

    constructor(theDataSource: DataSource) {
        dataSource = theDataSource
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initialise() {
        addMetas()
        addJson()
        addJsonLd()
    }

    private fun addResult(from: String, type: String, to: String, direction: Direction) {
        if (!metas.containsKey(type)) println("\n\n*****\nNo relationsihp meta for $type. Failing\n*****\n\n")
        val meta = metas[type]!!
        if (!relationships.containsKey(from)) {
            relationships[from] = mutableListOf()
        }
        relationships[from]!!.add(Result(meta, direction, to))
    }

    private fun addJsonLd() {
        // for historical purposes
        /*for(jsonld in listOf("agift")){
            val json = JsonLd("/definitions/jsonld/$jsonld.json")
            json.relations.forEach { addResult( it.from, it.type, it.to, Direction.TO)
                addResult( it.to, it.type, it.from, Direction.FROM)}
        }*/
    }

    private fun addJson() {
        for (relationship in getRelationships()) {
            val type = relationship.type
            try {
                val from = relationship.content[0]
                val to = relationship.content[1]
                addResult(from, type, to, Direction.TO)
                addResult(to, type, from, Direction.FROM)
            } catch (e: IndexOutOfBoundsException) {
                val from = ""
                val to = ""
                addResult(from, type, to, Direction.TO)
                addResult(to, type, from, Direction.FROM)
            }
        }
    }

    private fun readJsonArrayToStringList(input: String): MutableList<String> {
        val raw = input.substring(1, input.length - 1).trim()
        var output: MutableList<String> = mutableListOf()
        if (raw.length < 1) return output
        val rawArray = raw.split(',')
        for (s in rawArray) {
            output.add(s.replace('"', ' ').trim())
        }
        return output
    }

    private fun addMetas() {
        if (metas.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            for (jsonString in getMetaJsonFromDB()) {
                var jsonRelationship = Parser().parse(StringBuilder(jsonString)) as JsonObject
                val type = jsonRelationship.string("type") ?: ""
                val directed = jsonRelationship.boolean("directed") ?: false
                val to = jsonRelationship.string("to") ?: ""
                val from = jsonRelationship.string("from") ?: ""
                val verbMap = mapOf(Direction.TO to to, Direction.FROM to from)
                val meta = Meta(type, directed, verbMap)
                metas[type] = meta
            }
        }
    }

    fun getRelationshipFor(identifier: String): MutableMap<String, MutableList<Result>> {
        if (!relationships.containsKey(identifier)) return mutableMapOf()
        val relations = relationships[identifier]

        val results = mutableMapOf<String, MutableList<Result>>()

        for (relation in relations!!) {
            if (!results.containsKey(relation.meta.type)) results[relation.meta.type] = mutableListOf()
            results[relation.meta.type]!!.add(relation)
        }
        return results
    }

    fun getMeta(type: String): Meta {
        return metas[type]!!
    }

    fun getDBString(rel: NewRelationship): String {
        var output = "[\"${rel.content.first}\",\"${rel.content.second}\"]"
        return output
    }

    fun addRelationshipToMemoryDB(rel: NewRelationship) {
        addResult(rel.content.first, rel.type, rel.content.second, Direction.TO)
        addResult(rel.content.second, rel.type, rel.content.first, Direction.FROM)
    }

    fun addMetas(meta: Meta) {
        metas[meta.type] = meta
    }

    // database access functions

    private fun getMetaJsonFromDB(): List<String> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT meta FROM definitions_relation_meta")
            val rv: MutableList<String> = mutableListOf()
            while (rs.next()) {
                rv.add(rs.getString("meta"))
            }
            return rv.toList()

        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    private fun getRelationships(): MutableList<Relations> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM definitions_relationships")
            val rv: MutableList<Relations> = mutableListOf()
            while (rs.next()) {
                val relType = rs.getString("reltype")
                val content = readJsonArrayToStringList(rs.getString("relationship"))
                rv.add(Relations(relType, content))
            }
            return rv

        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun saveRelationship(relation: NewRelationship) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS definitions_relationships (acronym VARCHAR(5),relType VARCHAR(50), relationship JSONB)")
            val upsert = connection.prepareStatement("INSERT INTO definitions_relationships(acronym, relType, relationship) VALUES(?,?,?::jsonb)")
            upsert.setString(1, "")
            upsert.setString(2, relation.type)
            upsert.setString(3, getDBString(relation))
            upsert.executeUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }

        //The Synonym needs to be added to the in-memory list as SQL updates do not reflect until the app is restarted
        addRelationshipToMemoryDB(relation)
    }

    @Bean
    @Throws(SQLException::class)
    fun dataSource(): DataSource? {
        if (dbUrl?.isEmpty() ?: true) {
            return HikariDataSource()
        } else {
            val config = HikariConfig()
            config.jdbcUrl = dbUrl
            try {
                return HikariDataSource(config)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
