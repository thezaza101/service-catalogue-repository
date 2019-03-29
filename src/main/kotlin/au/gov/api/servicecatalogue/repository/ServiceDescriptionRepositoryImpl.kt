
package au.gov.api.servicecatalogue.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlin.collections.Iterable
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.org.apache.xpath.internal.operations.Bool

import org.springframework.context.event.EventListener
import org.springframework.boot.context.event.ApplicationReadyEvent

@Service
class ServiceDescriptionRepositoryImpl : ServiceDescriptionRepository {
    @Value("\${spring.datasource.url}")
    private var dbUrl: String? = null

    @Autowired
    private lateinit var dataSource: DataSource

    @EventListener(ApplicationReadyEvent::class)
    fun ingestFromGithub() {
        val url = "https://github.com/apigovau/api-gov-au-definitions/blob/master/api-documentation.md"
        val raw = GitHub.getTextOfFlie(url)
        val mdfm = SingleMarkdownWithFrontMatter(raw)
        val sd = mdfm.serviceDescription

        try{
            val existingSd = findByIngestion(url)
            existingSd.revise(mdfm.name, mdfm.description, mdfm.pages,false)
            existingSd.tags = sd.tags
            save(existingSd)

        }catch(e:Exception){

            sd.metadata.ingestSource = url
            save(sd)
                
        }

    }

    constructor(){}

    constructor(theDataSource:DataSource){
		dataSource = theDataSource
	}


    private fun findByIngestion(uri: String): ServiceDescription {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val q = connection.prepareStatement("select data from service_descriptions where data->'metadata'->>'ingestSource' = ?")
            q.setString(1, uri)
            var rs = q.executeQuery()
            if (!rs.next()) {
                throw RepositoryException()
            }
            val sd = ObjectMapper().readValue(rs.getString("data"), ServiceDescription::class.java)
            return sd
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun saveCache(cache_entry: WebRequestHandler.ResponseContentChacheEntry){
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS web_cache (id VARCHAR(300), data JSONB, PRIMARY KEY (id))")

            val upsert = connection.prepareStatement("INSERT INTO web_cache(id, data) VALUES(?, ?::jsonb) ON CONFLICT(id) DO UPDATE SET data = EXCLUDED.data")

            upsert.setString(1, cache_entry.request_uri)
            upsert.setString(2, ObjectMapper().writeValueAsString(cache_entry))
            upsert.executeUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun findCacheByURI(URI: String): WebRequestHandler.ResponseContentChacheEntry {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val q = connection.prepareStatement("SELECT data FROM web_cache WHERE id = ?")
            q.setString(1, URI)
            var rs = q.executeQuery()
            if (!rs.next()) {
                throw RepositoryException()
            }
            val wc = ObjectMapper().readValue(rs.getString("data"), WebRequestHandler.ResponseContentChacheEntry::class.java)
            return wc
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun deleteCacheByURI(URI: String,ignorePrams:Boolean=true) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            var sql = "DELETE FROM web_cache WHERE id LIKE ?"
            var likeOp="%"
            if (!ignorePrams) {
                sql = "DELETE FROM web_cache WHERE id = ?"
                likeOp = ""
            }

            val q = connection.prepareStatement(sql)
            q.setString(1, URI+likeOp)
            var rs = q.execute()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    override fun findById(id: String,returnPrivate:Boolean): ServiceDescription {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val q = connection.prepareStatement("SELECT data FROM service_descriptions WHERE id = ?")
            q.setString(1, id)
            var rs = q.executeQuery()
            if (!rs.next()) {
                throw RepositoryException()
            }
            val sd = ObjectMapper().readValue(rs.getString("data"), ServiceDescription::class.java)
            if (sd.metadata.visibility)
            {
                return sd
            } else {
                if (returnPrivate) return sd else throw RepositoryException()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    override fun delete(id: String) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val q = connection.prepareStatement("DELETE FROM service_descriptions WHERE id = ?")
            q.setString(1, id)
            q.executeUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    override fun count(): Int {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS service_descriptions (id VARCHAR(50), data JSONB, PRIMARY KEY (id))")
            var rs = stmt.executeQuery("SELECT COUNT(*) c FROM service_descriptions")
            if (!rs.next()) {
                throw RepositoryException()
            }
            return rs.getInt("c")
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    override fun save(service: ServiceDescription) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS service_descriptions (id VARCHAR(50), data JSONB, PRIMARY KEY (id))")

            val upsert = connection.prepareStatement("INSERT INTO service_descriptions(id, data) VALUES(?, ?::jsonb) ON CONFLICT(id) DO UPDATE SET data = EXCLUDED.data")
            if (service.id == null) {
                service.id = UUID.randomUUID().toString()
            }
            upsert.setString(1, service.id)
            upsert.setString(2, ObjectMapper().writeValueAsString(service))
            upsert.executeUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    override fun findAll(returnPrivate:Boolean): Iterable<ServiceDescription> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            val rs = stmt.executeQuery("SELECT data FROM service_descriptions")
            val rv: MutableList<ServiceDescription> = mutableListOf()
            val om = ObjectMapper()
            while (rs.next()) {
                rv.add(om.readValue(rs.getString("data"), ServiceDescription::class.java))
            }
            if (returnPrivate)
            {
                return rv
            } else {
                return rv.filter { s -> s.metadata.visibility == true}
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
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


