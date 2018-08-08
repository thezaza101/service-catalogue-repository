package au.gov.dxa.servicecatalogue.repository

import org.springframework.data.annotation.Id
import java.time.LocalDateTime

data class ServiceDescriptionContent(val name:String = "", val description:String = "", val pages:List<String> = listOf(""))
data class ServiceDescriptionRevision(val time:String, val content: ServiceDescriptionContent)


class ServiceDescription {

    @Id
    var id: String? = null
    var revisions: MutableList<ServiceDescriptionRevision> = mutableListOf()

    constructor (){}

    constructor (name:String, description: String, pages : List<String>){
        var firstContent = ServiceDescriptionContent(name, description, pages)
        var firstRevision = ServiceDescriptionRevision(LocalDateTime.now().toString(), firstContent)

        this.revisions = mutableListOf(firstRevision)

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
        var nextRevision = ServiceDescriptionRevision(LocalDateTime.now().toString(), nextContent)

        revisions.add(nextRevision)
    }
}