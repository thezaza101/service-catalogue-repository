package au.gov.api

import au.gov.api.servicecatalogue.repository.ServiceDescriptionRepositoryImpl
import au.gov.api.servicecatalogue.repository.WebRequestHandler
import org.junit.Assert
import org.junit.Test
import java.lang.Exception

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

    @Test
    fun can_remove_prams_from_uri()
    {
        val originalURI = "https://api.github.com/repos/octocat/Hello-World/pulls?limit=10&cats=dogs"
        val expectedURI = "https://api.github.com/repos/octocat/Hello-World/pulls"

        val actualURI = WebRequestHandler.getBaseURIWithoutPrams(originalURI)
        Assert.assertEquals(expectedURI,actualURI)
    }

    @Test
    fun can_remove_prams_from_no_pram_uri()
    {
        val originalURI = "https://api.github.com/repos/octocat/Hello-World/pulls"
        val expectedURI = "https://api.github.com/repos/octocat/Hello-World/pulls"

        val actualURI = WebRequestHandler.getBaseURIWithoutPrams(originalURI)
        Assert.assertEquals(expectedURI,actualURI)
    }

    @Test
    fun can_flush_cache () {
        var uri = "some_uri?hello=cats"
        var content = "some_content"
        var testCacheEntry = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri,content = content)
        repository.saveCache(testCacheEntry)

        //Ensure the cache works
        val cacheEntry = reqHandler.get(uri)
        Assert.assertEquals(content,cacheEntry)
        val cacheEntryx = reqHandler.getContentFromDatabaseCache(uri)
        Assert.assertEquals(content,cacheEntryx.content)

        reqHandler.flushCache(uri,false)

        var canRetrieveFromDB = true

        try {
            repository.findCacheByURI(uri)
        } catch (e : Exception) {
            canRetrieveFromDB = false
        }
        Assert.assertEquals(false, canRetrieveFromDB)

        var canRetrieveFromCache = true
        try {
            reqHandler.get(uri)
        } catch (e : Exception) {
            canRetrieveFromCache = false
        }
        Assert.assertEquals(false, canRetrieveFromCache)

    }

    @Test
    fun can_flush_cache_ignore_prams () {
        var uri = "some_uri?hello=cats"
        var content = "some_content"
        var testCacheEntry = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri,content = content)
        repository.saveCache(testCacheEntry)

        var uri1 = "some_uri?hello=dogs"
        var content1 = "some_content1"
        var testCacheEntry1 = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri1,content = content1)
        repository.saveCache(testCacheEntry1)

        var uri2 = "some_other_uri?hello=dogs"
        var content2 = "some_content1"
        var testCacheEntry2 = WebRequestHandler.ResponseContentChacheEntry(request_uri = uri2,content = content2)
        repository.saveCache(testCacheEntry2)


        reqHandler.flushCache(uri,true)

        var canRetrieveFromDB = true

        try {
            repository.findCacheByURI(uri)
        } catch (e : Exception) {
            canRetrieveFromDB = false
        }
        Assert.assertEquals(false, canRetrieveFromDB)

        var canRetrieveFromDB1 = true

        try {
            repository.findCacheByURI(uri1)
        } catch (e : Exception) {
            canRetrieveFromDB1 = false
        }
        Assert.assertEquals(false, canRetrieveFromDB1)

        var canRetrieveFromCache = true
        try {
            reqHandler.get(uri)
        } catch (e : Exception) {
            canRetrieveFromCache = false
        }
        Assert.assertEquals(false, canRetrieveFromCache)

        var canRetrieveFromCache1 = true
        try {
            reqHandler.get(uri1)
        } catch (e : Exception) {
            canRetrieveFromCache1 = false
        }
        Assert.assertEquals(false, canRetrieveFromCache1)

        val cacheEntry = reqHandler.get(uri2)
        Assert.assertEquals(content2,cacheEntry)

    }


}