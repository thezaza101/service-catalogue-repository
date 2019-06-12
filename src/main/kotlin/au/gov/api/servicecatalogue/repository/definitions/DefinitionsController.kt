package au.gov.api.servicecatalogue.repository.definitions

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class DefinitionsController {
    @Autowired
    lateinit var relationRepository: RelationshipRepository

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

    @Autowired
    lateinit var syntaxRepository: SyntaxRepository

    @CrossOrigin
    @GetMapping("/definitions/syntax")
    fun getSyntax(request: HttpServletRequest, @RequestParam id: String): Syntax {
        return  syntaxRepository.findOne(id)!!
    }

    @Autowired
    lateinit var synonymRepository: SynonymRepository

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
            var d = definitionRepository.getDomainByAcronym(domain) ?: Domain("","","")
            var output:MutableList<Domain> = mutableListOf()
            output.add(d)
            return output
        }
    }

    @CrossOrigin
    @GetMapping("/definitions/definition/count")
    fun getDefinitionsCount(request: HttpServletRequest, @RequestParam(required = false, defaultValue = "") domain: String): Int {
        if(domain=="") {
            return  definitionRepository.howManyDefinitions()
        } else {
            return  definitionRepository.howManyDefinitionsInDomain(domain)
        }
    }
    @Autowired
    private lateinit var dictionaryService: DictionaryService
    @CrossOrigin
    @GetMapping("/definitions/dict")
    fun getDefinitionDictCorrection(request: HttpServletRequest, @RequestParam query: String, @RequestParam(required = false, defaultValue = "")  domains: Array<String>): String {
        return dictionaryService.getDictionaryCorrection(query,domains)
    }
}