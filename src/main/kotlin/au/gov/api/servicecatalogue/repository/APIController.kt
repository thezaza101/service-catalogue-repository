
package au.gov.api.servicecatalogue.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

import javax.servlet.http.HttpServletRequest

import khttp.get
import khttp.structures.authorization.BasicAuthorization


@RestController
class APIController {

    @Autowired
    private lateinit var repository: ServiceDescriptionRepository

    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    private lateinit var environment: Environment

    @Autowired lateinit var monitor: Monitor


    @ResponseStatus(HttpStatus.FORBIDDEN)
    class UnauthorisedToAccessMonitoring() : RuntimeException()

    @ResponseStatus(HttpStatus.FORBIDDEN)
    class UnauthorisedToModifyServices() : RuntimeException()
    private fun isAuthorisedToSaveService(request:HttpServletRequest, space:String):Boolean{
        if(environment.getActiveProfiles().contains("prod")){
            val AuthURI = System.getenv("AuthURI")?: throw RuntimeException("No environment variable: AuthURI")

            // http://www.baeldung.com/get-user-in-spring-security
            val raw = request.getHeader("authorization")
            val apikey = String(Base64.getDecoder().decode(raw.removePrefix("Basic ")))
        
            val user = apikey.split(":")[0]
            val pass= apikey.split(":")[1]


            val authorisationRequest = get(AuthURI + "/api/canWrite",
                                            params=mapOf("space" to space),
                                            auth=BasicAuthorization(user, pass)
                                       )
            if(authorisationRequest.statusCode != 200) return false
            return authorisationRequest.text == "true"
        }
        return true
    }

    @CrossOrigin
    @GetMapping("/monitor")
    fun test_db_stats(@RequestParam authKey:String):Map<String, Any>?{
        var authKeyEnv: String = System.getenv("authKey") ?: ""
        if(authKey != authKeyEnv) throw UnauthorisedToAccessMonitoring()

        return monitor.getStats()
    }


    @CrossOrigin
    @GetMapping("/new")
    fun newService(request:HttpServletRequest, @RequestParam space:String):ServiceDescription{
        val service = ServiceDescription("NewServiceName", "NewServiceDescription", listOf("# Page1"), listOf(), "")

        if(isAuthorisedToSaveService(request, space)) {
            service.metadata.space=space
            repository.save(service)
            return service
        }

        throw UnauthorisedToModifyServices()

    }


    data class IndexDTO(val content:List<IndexServiceDTO>)
    data class IndexServiceDTO(val id:String, val name:String, val description:String, val tags:List<String>, val logoURI:String, val metadata:Metadata)
    @CrossOrigin
    @GetMapping("/index")
    fun index(): IndexDTO {
        val output = mutableListOf<IndexServiceDTO>()
        for(service in repository.findAll()){
            output.add(IndexServiceDTO(service.id!!, service.currentContent().name, service.currentContent().description, service.tags, service.logo, service.metadata))
        }
        return IndexDTO(output)
    }


    @CrossOrigin
    data class BackupDTO(val content:Iterable<ServiceDescription>)
    @GetMapping("/backup")
    fun backup(): BackupDTO {
        return BackupDTO(repository.findAll())
    }

    @CrossOrigin
    @GetMapping("/service/{id}")
    fun getService(@PathVariable id: String): ServiceDescriptionContent {
        return repository.findById(id).currentContent()
    }




    @CrossOrigin
    @ResponseStatus(HttpStatus.CREATED)  // 201
    @PostMapping("/service")
    fun setService(@RequestBody revision: ServiceDescriptionContent, request:HttpServletRequest): ServiceDescription {

        val service = ServiceDescription(revision.name, revision.description, revision.pages, listOf(), "")
        
        if(isAuthorisedToSaveService(request, service.metadata.space)) {
            repository.save(service)
            return service
        }

        throw UnauthorisedToModifyServices()
    }


}
