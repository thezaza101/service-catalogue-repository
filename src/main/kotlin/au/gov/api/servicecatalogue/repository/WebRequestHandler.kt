package au.gov.api.servicecatalogue.repository

import java.util.*
import khttp.get
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import kotlin.collections.HashMap

class WebRequestHandler {

    private var repository: ServiceDescriptionRepositoryImpl = ServiceDescriptionRepositoryImpl()

    data class ResponseContentChacheEntry (var request_uri:String = "-1", var last_updated:LocalDateTime = LocalDateTime.now(), var content:String ="")

    var cache:HashMap<String,ResponseContentChacheEntry> = HashMap<String,ResponseContentChacheEntry>()

    fun get(uri:String) : String {
        var output = getContentFromMemoryCache(uri)
        if (output.request_uri=="-1"){
            output = getContentFromDatabaseCache(uri)
            if (output.request_uri=="-1"){
                output = getContentFromWeb(uri)
            }
        }

        if (output.last_updated <= LocalDateTime.now().minusMinutes(30))
            output = getContentFromWeb(uri)

        return output.content
    }

    private fun getContentFromMemoryCache(uri:String) : ResponseContentChacheEntry {
        if(cache.containsKey(uri)){
            return cache.get(uri)!!
        } else {
            return ResponseContentChacheEntry()
        }
    }

    private fun getContentFromDatabaseCache(uri:String) : ResponseContentChacheEntry {
        try{
            var x = repository.findCacheByURI(uri)
            cache.put(x.request_uri,x)
            return x
        } catch (e: Exception){
            return ResponseContentChacheEntry()
        }
    }

    private fun getContentFromWeb(uri:String) : ResponseContentChacheEntry {
        val response = get(url = uri).text
        var x = ResponseContentChacheEntry(uri, LocalDateTime.now(),response)
        saveContentToDatabase(x)
        cache.put(x.request_uri,x)
        return x
    }

    private fun saveContentToDatabase(content:ResponseContentChacheEntry){
        repository.saveCache(content)
    }








}