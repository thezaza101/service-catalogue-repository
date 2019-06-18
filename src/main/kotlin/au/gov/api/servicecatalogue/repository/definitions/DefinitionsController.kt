package au.gov.api.servicecatalogue.repository.definitions

import au.gov.api.config.Config
import au.gov.api.servicecatalogue.repository.APIController
import au.gov.api.servicecatalogue.repository.Event
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.fasterxml.jackson.databind.ObjectMapper
import khttp.get
import khttp.structures.authorization.BasicAuthorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.constraints.Null

@RestController
class DefinitionsController {
    @Autowired
    lateinit var relationRepository: RelationshipRepository

    @Autowired
    lateinit var syntaxRepository: SyntaxRepository

    @Autowired
    lateinit var synonymRepository: SynonymRepository

    @Autowired
    private lateinit var dictionaryService: DictionaryService

    @Autowired
    private lateinit var environment: Environment

    private fun isAuthorisedToSaveDefinition(request:HttpServletRequest, space:String):Boolean{
        if(environment.getActiveProfiles().contains("prod")){
            val AuthURI = Config.get("AuthURI")

            // http://www.baeldung.com/get-user-in-spring-security
            val raw = request.getHeader("authorization")
            if (raw==null) return false;
            val apikey = String(Base64.getDecoder().decode(raw.removePrefix("Basic ")))

            val user = apikey.split(":")[0]
            val pass= apikey.split(":")[1]


            val authorisationRequest = get(AuthURI + "api/canWrite",
                    params=mapOf("space" to space),
                    auth= BasicAuthorization(user, pass)
            )
            if(authorisationRequest.statusCode != 200) return false
            return authorisationRequest.text == "true"
        }
        return true
    }

    private fun logEvent(request:HttpServletRequest, action:String, type:String, name:String, reason:String,content:String = "") {
        Thread(Runnable {
            print("Logging Event...")
            // http://www.baeldung.com/get-user-in-spring-security
            val raw = request.getHeader("authorization")
            val logURL = Config.get("LogURI")+"new"
            if (raw==null) throw RuntimeException()
            val user = String(Base64.getDecoder().decode(raw.removePrefix("Basic "))).split(":")[0]
            val parser: Parser = Parser()
            var eventPayload: JsonObject = parser.parse(StringBuilder(Klaxon().toJsonString(Event(user,action,type,name,reason,content)))) as JsonObject
            val eventAuth = System.getenv("LogAuthKey")
            val eventAuthUser = eventAuth.split(":")[0]
            val eventAuthPass = eventAuth.split(":")[1]
            var x = khttp.post(logURL,auth=BasicAuthorization(eventAuthUser, eventAuthPass),json = eventPayload)
            println("Status:"+x.statusCode)
        }).start()
    }

    //Get requests
    @CrossOrigin
    @GetMapping("/definitions/relationships")
    fun getRelationshipsForId(request: HttpServletRequest, @RequestParam id: String): Map<String,List<Result>> {
        return relationRepository.getRelationshipFor(id)
    }

    @CrossOrigin
    @GetMapping("/definitions/relationships/meta")
    fun getMetaForRelationshipType(request: HttpServletRequest, @RequestParam relationType: String): Meta {
        return  relationRepository.getMeta(relationType)
    }

    @CrossOrigin
    @GetMapping("/definitions/syntax")
    fun getSyntax(request: HttpServletRequest, @RequestParam id: String): Syntax {
        return  syntaxRepository.findOne(id)!!
    }

    @CrossOrigin
    @GetMapping("/definitions/synonyms")
    fun getSynonym(request: HttpServletRequest): MutableList<List<String>> {
        return  synonymRepository.origSynonyms
    }
    @CrossOrigin
    @GetMapping("/definitions/synonyms/{word}")
    fun getSynonymByText(request: HttpServletRequest, @RequestParam word: String): List<String>? {
        return  synonymRepository.getSynonym(word)
    }

    @CrossOrigin
    @GetMapping("/definitions/synonyms/expand")
    fun getExpandedSynonym(request: HttpServletRequest, @RequestParam query: String): SynonymExpansionResults {
        return  synonymRepository.expand(query)
    }

    @Autowired
    lateinit var definitionRepository: DefinitionRepository

    @CrossOrigin
    @GetMapping("/definitions")
    fun getDefinitions(request: HttpServletRequest, @RequestParam pageNumber: Int, @RequestParam pageSize: Int, @RequestParam(required = false, defaultValue = "") domain: String): List<Definition> {
        when (domain=="") {
            true -> return  definitionRepository.findAll(pageNumber, pageSize)
            false -> return definitionRepository.findAllInDomain(pageNumber, pageSize,domain)
        }
    }

    @CrossOrigin
    @GetMapping("/definitions/search")
    fun searchDefinitions(request: HttpServletRequest, @RequestParam query: String, @RequestParam(required = false, defaultValue = "")  domain: Array<String>, @RequestParam page: Int, @RequestParam size: Int, @RequestParam raw:Boolean, @RequestParam ignoreSynonym: Boolean): SearchResults {
        var dom = ""
        domain.forEach { dom +=  "$it " }
        dom = dom.trim()
        return definitionRepository.search(query,dom,page,size,raw,ignoreSynonym)
    }

    @CrossOrigin
    @GetMapping("/definitions/definition/detail")
    fun getDefinitionDetail(request: HttpServletRequest, @RequestParam id: String): Definition {
        return  definitionRepository.findOne(id)
    }

    @CrossOrigin
    @GetMapping("/definitions/domains")
    fun getDefinitionDomains(request: HttpServletRequest, @RequestParam(required = false, defaultValue = "") domain: String): List<Domain> {
        if(domain=="") {
            return  definitionRepository.getDomains()
        } else {
            val d = definitionRepository.getDomainByAcronym(domain) ?: Domain("","","")
            var output:MutableList<Domain> = mutableListOf()
            output.add(d)
            return output
        }
    }

    @CrossOrigin
    @GetMapping("/definitions/definition/count")
    fun getDefinitionsCount(request: HttpServletRequest, @RequestParam(required = false, defaultValue = "") domain: String): Int {
        when(domain=="") {
            true -> return definitionRepository.howManyDefinitions()
            false -> return  definitionRepository.howManyDefinitionsInDomain(domain)
        }
    }

    @CrossOrigin
    @GetMapping("/definitions/dict")
    fun getDefinitionDictCorrection(request: HttpServletRequest, @RequestParam query: String, @RequestParam(required = false, defaultValue = "")  domains: Array<String>): String {
        return dictionaryService.getDictionaryCorrection(query,domains)
    }


    //Post requests

    @ResponseStatus(HttpStatus.FORBIDDEN)
    class Unauthorised() : RuntimeException()

    //Add synonym
    @CrossOrigin
    @PostMapping("/definitions/synonyms")
    fun postSynonym(request: HttpServletRequest, @RequestParam replace: Boolean, @RequestParam(required = false, defaultValue="false") remove:Boolean, @RequestBody synonyms:List<String>) {
        if(isAuthorisedToSaveDefinition(request,"admin")) {
            val (validated,existing) = synonymRepository.validateNewSynonym(synonyms)
            if(!remove) {
                if (!validated) throw Exception("List cannot be validated")
                if(existing!=null) {
                    if (replace) {
                        synonymRepository.replaceSynonyms(existing!!,synonyms)
                        logEvent(request, "Updated", "Synonym", ObjectMapper().writeValueAsString(existing!!),"replace", ObjectMapper().writeValueAsString(synonyms))
                    } else {
                        val newList = synonyms.toMutableList()
                        newList.addAll(existing!!)
                        synonymRepository.replaceSynonyms(existing!!,newList.distinct())
                        logEvent(request, "Updated", "Synonym", ObjectMapper().writeValueAsString(existing!!), "add" ,ObjectMapper().writeValueAsString(newList.distinct()))
                    }
                } else {
                    synonymRepository.saveSynonym(synonyms)
                    logEvent(request, "Updated", "Synonym", ObjectMapper().writeValueAsString(synonyms), "new")
                }
            } else {
                if(existing!=null) {
                    synonymRepository.removeSynonyms(synonyms)
                    logEvent(request, "Deleted", "Synonym", ObjectMapper().writeValueAsString(synonyms), "Delete")
                }

            }
        }else { throw Unauthorised() }
    }


    //Post syntax
    @CrossOrigin
    @PostMapping("/definitions/syntax")
    fun postSyntax(request: HttpServletRequest,@RequestParam id: String,  @RequestBody syntaxs:Map<String,Map<String,Map<String, String>>>) {
        if(isAuthorisedToSaveDefinition(request,"admin")) {
            try {
                val definition = definitionRepository.getDefinitionById(id)
            } catch (e: Exception) {
                throw Exception("Identifier does not exist", e)
            }
            syntaxRepository.saveSyntax(id, syntaxs)
            logEvent(request, "Updated", "Syntax", id, ObjectMapper().writeValueAsString(syntaxs))
        } else { throw Unauthorised() }


    }

    //Post relationship
    @CrossOrigin
    @PostMapping("/definitions/relationships")
    fun postRelationship(request: HttpServletRequest, @RequestBody relationship: RelationshipRepository.NewRelationship) {
        if ((relationship.type =="").or(relationship.content.first == "").or(relationship.content.second == "" )) throw Exception("Required values are empty")
        if(isAuthorisedToSaveDefinition(request,"admin")) {
        try{
            val first = definitionRepository.getDefinitionById(relationship.content.first)
            val second = definitionRepository.getDefinitionById(relationship.content.second)
        } catch (e:Exception) {throw Exception("Identifier does not exist", e)}

            relationRepository.saveRelationship(relationship)
            logEvent(request,"Created","Relationship",relationship.content.first,relationship.content.second)
        } else { throw Unauthorised() }
    }
}