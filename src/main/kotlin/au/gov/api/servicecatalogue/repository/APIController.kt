
package au.gov.api.servicecatalogue.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

import javax.servlet.http.HttpServletRequest

import khttp.get
import khttp.structures.authorization.BasicAuthorization

import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.beust.klaxon.JsonObject

import au.gov.api.config.*
import au.gov.api.servicecatalogue.Diff.HTMLDiffOutputGenerator
import au.gov.api.servicecatalogue.Diff.MyersDiff
import au.gov.api.servicecatalogue.Diff.TextDiff
import java.sql.Timestamp

@RestController
class APIController {

    @Autowired
    private lateinit var repository: ServiceDescriptionRepository

    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var ghapi:GitHub

    @ResponseStatus(HttpStatus.FORBIDDEN)
    class UnauthorisedToModifyServices() : RuntimeException()
    class UnauthorisedToViewServices() : RuntimeException()

    @ResponseStatus(HttpStatus.NOT_FOUND)
    class NoContentFound(override val message: String?) : java.lang.Exception()

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    class InvallidRequest(override val message: String?) : java.lang.Exception()

    private fun isAuthorisedToSaveService(request:HttpServletRequest, space:String):Boolean{
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
                                            auth=BasicAuthorization(user, pass)
                                       )
            if(authorisationRequest.statusCode != 200) return false
            return authorisationRequest.text == "true"
        }
        return true
    }

    data class Event(var key:String = "", var action:String = "", var type:String = "", var name:String = "", var reason:String = "")

    private fun logEvent(request:HttpServletRequest, action:String, type:String, name:String, reason:String) {
        Thread(Runnable {
            print("Logging Event...")
            // http://www.baeldung.com/get-user-in-spring-security
            val raw = request.getHeader("authorization")
            val logURL = Config.get("LogURI")+"new"
            if (raw==null) throw RuntimeException()
            val user = String(Base64.getDecoder().decode(raw.removePrefix("Basic "))).split(":")[0]
            val parser:Parser = Parser()
            var eventPayload:JsonObject = parser.parse(StringBuilder(Klaxon().toJsonString(Event(user,action,type,name,reason)))) as JsonObject
            val eventAuth = System.getenv("LogAuthKey")
            val eventAuthUser = eventAuth.split(":")[0]
            val eventAuthPass = eventAuth.split(":")[1]
            var x = khttp.post(logURL,auth=BasicAuthorization(eventAuthUser, eventAuthPass),json = eventPayload)
            println("Status:"+x.statusCode)
        }).start()
    }


	fun writableSpaces(request:HttpServletRequest):List<String>{

            val AuthURI = Config.get("AuthURI")

            // http://www.baeldung.com/get-user-in-spring-security
            val raw = request.getHeader("authorization")
            if (raw==null) return listOf();
            val apikey = String(Base64.getDecoder().decode(raw.removePrefix("Basic ")))
            val user = apikey.split(":")[0]
            val pass= apikey.split(":")[1]


            val authorisationRequest = get(AuthURI + "api/spaces",
                                            auth=BasicAuthorization(user, pass)
                                       )
            if(authorisationRequest.statusCode != 200) return listOf()

			println(authorisationRequest.text)
			println(authorisationRequest.jsonArray)

			val spaces = mutableListOf<String>()
			
			for (i in 0..(authorisationRequest.jsonArray!!.length() - 1)) {
			    val item = authorisationRequest.jsonArray!![i].toString()
				spaces.add(item)
			}
			
			return spaces


	}


    @CrossOrigin
    @GetMapping("/new")
    fun newService(request:HttpServletRequest, @RequestParam space:String):ServiceDescription{
        val service = ServiceDescription("NewServiceName", "NewServiceDescription", listOf("# Page1"), listOf(), "")

        if(isAuthorisedToSaveService(request, space)) {
            service.metadata.space=space
            service.metadata.visibility = false
            repository.save(service)
            try {
                logEvent(request,"Created","Service",service.id!!,service.revisions.first().content.name)
            }
            catch (e:Exception)
            { println(e.message)}

            return service
        }

        throw UnauthorisedToModifyServices()
    }


    data class IndexDTO(val content:List<IndexServiceDTO>)
    data class IndexServiceDTO(val id:String, val name:String, val description:String, val tags:List<String>, val logoURI:String, val metadata:Metadata)
    @CrossOrigin
    @GetMapping("/index")
    fun index(request:HttpServletRequest): IndexDTO {
        val output = mutableListOf<IndexServiceDTO>()
        val auth = isAuthorisedToSaveService(request,"admin")

        for(service in repository.findAll(auth)){
            val ingestSrc = service.metadata.ingestSource
            if (ingestSrc.contains("github",true)) {

            }

                output.add(IndexServiceDTO(service.id!!, service.currentContent().name, service.currentContent().description, service.tags, service.logo, service.metadata))
        }

        return IndexDTO(output)
    }



    @CrossOrigin
    @GetMapping("/indexWritable")
    fun indexWritable(request:HttpServletRequest): IndexDTO {
        val output = mutableListOf<IndexServiceDTO>()
        val spaces = writableSpaces(request)
        for(service in repository.findAll()){
				if(service.metadata.space in spaces || "admin" in spaces) output.add(IndexServiceDTO(service.id!!, service.currentContent().name, service.currentContent().description, service.tags, service.logo, service.metadata))
        }
        return IndexDTO(output)
    }



/*

turn this off for now to prevent !visibility data leaking out

    @CrossOrigin
    data class BackupDTO(val content:Iterable<ServiceDescription>)
    @GetMapping("/backup")
    fun backup(): BackupDTO {
        return BackupDTO(repository.findAll())
    }
*/
    @CrossOrigin
    @GetMapping("/colab/{id}")
    fun getColab(request:HttpServletRequest,
                 @PathVariable id: String,
                 @RequestParam(required = false, defaultValue = "false") showall: Boolean,
                 @RequestParam(required = false, defaultValue = "true") sort: Boolean,
                 @RequestParam(required = false, defaultValue = "false") flush: Boolean,
                 @RequestParam(required = false, defaultValue = "15") size: Int,
                 @RequestParam(defaultValue = "1") page: Int
                 ): PageResult<GitHub.Conversation> {
        val service = repository.findById(id,false)
        val ingestSrc = service.metadata.ingestSource
        if (ingestSrc.contains("github",true))
        {
            if (flush)
            {
                ghapi.clearCacheForRepo(GitHub.getUserGitHubUri(ingestSrc),GitHub.getRepoGitHubUri(ingestSrc))
            }
            var fullList = ghapi.getGitHubConvos(GitHub.getUserGitHubUri(ingestSrc),GitHub.getRepoGitHubUri(ingestSrc),showall)
            return PageResult(ghapi.getGitHubConvosHATEOS(fullList,sort,size,page),URLHelper().getURL(request),fullList.count())
        }else{
            throw APIController.NoContentFound("This service is not connected to github")
        }
    }

    @CrossOrigin
    @GetMapping("/colab/{id}/comments")
    fun getColabComments(request:HttpServletRequest,
                 @PathVariable id: String,
                 @RequestParam(required = false, defaultValue = "") convoId: String,
                 @RequestParam(required = false, defaultValue = "") convoType: String,
                 @RequestParam(required = false, defaultValue = "false") flush: Boolean,
                 @RequestParam(required = false, defaultValue = "15") size: Int,
                 @RequestParam(defaultValue = "1") page: Int
    ): PageResult<GitHub.Comment> {
        if (convoId=="" || convoType=="") throw NoContentFound("The 'convoId' and 'convoType' parameters must be set")
        val service = repository.findById(id,false)
        val ingestSrc = service.metadata.ingestSource
        if (ingestSrc.contains("github",true))
        {
            if (flush)
            {
                ghapi.clearCacheForRepo(GitHub.getUserGitHubUri(ingestSrc),GitHub.getRepoGitHubUri(ingestSrc))
            }
            var fullList = ghapi.getComments(GitHub.getUserGitHubUri(ingestSrc),GitHub.getRepoGitHubUri(ingestSrc),convoType,convoId)
            return PageResult(ghapi.getGitHubCommentsHATEOS(fullList,size,page),URLHelper().getURL(request),fullList.count())
        }else{
            throw APIController.NoContentFound("This service is not connected to github")
        }
    }

    @CrossOrigin
    @GetMapping("/colab/{id}/comments/count")
    fun getColabComments(request:HttpServletRequest,
                         @PathVariable id: String,
                         @RequestParam(required = false, defaultValue = "false") countClosed: Boolean,
                         @RequestParam(required = false, defaultValue = "true") countPRComments: Boolean,
                         @RequestParam(defaultValue = "1") page: Int
    ): Int {
        val service = repository.findById(id,false)
        val ingestSrc = service.metadata.ingestSource
        if (ingestSrc.contains("github",true))
        {
            var convoCount = ghapi.getConvoCount(GitHub.getUserGitHubUri(ingestSrc),GitHub.getRepoGitHubUri(ingestSrc),countClosed,countPRComments)
            var serviceMetadata = service.metadata
            serviceMetadata.NumberOfConversations = convoCount
            service.metadata = serviceMetadata
            repository.save(service)
            return convoCount
        }else{
            throw APIController.NoContentFound("This service is not connected to github")
        }
    }

    @CrossOrigin
    @GetMapping("/service/{id}")
    fun getService(request:HttpServletRequest, @PathVariable id: String): ServiceDescriptionContent {
        val auth = isAuthorisedToSaveService(request,"admin")
        try{
            val service = repository.findById(id,auth)
            return service.currentContent()
        } catch (e:Exception){
            throw UnauthorisedToViewServices()
        }
    }


    data class ServiceDescriptionRevisionMetadata (var id:String, var timestamp: String)
    @CrossOrigin
    @GetMapping("/service/{id}/revisions")
    fun getServiceRevisions(request:HttpServletRequest, @PathVariable id: String): List<ServiceDescriptionRevisionMetadata> {
        val auth = isAuthorisedToSaveService(request,"admin")
        try{
            val service = repository.findById(id,auth)
            var outputList = mutableListOf<ServiceDescriptionRevisionMetadata>()
            service.revisions.forEachIndexed { index, element ->
                outputList.add(ServiceDescriptionRevisionMetadata(element.id,element.time))
            }
            return outputList
        } catch (e:Exception){
            throw UnauthorisedToViewServices()
        }
    }

    @CrossOrigin
    @GetMapping("/service/{id}/compare")
    fun getServiceComparison(request:HttpServletRequest, @PathVariable id: String,
                             @RequestParam(required = false, defaultValue = "") original: String,
                             @RequestParam(required = false, defaultValue = "") new: String,
                             @RequestParam(required = false, defaultValue = "99") page: Int,
                             @RequestParam(required = false,defaultValue = "false") lines:Boolean): String {
        if (original=="" || new=="" || page == 99) throw NoContentFound("The 'original', 'new', and 'page' parameters must be set")
        val auth = isAuthorisedToSaveService(request,"admin")
        try{
            val service = repository.findById(id,auth)
            val originalRev = service.getRevisionById("#"+original)
            val newRev = service.getRevisionById("#"+new)

            var originalPage = ""
            var newPage = ""
            try {
                originalPage = originalRev!!.content.pages[page]
            } catch (e:Exception) {}

            try {
                newPage = newRev!!.content.pages[page]
            } catch (e:Exception) {}

            val diffObj = TextDiff(MyersDiff(lines), HTMLDiffOutputGenerator("span","style",lines))
            return diffObj.generateDiffOutput(originalPage,newPage)
        } catch (e:Exception){
            throw UnauthorisedToViewServices()
        }
    }




    @CrossOrigin
    @PostMapping("/metadata/{id}")
    fun setMetadata(@RequestBody metadata: Metadata, @PathVariable id:String, @RequestParam(required = false, defaultValue = "") logo: String,  request:HttpServletRequest): Metadata{

        val auth = isAuthorisedToSaveService(request, "admin")
        val service = repository.findById(id,auth)
        
        if(auth) {
            service.metadata = metadata
            if(logo!="") service.logo = logo
            repository.save(service)
            try {
                logEvent(request,"Updated","Service",service.id!!,"Metadata")
            }
            catch (e:Exception)
            { println(e.message)}
            return service.metadata
        }

        throw UnauthorisedToModifyServices()
    }

    @ResponseStatus(HttpStatus.CREATED)  // 201
    @CrossOrigin
    @PostMapping("/service")
    fun setService(@RequestBody revision: ServiceDescriptionContent, request:HttpServletRequest): ServiceDescription {

        val service = ServiceDescription(revision.name, revision.description, revision.pages, listOf(), "")
        
        if(isAuthorisedToSaveService(request, service.metadata.space)) {
            repository.save(service)
            try {
                logEvent(request,"Created","Service",service.id!!,revision.name)
            }
            catch (e:Exception)
            { println(e.message)}
            return service
        }

        throw UnauthorisedToModifyServices()
    }

    @CrossOrigin
    @ResponseStatus(HttpStatus.OK)  // 200
    @PostMapping("/service/{id}")
    fun reviseService(@PathVariable id:String, @RequestBody revision: ServiceDescriptionContent, request:HttpServletRequest): ServiceDescriptionContent {
        val service = repository.findById(id)

        if(isAuthorisedToSaveService(request, service.metadata.space)) {

            service.revise(revision.name, revision.description, revision.pages)

            repository.save(service)
            var toRevision = service.revisions.count()
            var fromRevision = toRevision-1
            try {
                logEvent(request,"Updated","Service",service.id!!,"Revision from $fromRevision to $toRevision")
            }
            catch (e:Exception)
            { println(e.message)}
            return service.currentContent()
        }

        throw UnauthorisedToModifyServices()
    }


    @CrossOrigin
    @ResponseStatus(HttpStatus.OK)  // 200
    @DeleteMapping("/service/{id}")
    fun deleteService(@PathVariable id:String, request:HttpServletRequest) {
        val service = repository.findById(id, true)

        if(isAuthorisedToSaveService(request, service.metadata.space)) {

            repository.delete(id)
            try {
                logEvent(request,"Deleted","Service",service.id!!,"Deleted")
            }
            catch (e:Exception)
            { println(e.message)}
            return
        }

        throw UnauthorisedToModifyServices()
    }



}
