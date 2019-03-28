package au.gov.api.servicecatalogue.repository

import kotlin.collections.Iterable

class RepositoryException() : RuntimeException()

interface ServiceDescriptionRepository {
    fun findById(id: String,returnPrivate:Boolean = false): ServiceDescription
    fun count(): Int
    fun save(service: ServiceDescription)
    fun delete(id:String)
    fun findAll(returnPrivate:Boolean = false): Iterable<ServiceDescription>
}
