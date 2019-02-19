package au.gov.api

import org.junit.Assert
import org.junit.Test

import au.gov.api.servicecatalogue.repository.GitHub

class GitHubTest{


    @Test
    fun can_get_raw_github_uri_from_actual_uri(){

        val actualURI = "https://github.com/apigovau/api-gov-au-definitions/blob/master/api-documentation.md"
        val rawURI = "https://raw.githubusercontent.com/apigovau/api-gov-au-definitions/master/api-documentation.md"

        Assert.assertEquals(rawURI, GitHub.getRawURI(actualURI))
    }


    @Test
    fun can_get_text_of_github_file(){
        val uri="https://github.com/octocat/hello-worId/blob/master/README.md"
        val expectedContents = """hello-worId
===========

My first repository on GitHub.
"""

        Assert.assertEquals(expectedContents, GitHub.getTextOfFlie(uri))
    }


}
