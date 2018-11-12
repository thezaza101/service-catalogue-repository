package au.gov.dxa.servicecatalogue.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Monitor {

    @Autowired
    private lateinit var repository: ServiceDescriptionRepository

    fun getStats():Map<String, Any>{

        val map = mutableMapOf<String, Any>()

        val serviceCount = repository.count()
        val serviceMap = mutableMapOf<String, Any>()
        serviceMap["count"] = serviceCount
        serviceMap["avgRevisions"] = get_total_revisions() / serviceCount
        serviceMap["totalRevisions"] = get_total_revisions()

        map["services"] = serviceMap
        return map
    }

    fun get_total_revisions():Double{
        var revisionCount = 0.0
        for(description in repository.findAll()){
            revisionCount += description.revisions.size
        }
        return revisionCount
    }
}