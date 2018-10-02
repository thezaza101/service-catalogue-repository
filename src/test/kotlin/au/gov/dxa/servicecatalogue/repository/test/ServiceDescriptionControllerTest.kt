package au.gov.dxa.servicecatalogue.repository.test

import au.gov.dxa.servicecatalogue.repository.Application
import au.gov.dxa.servicecatalogue.repository.ServiceDescription
import au.gov.dxa.servicecatalogue.repository.ServiceDescriptionRepository
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime


class ServiceDescriptionControllerTest {


    @Test
    fun when_constructing_new_service_current_content_is_set(){

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")
        val svc = ServiceDescription(name, description, pages, listOf(), "")

        val now = LocalDateTime.now().toString().split("T")[0]

        Assert.assertEquals(1, svc.revisions.size)
        Assert.assertNotNull(svc.currentRevision())
        Assert.assertNotNull(svc.currentContent())
        Assert.assertEquals(name, svc.currentContent().name)
        Assert.assertEquals(description, svc.currentContent().description)
        Assert.assertEquals(pages, svc.currentContent().pages)



        Assert.assertTrue(svc.currentRevision().time.toString().startsWith(now))
    }

    @Test
    fun adding_new_revision_sets_current_content(){
        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")

        val svc = ServiceDescription(name, description, pages, listOf(), "")

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

        Assert.assertEquals(2, svc.revisions.size)
        Assert.assertNotNull(svc.currentRevision())
        Assert.assertNotNull(svc.currentContent())
        Assert.assertEquals(nameD, svc.currentContent().name)
        Assert.assertEquals(descriptionD, svc.currentContent().description)
        Assert.assertEquals(pagesD, svc.currentContent().pages)





    }

}
