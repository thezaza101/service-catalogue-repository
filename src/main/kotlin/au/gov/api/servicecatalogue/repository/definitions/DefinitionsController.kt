package au.gov.api.servicecatalogue.repository.definitions

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

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


    //Add synonym
    @CrossOrigin
    @PostMapping("/definitions/synonyms")
    fun postSynonym(request: HttpServletRequest, synonyms:Array<String>) {

    }

}