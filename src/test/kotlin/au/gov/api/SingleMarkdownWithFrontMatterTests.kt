package au.gov.api

import org.junit.Assert
import org.junit.Test

import au.gov.api.repository.SingleMarkdownWithFrontMatter

class SingleMarkdownWithFrontMatterTest{

    val frontMatter = """name: "An API"
description: "A Description"
logo : "https://api.gov.au/img/catalogue_brand.png"
tags:
 - "Security:Open"
 - "Technology:Rest/JSON"
 - "OpenAPISpec:Swagger"
 - "AgencyAcr:ATO"
 - "Status:Published"
 - "Category:Metadata"
"""


    val content = """# Page1

## A heading on page 1

some content about page 1

# Page2

## A heading on page 2

some content about page 2


"""
    val example = """---
${frontMatter}
---
${content}
"""



    val mdfm = SingleMarkdownWithFrontMatter(example)

    @Test
    fun can_get_front_matter(){
        Assert.assertEquals(frontMatter, mdfm.frontMatter)
    }

    @Test
    fun can_get_front_content(){
        Assert.assertEquals(content, mdfm.content)
    }


    @Test
    fun test_there_are_two_pages(){
        Assert.assertEquals(2, mdfm.pages.size)
        Assert.assertTrue(mdfm.pages[0].startsWith("# Page1")) 
        Assert.assertTrue(mdfm.pages[1].startsWith("# Page2")) 
    }


    @Test
    fun can_get_metadata(){
        Assert.assertEquals("An API", mdfm.yaml["name"])
        Assert.assertEquals("A Description", mdfm.yaml["description"])
    }


    @Test
    fun can_get_service_description(){
        val sd = mdfm.serviceDescription

        Assert.assertEquals(1, sd.revisions.size)
        Assert.assertEquals("An API", sd.currentContent().name)
        Assert.assertEquals("A Description", sd.currentContent().description)

    }

}
