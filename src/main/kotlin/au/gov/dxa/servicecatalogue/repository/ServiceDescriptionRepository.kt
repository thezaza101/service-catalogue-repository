package au.gov.dxa.servicecatalogue.repository

import kotlin.collections.Iterable

class RepositoryException() : RuntimeException()

interface ServiceDescriptionRepository {
    fun findById(id: String): ServiceDescription
    fun count(): Int
    fun save(service: ServiceDescription)
    fun findAll(): Iterable<ServiceDescription>
}
