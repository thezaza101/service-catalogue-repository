package au.gov.dxa.servicecatalogue.repository.test

import au.gov.dxa.servicecatalogue.repository.Application
import au.gov.dxa.servicecatalogue.repository.ServiceDescription
import au.gov.dxa.servicecatalogue.repository.ServiceDescriptionRepository
import com.lordofthejars.nosqlunit.annotation.UsingDataSet
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum
import com.mongodb.MongoClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(classes=[Application::class])
@Import(value = FakeMongo::class)
class MetadataTest{

    @Autowired
    private lateinit var repository: ServiceDescriptionRepository

    @Test
    fun by_default_services_have_empty_metadata(){

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")
        val svc = ServiceDescription(name, description, pages, listOf(), "")
        Assert.assertEquals(listOf<String>(), svc.tags)
        Assert.assertEquals("", svc.logo)
        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)
        Assert.assertEquals(1, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals(name, svc2.currentContent().name)
        Assert.assertEquals(description, svc2.currentContent().description)
        Assert.assertEquals(pages, svc2.currentContent().pages)
        Assert.assertEquals(listOf<String>(), svc2.tags)
        Assert.assertEquals("", svc.logo)

    }



    @Test
    fun services_will_save_metadata(){

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")
        val tags = listOf("Security:Open","Technology:REST")
        val logo = "http://somelogo.com"
        val svc = ServiceDescription(name, description, pages, tags, logo)

        Assert.assertEquals(tags, svc.tags)
        Assert.assertEquals(logo, svc.logo)
        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)
        Assert.assertEquals(1, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals(name, svc2.currentContent().name)
        Assert.assertEquals(description, svc2.currentContent().description)
        Assert.assertEquals(pages, svc2.currentContent().pages)
        Assert.assertEquals(tags, svc.tags)
        Assert.assertEquals(logo, svc.logo)

    }

}
