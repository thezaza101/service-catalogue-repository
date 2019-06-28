package au.gov.api.servicecatalogue.repository.definitions

import DoubleMetaphone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.absoluteValue
import kotlin.math.pow

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
        var dms: MutableList<List<List<String>>> = mutableListOf()

        //Get the approximate phonetic encoding(s) for the filtered definition list
        filterdDef.forEach { dms.add(getMetaphoneMatrix(it.name)) }

        //Get the approximate phonetic encoding(s) for the query
        val queryDM = getMetaphoneMatrix(query)

        //Compare all of the phonetic encodings and save the result
        var scores: MutableList<Int> = mutableListOf()
        dms.forEach { scores.add(compareMetaphoneMatrix(queryDM, it)) }

        val phoneDefs : MutableList<DistanceResult> = mutableListOf()
        var strResults: MutableList<DistanceResult> = mutableListOf()


        filterdDef.forEach { strResults.add(DistanceResult(it.name, damerauLevenshtein(query, it.name).toDouble())) }

        for (i in 0 until scores.count()){
            phoneDefs.add(DistanceResult(filterdDef[i].name,scores[i].toDouble()))
        }

        val strMax = strResults.maxBy { it.distance }
        val phoneMax = phoneDefs.maxBy { it.distance }


        var results: MutableList<DistanceResult> = mutableListOf()

        for (i in 0 until scores.count()){
            var newScore = getWeightedScore(phoneDefs[i],phoneMax!!.distance) +
                    getWeightedScore(strResults[i],strMax!!.distance + (query.length - strResults[i].value.length).absoluteValue)
            results.add(DistanceResult(phoneDefs[i].value,newScore))
        }

        results.sortBy { it.distance }
        return results.first().value
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

    private fun damerauLevenshtein(a: CharSequence, b: CharSequence): Int {
        val cost = Array(a.length + 1, { IntArray(b.length + 1) })
        for (iA in 0..a.length) {
            cost[iA][0] = iA
        }
        for (iB in 0..b.length) {
            cost[0][iB] = iB
        }
        val mapCharAToIndex = hashMapOf<Char, Int>()

        for (iA in 1..a.length) {
            var prevMatchingBIndex = 0
            for (iB in 1..b.length) {
                val doesPreviousMatch = (a[iA - 1] == b[iB - 1])

                val possibleCosts = mutableListOf<Int>()
                if (doesPreviousMatch) {
                    // Perfect match cost.
                    possibleCosts.add(cost[iA - 1][iB - 1])
                } else {
                    // Substitution cost.
                    possibleCosts.add(cost[iA - 1][iB - 1] + 1)
                }
                // Insertion cost.
                possibleCosts.add(cost[iA][iB - 1] + 1)
                // Deletion cost.
                possibleCosts.add(cost[iA - 1][iB] + 1)

                // Transposition cost.
                val bCharIndexInA = mapCharAToIndex.getOrDefault(b[iB - 1], 0)
                if (bCharIndexInA != 0 && prevMatchingBIndex != 0) {
                    possibleCosts.add(cost[bCharIndexInA - 1][prevMatchingBIndex - 1]
                            + (iA - bCharIndexInA - 1) + 1 + (iB - prevMatchingBIndex - 1))
                }

                cost[iA][iB] = possibleCosts.min()!!

                if (doesPreviousMatch) prevMatchingBIndex = iB
            }
            mapCharAToIndex[a[iA - 1]] = iA
        }
        return cost[a.length][b.length]
    }

    private fun getWeightedScore(dist: DistanceResult, max:Double): Double {
            return (dist.distance * (1-(dist.distance/max.pow(2))))
    }

    fun getMetaphoneMatrix(input: String): List<List<String>> {
        //tokenize the input
        var splitInput = input.split(' ')
        //splitInput = removeStopWords(splitInput)
        var output: MutableList<List<String>> = mutableListOf()
        splitInput.forEach { output.add(DoubleMetaphone.getDoubleMetaphone(it).toList()) }
        return output.toList()
    }


    private fun compareMetaphoneMatrix(query: List<List<String>>, compareTo: List<List<String>>): Int {
        //keep track of the score
        var scores: MutableList<Int> = mutableListOf()

        //for each word in the query
        for (i in 0 until maxOf(query.count(), compareTo.count())) {
            val queryDM = query.getOrNull(i) ?: listOf(" ", " ")
            val compareDM = compareTo.getOrNull(i) ?: listOf(" ", " ")
            var score = ComputeSingleDoubleMetaphoneScore(queryDM, compareDM)
            scores.add(score)
        }
        //Aggregate the scores
        var totalScore: Int = 0
        scores.forEach { totalScore += it }

        return totalScore
    }

    private fun ComputeSingleDoubleMetaphoneScore(query: List<String>, compareTo: List<String>): Int {
        var primaryScore: Int = 0
        var alternateScore: Int = 0

        //Compute the primary score
        var p = query.first()
        if (p != compareTo.first() || p != compareTo.lastOrNull()) {
            primaryScore = p.length - maxOf(lcs(p, compareTo.first()).count(), lcs(p, compareTo.lastOrNull()
                    ?: "").count())
        }

        //Compute the alternate score
        if (query.count() > 1) {
            var a = query.last()
            if (a != compareTo.first() || a != compareTo.lastOrNull()) {
                alternateScore = a.length - maxOf(lcs(a, compareTo.first()).count(), lcs(a, compareTo.lastOrNull()
                        ?: "").count())
            }
        } else {
            alternateScore = primaryScore
        }

        return minOf(primaryScore, alternateScore)
    }

    //https://rosettacode.org/wiki/Longest_Common_Substring#Kotlin
    private fun lcs(a: String, b: String): String {
        if (a.length > b.length) return lcs(b, a)
        var res = ""
        for (ai in 0 until a.length) {
            for (len in a.length - ai downTo 1) {
                for (bi in 0 until b.length - len) {
                    if (a.regionMatches(ai, b, bi, len) && len > res.length) {
                        res = a.substring(ai, ai + len)
                    }
                }
            }
        }
        return res
    }

}

data class DistanceResult(var value: String, var distance: Double)