package au.gov.api


import au.gov.api.repository.ServiceDescriptionRepositoryImpl
import au.gov.api.repository.ServiceDescription
import org.junit.Assert
import org.junit.Test

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

import com.opentable.db.postgres.embedded.EmbeddedPostgres

class MetaDataTest{

    var repository = ServiceDescriptionRepositoryImpl(MockDataSource())

@Test
    fun by_default_services_have_empty_tags(){

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
    fun services_will_save_tags(){

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




    @Test
    fun testServicesStartWithNoMetadata(){
        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")
        val svc = ServiceDescription(name, description, pages, listOf(), "")
        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)
        val meta = svc2.metadata

        Assert.assertEquals("", meta.agency)
        Assert.assertEquals("", meta.space)
    }


    @Test
    fun testServicesCanSetMetadata(){
        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")
        val svc = ServiceDescription(name, description, pages, listOf(), "")
        svc.metadata.agency = "ato.gov.au"
        svc.metadata.space = "eInvoicing"
        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)
        val meta = svc2.metadata

        Assert.assertEquals("ato.gov.au", meta.agency)
        Assert.assertEquals("eInvoicing", meta.space)
    }

    @Test
    fun testServicesCanBeInvisible(){
        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")
        val svc = ServiceDescription(name, description, pages, listOf(), "")
        svc.metadata.agency = "ato.gov.au"
        svc.metadata.space = "eInvoicing"
        svc.metadata.visibility = false

        repository.save(svc)

        var svc2 = ServiceDescription("", "", pages, listOf(), "")

        try {
            svc2 = repository.findById(svc.id!!)
        } catch (e:RuntimeException)
        {
            Assert.assertNotEquals("ato.gov.au", svc2.metadata.agency)
        }

        svc2 = repository.findById(svc.id!!,true)
        Assert.assertEquals("ato.gov.au", svc2.metadata.agency)

    }
}
