package au.gov.dxa.servicecatalogue.repository.test

// https://gluecoders.github.io/springboot-guide-docs/testing-mongodb-springdata.html

import com.github.fakemongo.Fongo
import com.mongodb.MongoClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration

@Configuration
open class FakeMongo : AbstractMongoConfiguration() {

    protected override fun getDatabaseName(): String {
        return "mockDB"
    }

    @Bean
    override fun mongo(): MongoClient {
        val fongo = Fongo("mockDB")
        return fongo.getMongo()
    }

}