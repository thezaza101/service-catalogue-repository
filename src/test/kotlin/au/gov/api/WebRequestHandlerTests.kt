package au.gov.api

import au.gov.api.servicecatalogue.repository.ServiceDescriptionRepositoryImpl
import au.gov.api.servicecatalogue.repository.WebRequestHandler
import org.junit.Assert
import org.junit.Test

class WebRequestHandlerTests {
    var repository = ServiceDescriptionRepositoryImpl(MockDataSource())
    var reqHandler = WebRequestHandler(repository)


    @Test
    fun can_save_retrieve_with_cache() {
        var uri = "some_uri"
        var content = "some_content"

        var testCacheEntry = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri,content = content)
        repository.saveCache(testCacheEntry)


        val cacheEntry = reqHandler.get(uri)
        Assert.assertEquals(content,cacheEntry)
    }

    @Test
    fun can_update_cache() {
        var uri = "some_uri"
        var content = "some_content"

        var testCacheEntry = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri,content = content)
        repository.saveCache(testCacheEntry)

        var newContent = "some__updated_content"
        var testCacheEntryNew = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri,content = newContent)
        repository.saveCache(testCacheEntryNew)

        val cacheEntry = reqHandler.get(uri)
        Assert.assertEquals(newContent,cacheEntry)
    }


}