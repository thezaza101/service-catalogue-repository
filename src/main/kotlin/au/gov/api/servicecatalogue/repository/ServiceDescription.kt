package au.gov.api.servicecatalogue.repository

import com.sun.org.apache.xpath.internal.operations.Bool
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

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
        var firstRevision = ServiceDescriptionRevision(getNewID(name),LocalDateTime.now().toString(), firstContent)
        this.revisions = mutableListOf(firstRevision)
        this.tags = tags.toMutableList()
        this.logo = logo
    }

    override fun toString(): String {
        return String.format(
                "ServiceDescription[id=%s, name='%s']",
                id, currentContent().name)
    }
    fun getRevisionById(revId:String) : ServiceDescriptionRevision? {
        return revisions.find { it.id == revId }
    }
    fun currentRevision(): ServiceDescriptionRevision = revisions.last()
    fun currentContent(): ServiceDescriptionContent = currentRevision().content

    fun revise(name:String, description: String, pages : List<String>, force:Boolean = true){
        var nextContent = ServiceDescriptionContent(name, description, pages)
        var nextRevision = ServiceDescriptionRevision(getNewID(name),LocalDateTime.now().toString(), nextContent)

        if(force){
            revisions.add(nextRevision)
            revisions = revisions.takeLast(REVISION_LIMIT).toMutableList()
        } else {
            if(!checkRevisionEqality(nextRevision, currentRevision())) {
                revisions.add(nextRevision)
                revisions = revisions.takeLast(REVISION_LIMIT).toMutableList()
            }
        }

    }

    fun checkRevisionEqality (rev1: ServiceDescriptionRevision, rev2: ServiceDescriptionRevision) : Boolean {
        val content = rev1.content
        val contentToCompare = rev2.content
        var equal = true

        if (content.name != contentToCompare.name || content.description != contentToCompare.description) equal = false

        if (content.pages.count() != contentToCompare.pages.count()) {
            equal = false
        } else {
            content.pages.forEachIndexed { i, value ->
                if(contentToCompare.pages[i] != value) {
                    equal = false
                }
            }
        }
        return equal
    }

    companion object{
        @JvmStatic
        val REVISION_LIMIT = 10

        @JvmStatic
        fun getNewID(serviceName:String):String {
            fun getNameAcrynm():String{
                if (serviceName.length >= 2) {
                    if(serviceName.contains(' ')) {
                        val split = serviceName.split(' ')
                        var p1 = split[0][0]
                        var p2 = split[1][0]
                        return "$p1$p2"
                    } else {
                        return serviceName.substring(0,1)
                    }
                } else {
                    return serviceName
                }
            }
            val dateTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC).toString()
            var output:String = "#" + getNameAcrynm() + "${dateTime.substring(dateTime.length-6,dateTime.length)}"
            return output
        }
    }

}
