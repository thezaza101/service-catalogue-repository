package au.gov.dxa.servicecatalogue.repository

import org.springframework.data.mongodb.repository.MongoRepository

interface ServiceDescriptionRepository : MongoRepository<ServiceDescription, String> {
    fun findById(id: String): ServiceDescription
}