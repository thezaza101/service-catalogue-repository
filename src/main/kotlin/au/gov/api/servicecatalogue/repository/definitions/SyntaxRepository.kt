package au.gov.api.servicecatalogue.repository.definitions

import au.gov.api.servicecatalogue.repository.RepositoryException
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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.mutableMapOf
import kotlin.collections.set


data class Syntax(@JsonIgnore val identifier: String, val syntaxes: Map<String, Map<String, String>>)

@Component
class SyntaxRepository {

    @Value("\${spring.datasource.url}")
    private var dbUrl: String? = null

    @Autowired
    private lateinit var dataSource: DataSource

    private var syntaxData: MutableMap<String, Syntax> = mutableMapOf()


    @EventListener(ApplicationReadyEvent::class)
    fun initialise() {
        for ( syntax in getSyntax()){
            val identifier = syntax["identifier"] as String
            val syntaxes = syntax["syntax"] as LinkedHashMap<String,*>
            val syntaxesMap = mutableMapOf<String, Map<String, String>>()
            if(syntaxes != null){
                for((syntaxName, syntaxMap) in syntaxes.iterator()){
                    val syntaxOptions = mutableMapOf<String,String>()
                    if(syntaxMap != null && syntaxMap is MutableMap<*, *>){
                        for((syntaxProperty, syntaxValue) in syntaxMap.iterator()){
                            if(syntaxProperty is String && syntaxValue is String) {
                                syntaxOptions[syntaxProperty] = syntaxValue
                            }
                        }
                    }
                    syntaxesMap[syntaxName] = syntaxOptions
                }
            }

            val newSyntax = Syntax(identifier, syntaxesMap)
            syntaxData[identifier] = newSyntax
        }
    }

    private fun getSyntax() : List<LinkedHashMap<String,*>> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT syntax FROM syntaxes")
            val rv: MutableList<LinkedHashMap<String,*>> = mutableListOf()
            val om = ObjectMapper()
            while (rs.next()) {
                var value = rs.getString("syntax")
                val syntax = om.readValue(value, LinkedHashMap::class.java)
                rv.add(syntax as LinkedHashMap<String,*>)
            }
            return rv.toList()

        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun findOne(id: String): Syntax? = syntaxData[id]


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
