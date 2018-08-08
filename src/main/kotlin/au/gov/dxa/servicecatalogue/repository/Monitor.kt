package au.gov.dxa.servicecatalogue.repository

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Monitor {

    @Value("\${spring.data.mongodb.uri}")
    private lateinit var springDataMongodbUri: String

    @Autowired
    private lateinit var mongoClient: MongoClient

    @Autowired
    private lateinit var repository: ServiceDescriptionRepository

    fun getStats():Map<String, Any>{

        val map = mutableMapOf<String, Any>()

        val serviceCount = repository.count()
        val serviceMap = mutableMapOf<String, Any>()
        serviceMap["count"] = serviceCount
        serviceMap["avgRevisions"] = get_total_revisions() / serviceCount
        serviceMap["totalRevisions"] = get_total_revisions()

        map["mongo"] = get_mongo_stats()
        map["services"] = serviceMap
        return map
    }


    fun get_mongo_stats():Map<String,Any> {
        var dbName = springDataMongodbUri.split("/").last()
        var db = mongoClient.getDatabase(dbName)
        return (db.runCommand(Document("dbStats", 1)).toMap<String, Any>())
    }


    fun get_total_revisions():Double{
        var revisionCount = 0.0
        for(description in repository.findAll()){
            revisionCount += description.revisions.size
        }
        return revisionCount
    }
}