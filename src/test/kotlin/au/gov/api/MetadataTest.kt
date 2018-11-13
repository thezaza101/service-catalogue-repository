package au.gov.api


import au.gov.api.servicecatalogue.repository.ServiceDescriptionRepositoryImpl
import au.gov.api.servicecatalogue.repository.ServiceDescription
import org.junit.Assert
import org.junit.Test

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

import com.opentable.db.postgres.embedded.EmbeddedPostgres

class MetaDataTest{

    var repository = ServiceDescriptionRepositoryImpl(MockDataSource())

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
