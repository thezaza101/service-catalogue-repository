package au.gov.api.servicecatalogue.repository.definitions

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
public class DictionaryService {

    @Autowired
    private lateinit var repository: DefinitionRepository

    fun getDictionaryCorrection(query: String, domains: Array<String>): String {
        try {
            if (query.length > 3) {
                if (domains.count() == 0 || domains.first() == "") {
                    return runQuery(query, repository.getAllDefinitions())
                } else {
                    if (domains.count() > 0) {
                        var filterdList: MutableList<Definition> = mutableListOf()
                        domains.forEach { filterdList.addAll(repository.getAllDefinitionsInDomain(it)) }
                        return runQuery(query, filterdList)
                    } else {
                        return runQuery(query, repository.getAllDefinitions())
                    }
                }
            } else {
                return ""
            }
        } catch (e: Exception) {
            return ""
        }
        return ""
    }

    fun runQuery(query: String, filterdDef: MutableList<Definition>): String {
        var results: MutableList<DistanceResult> = mutableListOf()
        filterdDef.forEach { results.add(DistanceResult(it.name, levenshtein(query, it.name))) }
        results.sortBy { it.distance }
        when (results.first().distance < query.length / 1.5) {
            true -> return results.first().value
            false -> return ""
        }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = Array(lhsLength) { it }
        var newCost = Array(lhsLength) { 0 }

        for (i in 1..rhsLength - 1) {
            newCost[0] = i

            for (j in 1..lhsLength - 1) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = Math.min(Math.min(costInsert, costDelete), costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength - 1]
    }
}

data class DistanceResult(var value: String, var distance: Int)