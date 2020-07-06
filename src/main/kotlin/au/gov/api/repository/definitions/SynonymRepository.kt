package au.gov.api.repository.definitions

import au.gov.api.repository.RepositoryException
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

data class SynonymExpansionResults(val expandedQuery: String, val usedSynonyms: Map<String, List<String>>)

@Component
class SynonymRepository {


    @Autowired
    lateinit var dataSource: DataSource

    var originalSynonymWordWeighting = "^1.2"

    var origSynonyms = mutableListOf<List<String>>()

    private var synonyms: MutableMap<String, List<String>> = mutableMapOf()

    constructor() {}

    constructor(theDataSource: DataSource) {
        dataSource = theDataSource
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initialise() {

        if (synonyms.isEmpty()) {

            //@Suppress("UNCHECKED_CAST")
            for (synonymSet in getSynonyms()) {
                origSynonyms.add(synonymSet)

                for (synonymWord in synonymSet) {
                    if (synonymWord in synonyms) {
                        println("Duplicate synonym found: $synonymWord\n${synonyms[synonymWord]}\n$synonymSet")
                        System.exit(1)
                    }
                    synonyms[synonymWord] = synonymSet
                }
            }
        }
    }

    private fun addSynonymToMemoryDB(synonym: String) {
        var syns = getSynonymList(synonym)
        origSynonyms.add(syns)

        for (synonymWord in syns) {
            if (synonymWord in synonyms) {
                println("Duplicate synonym found: $synonymWord\n${synonyms[synonymWord]}\n$synonym")
                System.exit(1)
            }
            synonyms[synonymWord] = syns
        }
    }

    fun validateNewSynonym(input: List<String>): Pair<Boolean, List<String>?> {
        var originalSyns: HashMap<List<String>?, Int> = hashMapOf()
        if (input.count() < 2) return Pair(false, null)
        for (word in input) {
            val foundVal = getSynonym(word)
            originalSyns[foundVal] = if (originalSyns.containsKey(foundVal)) originalSyns[foundVal]!! + 1 else 1
        }
        val filterdSyns = originalSyns.keys.filterNotNull()
        if (filterdSyns.count() > 1) return Pair(false, null) else return Pair(true, filterdSyns.firstOrNull())
    }

    private fun getDBString(input: List<String>): String {
        var output = "[\""
        for (s in input) {
            output += "$s\", \""
        }
        output = output.substring(0, output.length - 3) + "]"
        return output
    }

    fun replaceSynonyms(inputOriginal: List<String>, inputNew: List<String>) {
        removeSynonyms(inputOriginal)
        saveSynonym(inputNew)
    }

    fun getSynonym(word: String): List<String>? {
        if (synonyms.contains(word)) return synonyms[word]
        return null
    }

    private fun getSynonymList(input: String): List<String> {
        var split = input.replace("\"", "").replace("[", "").replace("]", "").split(',')
        var synonyms = mutableListOf<String>()
        for (word in split) {
            synonyms.add(word.trim())
        }
        return synonyms
    }

    fun expand(input: String): SynonymExpansionResults {
        var output = ""

        val usedSynonyms = mutableMapOf<String, List<String>>()

        val tokens = getTokens(input)
        for (token in tokens) {
            var workingToken = token
            val hadModifier = workingToken.startsWith("-") || workingToken.startsWith("+")
            var modifier = ""
            if (hadModifier) {
                modifier = workingToken.substring(0, 1)
                workingToken = workingToken.substring(1)
                //println("Had modifier of '${modifier}'. Now is : ${workingToken}")
            }
            val wasQuoted = workingToken.startsWith('"') && workingToken.endsWith('"')
            if (wasQuoted) workingToken = workingToken.removePrefix("\"").removeSuffix("\"")
            //println("workingToken: ${workingToken}")
            if (workingToken in synonyms) {
                var expandedSynonyms = synonyms[workingToken]!!.map { quoteIfNotMainSynonymAndHasSpaces(workingToken, it) }.joinToString(" ")

                val synonymAlternatives = synonyms[workingToken]!!.filter { it != workingToken }
                usedSynonyms[workingToken] = synonymAlternatives

                //println(expandedSynonyms)
                var origTokenWithWeight = token + originalSynonymWordWeighting
                if (wasQuoted) origTokenWithWeight = "\"$workingToken\"$originalSynonymWordWeighting"
                expandedSynonyms = expandedSynonyms.replace(workingToken, origTokenWithWeight)
                output = "$output$modifier($expandedSynonyms)"
            } else output += token
            output += " "
        }
        output = output.removeSuffix(" ")

        return SynonymExpansionResults(output, usedSynonyms.toMap())
    }

    private fun quoteIfNotMainSynonymAndHasSpaces(mainToken: String, token: String): String {
        if (token.contains(" ") && token != mainToken) return "\"$token\""
        return token
    }

    private fun getTokens(input: String): List<String> {
        val theInput = quoteSynonyms(input)
        var workingInput = theInput
        val quoteMatcher = Regex("([\"'])(?:(?=(\\\\?))\\2.)*?\\1")
        for (match in quoteMatcher.findAll(theInput)) {
            val withoutSpaces = match.value.replace(" ", "~~")
            workingInput = workingInput.replace(match.value, withoutSpaces)
        }
        val workingTokens = workingInput.split(" ")
        return workingTokens.map { it.replace("~~", " ") }

    }

    private fun quoteSynonyms(input: String): String {
        var output = ""
        for (synonym in synonyms.keys.sortedByDescending { it.length }) {
            val regex = Regex("\\b$synonym\\b")
            if (input.contains(regex) && synonym.contains(" ") && !input.contains("\"$synonym\"")) output += " \"$synonym\""
        }
        output = input + " " + output.replace("\"\"", "\"").trim()
        return output.trim()
    }

    // database access functions

    private fun getSynonyms(): List<List<String>> {
        var connection: Connection? = null
        try {
            connection = dataSource.connection

            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS synonyms (synonym JSONB)")
            val rs = stmt.executeQuery("SELECT synonym FROM synonyms")
            val rv: MutableList<List<String>> = mutableListOf()
            while (rs.next()) {
                var value = rs.getString("synonym")
                rv.add(getSynonymList(value))
            }
            return rv.toList()

        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }

    fun saveSynonym(synonym: List<String>) {
        var stringToSave = getDBString(synonym)
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
        addSynonymToMemoryDB(stringToSave)
    }

    fun removeSynonyms(input: List<String>) {
        var connection: Connection? = null
        try {
            connection = dataSource.connection
            val dbEntry = getDBString(input)

            val stmt = connection.createStatement()
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS synonyms (synonym JSONB)")
            val q = connection.prepareStatement("DELETE FROM synonyms WHERE synonym::text = ?")
            q.setString(1, dbEntry)
            q.executeUpdate()

            origSynonyms.remove(input)

            for (word in input) {
                synonyms.remove(word)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RepositoryException()
        } finally {
            if (connection != null) connection.close()
        }
    }


}
