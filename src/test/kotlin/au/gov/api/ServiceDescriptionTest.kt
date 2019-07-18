package au.gov.api


import au.gov.api.repository.ServiceDescriptionRepositoryImpl
import au.gov.api.repository.ServiceDescription
import org.junit.Assert
import org.junit.Test

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

import com.opentable.db.postgres.embedded.EmbeddedPostgres

class ServiceDescriptionTest{

    var repository = ServiceDescriptionRepositoryImpl(MockDataSource())

 @Test
    fun can_save_service(){

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")

        val svc = ServiceDescription(name, description, pages, listOf(), "")

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
    fun can_delete_service(){


        val initialCount = repository.count()

        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")

        val svc = ServiceDescription(name, description, pages, listOf(), "")

        repository.save(svc)

        Assert.assertEquals(initialCount + 1, repository.count())

        val svc2 = repository.findById(svc.id!!)

        Assert.assertEquals(1, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals(name, svc2.currentContent().name)
        Assert.assertEquals(description, svc2.currentContent().description)
        Assert.assertEquals(pages, svc2.currentContent().pages)

        repository.delete(svc.id!!)

        Assert.assertEquals(initialCount, repository.count())

    }


    @Test
    fun can_save_service_revision(){

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

        repository.save(svc)


        val svc2 = repository.findById(svc.id!!)
        Assert.assertEquals(2, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals(nameD, svc2.currentContent().name)
        Assert.assertEquals(descriptionD, svc2.currentContent().description)
        Assert.assertEquals(pagesD, svc2.currentContent().pages)
    }

    @Test
    fun repository_will_only_keep_latest_x_revisions(){

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

        for(i in (2..50)){

            val nameD = "ServiceName${i}"
            val descriptionD = "ServiceDescription${i}"
            val pagesD = listOf("Page 1","Page 2", "Page ${i}")
            svc.revise(nameD, descriptionD, pagesD)
            repository.save(svc)
            val svc2 = repository.findById(svc.id!!)

            if( i <= ServiceDescription.REVISION_LIMIT) Assert.assertEquals(i, svc2.revisions.size)
            if( i > ServiceDescription.REVISION_LIMIT) Assert.assertEquals(ServiceDescription.REVISION_LIMIT, svc2.revisions.size)

            Assert.assertNotNull(svc2.currentRevision())
            Assert.assertNotNull(svc2.currentContent())
            Assert.assertEquals(nameD, svc2.currentContent().name)
            Assert.assertEquals(descriptionD, svc2.currentContent().description)
            Assert.assertEquals(pagesD, svc2.currentContent().pages)
        }

        val svc2 = repository.findById(svc.id!!)
        Assert.assertEquals(ServiceDescription.REVISION_LIMIT, svc2.revisions.size)
        Assert.assertNotNull(svc2.currentRevision())
        Assert.assertNotNull(svc2.currentContent())
        Assert.assertEquals("ServiceName50", svc2.currentContent().name)
        Assert.assertEquals("ServiceDescription50", svc2.currentContent().description)
        Assert.assertEquals(listOf("Page 1", "Page 2", "Page 50"), svc2.currentContent().pages)
    }

    @Test
    fun testMockDataSource(){
    var rep = ServiceDescriptionRepositoryImpl(MockDataSource())
    val service = ServiceDescription("NewServiceName", "NewServiceDescription", listOf("# Page1"), listOf(), "")
    rep.save(service)
    Assert.assertEquals(1, rep.count())
    }

    @Test
    fun repositoryCanCompareEquality() {
        val name = "ServiceName"
        val description = "ServiceDescription"
        val pages = listOf("Page 1","Page 2")

        val svc = ServiceDescription(name, description, pages, listOf(), "")

        Assert.assertEquals(1, svc.revisions.count())

        val name1 = "ServiceName"
        val description1 = "ServiceDescription"
        val pages1 = listOf("Page 1","Page 2")
        svc.revise(name1,description1,pages1,false)
        Assert.assertEquals(1, svc.revisions.count())

        val name2 = "ServiceName1"
        val description2 = "ServiceDescription1"
        val pages2 = listOf("Page 1","Page 2")
        svc.revise(name2,description2,pages2,true)
        Assert.assertEquals(2, svc.revisions.count())
    }

}
