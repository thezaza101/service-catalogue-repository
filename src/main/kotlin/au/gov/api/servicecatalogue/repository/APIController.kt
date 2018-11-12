
package au.gov.api.servicecatalogue.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

import javax.servlet.http.HttpServletRequest



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

    @CrossOrigin
    @GetMapping("/monitor")
    fun test_db_stats(@RequestParam authKey:String):Map<String, Any>?{
        var authKeyEnv: String = System.getenv("authKey") ?: ""
        if(authKey != authKeyEnv) throw UnauthorisedToAccessMonitoring()

        return monitor.getStats()
    }


    @CrossOrigin
    @GetMapping("/new")
    fun newService(@RequestParam authorization:String):ServiceDescription{
        val service = ServiceDescription("NewServiceName", "NewServiceDescription", listOf("# Page1"), listOf(), "")

        if(isAuthorisedToSaveService(authorization, service)) {
            repository.save(service)
            return service
        }

        throw UnauthorisedToModifyServices()

    }


    data class IndexDTO(val content:List<IndexServiceDTO>)
    data class IndexServiceDTO(val id:String, val name:String, val description:String, val tags:List<String>, val logoURI:String)
    @CrossOrigin
    @GetMapping("/index")
    fun index(): IndexDTO {
        val output = mutableListOf<IndexServiceDTO>()
        for(service in repository.findAll()){
            output.add(IndexServiceDTO(service.id!!, service.currentContent().name, service.currentContent().description, service.tags, service.logo))
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



    @ResponseStatus(HttpStatus.FORBIDDEN)
    class UnauthorisedToModifyServices() : RuntimeException()

    private fun isAuthorisedToSaveService(authorisation:String, service: ServiceDescription):Boolean{
        if(environment.getActiveProfiles().contains("prod")){
            val authKey = environment.getProperty("auth.key")
            return authorisation == authKey
        }
        return true
    }

    @CrossOrigin
    @ResponseStatus(HttpStatus.CREATED)  // 201
    @PostMapping("/service")
    fun setService(@RequestBody revision: ServiceDescriptionContent, request:HttpServletRequest): ServiceDescription {

        // http://www.baeldung.com/get-user-in-spring-security
        val raw = request.getHeader("authorization")
        val authorization = String(Base64.getDecoder().decode(raw.removePrefix("Basic ")))

        val service = ServiceDescription(revision.name, revision.description, revision.pages, listOf(), "")

        if(isAuthorisedToSaveService(authorization, service)) {
            repository.save(service)
            return service
        }

        throw UnauthorisedToModifyServices()
    }


    @CrossOrigin
    @ResponseStatus(HttpStatus.OK)  // 200
    @PostMapping("/service/{id}")
    fun reviseService(@PathVariable id:String, @RequestBody revision: ServiceDescriptionContent, request:HttpServletRequest): ServiceDescriptionContent {

        // http://www.baeldung.com/get-user-in-spring-security
        val raw = request.getHeader("authorization")
        val authorization = String(Base64.getDecoder().decode(raw.removePrefix("Basic ")))

        val service = repository.findById(id)

        if(isAuthorisedToSaveService(authorization, service)) {

            service.revise(revision.name, revision.description, revision.pages)

            repository.save(service)
            return service.currentContent()
        }

        throw UnauthorisedToModifyServices()
    }

}
