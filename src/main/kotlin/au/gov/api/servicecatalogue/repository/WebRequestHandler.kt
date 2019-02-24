package au.gov.api.servicecatalogue.repository

import java.util.*
import khttp.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URL
import java.time.LocalDateTime
import kotlin.collections.HashMap

@Component
class WebRequestHandler {
    @Autowired
    private lateinit var repository: ServiceDescriptionRepositoryImpl
    data class ResponseContentChacheEntry (var request_uri:String = "-1", var last_updated:Date = WebRequestHandler.getCurrentDateTime(), var content:String ="")

    var cache:HashMap<String,ResponseContentChacheEntry> = HashMap<String,ResponseContentChacheEntry>()

    fun get(uri:String) : String {
        var output = getContentFromMemoryCache(uri)
        if (output.request_uri=="-1"){
            output = getContentFromDatabaseCache(uri)
            if (output.request_uri=="-1"){
                output = getContentFromWeb(uri)
            }
        }

        var x = output.last_updated.time
        var y = (getCurrentDateTime().time - (60000*30))

        if (x <= y)
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
        var x = ResponseContentChacheEntry(uri, getCurrentDateTime(),response)
        saveContentToDatabase(x)
        cache.put(x.request_uri,x)
        return x
    }

    private fun saveContentToDatabase(content:ResponseContentChacheEntry){
        repository.saveCache(content)
    }

    companion object{
        @JvmStatic
        fun getCurrentDateTime():Date{
            val dt:LocalDateTime = LocalDateTime.now()
            return Date(dt.year,dt.monthValue,dt.dayOfMonth,dt.hour,dt.minute)
        }
    }








}