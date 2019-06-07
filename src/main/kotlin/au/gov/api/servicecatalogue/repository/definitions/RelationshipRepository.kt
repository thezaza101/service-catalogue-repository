package au.gov.api.servicecatalogue.repository.definitions

import au.gov.api.servicecatalogue.repository.RepositoryException
import com.beust.klaxon.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource


enum class Direction {
    FROM, TO, UNDIRECTED
}

data class Result(@JsonIgnore var meta: Meta, val direction: Direction, val to: String, var toName:String = "")
data class Meta(val type:String, val directed:Boolean, val verbs : Map<Direction, String>)
data class RelationDTO(val from:String, val type:String, val to:String, val direction: Direction)


@Service
class RelationshipRepository {

    @Value("\${spring.datasource.url}")
    private var dbUrl: String? = null

    @Autowired
    private lateinit var dataSource: DataSource

    private var relationships: MutableMap<String, MutableList<Result>> = mutableMapOf()
    private var metas: MutableMap<String, Meta> = mutableMapOf()


    constructor(){}

    constructor(theDataSource:DataSource){
        dataSource = theDataSource
    }
    private fun addResult(from:String, type:String, to:String, direction: Direction){
        if(!metas.containsKey(type)) println("\n\n*****\nNo relationsihp meta for $type. Failing\n*****\n\n")
        val meta = metas[type]!!
        if(!relationships.containsKey(from)){
            relationships[from] = mutableListOf()
        }
        relationships[from]!!.add(Result(meta, direction, to))
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initialise() {
        addMetas()
        addJson()
        addJsonLd()
    }

    private fun addJsonLd(){
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
            for (relations in relationship.content) {
                val from = relations[0]
                val to = relations[1]
                addResult(from, type, to, Direction.TO)
                addResult(to, type, from, Direction.FROM)
            }
        }
    }

    class Relations {
        var type:String = ""
        var content:MutableList<MutableList<String>> = mutableListOf()
        constructor (){}
    }
    private fun getRelationships() : MutableList<Relations> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT relationship FROM definitions_relationships")
            val rv: MutableList<Relations> = mutableListOf()
            val om = ObjectMapper()
            while (rs.next()) {
                rv.add(om.readValue(rs.getString("relationship"), Relations::class.java))
            }
            return rv

        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    private fun addMetas() {
        if (metas.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            for (jsonRelationship in JsonHelper.parse("/relationships/meta.json") as JsonArray<JsonObject>) {
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

    fun getRelationshipFor(identifier: String): MutableMap<String, MutableList<Result>>{
        if(!relationships.containsKey(identifier)) return mutableMapOf()
        val relations = relationships[identifier]

        val results = mutableMapOf<String, MutableList<Result>>()

        for(relation in relations!!){
            if(!results.containsKey(relation.meta.type)) results[relation.meta.type] = mutableListOf()
            results[relation.meta.type]!!.add(relation)
        }
        return results
    }

    fun getMeta(type:String): Meta {
        return metas[type]!!
    }

    data class NewRelationship(var type: String, var dir:Direction, var content: Pair<String,String>)

    fun saveRelationship(relation:NewRelationship)
    {
        var stringToSave = getDBString(relation)
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS synonyms (synonym JSONB)")
            val upsert = connection.prepareStatement("INSERT INTO synonyms(synonym) VALUES(?::jsonb)")
            upsert.setString(1, stringToSave)
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

    fun getDBString(rel:NewRelationship) : String {
        var output = "{\"type\":\"${rel.type}\",\"content\":[[\"${rel.content.first}\",\"${rel.content.second}\"]]}"
        return output
    }

    fun addRelationshipToMemoryDB (rel:NewRelationship) {
        addResult(rel.content.first, rel.type, rel.content.second, Direction.TO)
        addResult(rel.content.second, rel.type, rel.content.first, Direction.FROM)
    }
    fun addMetas (meta:Meta) {
        metas[meta.type] = meta
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
