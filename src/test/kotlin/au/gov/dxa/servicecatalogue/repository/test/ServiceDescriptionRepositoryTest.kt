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
class ServiceDescriptionRepositoryTest {

    @Autowired
    private lateinit var repository: ServiceDescriptionRepository

    @Test
    fun can_save_service(){

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")

        val svc = ServiceDescription(name, description, pages)

        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)

        Assert.assertEquals(1, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals(name, svc2.currentContent().name)
        Assert.assertEquals(description, svc2.currentContent().description)
        Assert.assertEquals(pages, svc2.currentContent().pages)

    }


    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    fun can_save_service_revision(){

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")

        val svc = ServiceDescription(name, description, pages)

        Assert.assertEquals(1, svc.revisions.size)
        Assert.assertNotNull(svc.currentRevision())
        Assert.assertNotNull(svc.currentContent())
        Assert.assertEquals(name, svc.currentContent().name)
        Assert.assertEquals(description, svc.currentContent().description)
        Assert.assertEquals(pages, svc.currentContent().pages)



        val nameD = "ServiceNameDelta"
        val descriptionD = "ServiceDescriptionDelta"
        val pagesD = listOf("Page 1","Page 2", "Page 3")

        svc.revise(nameD, descriptionD, pagesD)

        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)

        Assert.assertEquals(2, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals(nameD, svc2.currentContent().name)
        Assert.assertEquals(descriptionD, svc2.currentContent().description)
        Assert.assertEquals(pagesD, svc2.currentContent().pages)


    }

}