package au.gov.api

import au.gov.api.servicecatalogue.repository.ServiceDescriptionRepositoryImpl
import au.gov.api.servicecatalogue.repository.WebRequestHandler
import org.junit.Assert
import org.junit.Test

class WebRequestHandlerTests {
    var repository = ServiceDescriptionRepositoryImpl(MockDataSource())
    var cache = WebRequestHandler(repository)

    fun setup_cache() {

    }

    @Test
    fun can_save_retrieve_with_cache() {
        setup_cache()
        var uri = "some_uri"
        var content = "some_content"

        var testCacheEntry = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri,content = content)
        repository.saveCache(testCacheEntry)


        val cacheEntry = cache.get(uri)
        Assert.assertEquals(content,cacheEntry)
    }


}