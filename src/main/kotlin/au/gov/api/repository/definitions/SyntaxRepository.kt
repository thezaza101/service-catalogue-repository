package au.gov.api.repository.definitions

import au.gov.api.repository.RepositoryException
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.collections.set

data class Syntax(@JsonIgnore val identifier: String, val syntaxes: Map<String, Map<String, String>>)

@Component
class SyntaxRepository {


    @Autowired
    lateinit var dataSource: DataSource

    private var syntaxData: MutableMap<String, Syntax> = mutableMapOf()

    constructor() {}

    constructor(theDataSource: DataSource) {
        dataSource = theDataSource
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initialise() {
        for (syntax in getSyntax()) {
            val identifier = syntax["identifier"] as String
            val syntaxes = syntax["syntax"] as Map<String, Map<String, String>>
            addSuntaxToRepo(identifier, syntaxes)
        }
    }

    fun findOne(id: String): Syntax? = syntaxData[id]

    private fun addSuntaxToRepo(ident: String, syntaxs: Map<String, Map<String, String>>) {
        val newSyntax = Syntax(ident, syntaxs)
        syntaxData[ident] = newSyntax
    }

    // database access functions

    private fun getSyntax(): List<LinkedHashMap<String, *>> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS syntaxes (syntax JSONB)")
            val rs = stmt.executeQuery("SELECT syntax FROM syntaxes")
            val rv: MutableList<LinkedHashMap<String, *>> = mutableListOf()
            val om = ObjectMapper()
            while (rs.next()) {
                var value = rs.getString("syntax")
                val syntax = om.readValue(value, LinkedHashMap::class.java)
                rv.add(syntax as LinkedHashMap<String, *>)
            }
            return rv.toList()

        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun saveSyntax(id: String, syntaxs: Map<String, Map<String, Map<String, String>>>) {
        data class SyntaxStruct(var identifier: String, var syntax: Map<String, Map<String, String>>)

        var syntaxToSave = SyntaxStruct(id, syntaxs[syntaxs.keys.first()]!!)
        val json = ObjectMapper().writeValueAsString(syntaxToSave)

        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS syntaxes (syntax JSONB)")
            val upsert = connection.prepareStatement("INSERT INTO syntaxes(ident,syntax) VALUES(?, ?::jsonb) ON CONFLICT(ident) DO UPDATE SET syntax = EXCLUDED.syntax")
            upsert.setString(1, id)
            upsert.setString(2, json)
            upsert.executeUpdate()
            addSuntaxToRepo(id, syntaxs[syntaxs.keys.first()]!!)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

}
