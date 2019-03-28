package au.gov.api.servicecatalogue.repository

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

data class ServiceDescriptionContent(val name:String = "", val description:String = "", val pages:List<String> = listOf(""))
data class ServiceDescriptionRevision(val id: String ="", val time:String = "", val content: ServiceDescriptionContent = ServiceDescriptionContent())


data class Metadata(var agency:String = "", var space:String = "", var visibility:Boolean = true, var ingestSource:String = "", var NumberOfConversations:Int = 0)

class ServiceDescription {
    

    @Id
    var id: String? = null
    var revisions: MutableList<ServiceDescriptionRevision> = mutableListOf()
    var tags: MutableList<String> = mutableListOf()
    var logo: String = ""
    var metadata = Metadata()

    constructor (){}

/*    constructor (name:String, description: String, pages : List<String>){
        var firstContent = ServiceDescriptionContent(name, description, pages)
        var firstRevision = ServiceDescriptionRevision(LocalDateTime.now().toString(), firstContent)
        this.revisions = mutableListOf(firstRevision)
    }
 */
    constructor (name:String, description: String, pages : List<String>, tags : List<String>, logo: String){
        var firstContent = ServiceDescriptionContent(name, description, pages)
        var firstRevision = ServiceDescriptionRevision(LocalDateTime.now().toString().substring(0,6),LocalDateTime.now().toString(), firstContent)
        this.revisions = mutableListOf(firstRevision)
        this.tags = tags.toMutableList()
        this.logo = logo
    }

    override fun toString(): String {
        return String.format(
                "ServiceDescription[id=%s, name='%s']",
                id, currentContent().name)
    }

    fun currentRevision(): ServiceDescriptionRevision = revisions.last()
    fun currentContent(): ServiceDescriptionContent = currentRevision().content

    fun revise(name:String, description: String, pages : List<String>){
        var nextContent = ServiceDescriptionContent(name, description, pages)
        var nextRevision = ServiceDescriptionRevision(LocalDateTime.now().toString().substring(0,6),LocalDateTime.now().toString(), nextContent)

        revisions.add(nextRevision)
        revisions = revisions.takeLast(REVISION_LIMIT).toMutableList()
    }

    companion object{
        @JvmStatic
        val REVISION_LIMIT = 10
    }

}
