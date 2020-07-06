package au.gov.api.repository

import org.yaml.snakeyaml.Yaml


class SingleMarkdownWithFrontMatter(rawContent: String) {

    val frontMatter: String
    private var contentStartsAtLine = 0
    val content: String
    val lines = rawContent.lines()
    val pages: List<String>
    val yaml: Map<String, Any>
    val serviceDescription: ServiceDescription
    val name: String
    val description: String
    val tags: List<String>

    init {
        frontMatter = extractFrontMatter()
        yaml = Yaml().load(frontMatter)

        name = yaml["name"] as String? ?: "Untitled"
        description = yaml["description"] as String? ?: "No description available"

        content = lines.drop(contentStartsAtLine).joinToString(separator = "\n").removeSuffix("\n")
        pages = splitPages()
        tags = getTagsFromYaml()
        serviceDescription = createServiceDescription()

    }

    fun getTagsFromYaml(): List<String> {
        var tmpTags = yaml["tags"] as List<String>
        var output = tmpTags ?: mutableListOf<String>()
        return output
    }


    private fun createServiceDescription(): ServiceDescription {

        val logo: String = yaml["logo"] as String? ?: ""

        val service = ServiceDescription(name, description, pages, tags, logo)

        return service
    }


    @Throws(RuntimeException::class)
    private fun extractFrontMatter(): String {

        if (lines.count { isFrontMatterBoundry(it) } != 2) throw RuntimeException("There weren't two '---' lines that wrap the FrontMatter")

        var extract = ""
        var insideFrontMatter = false

        for (line in lines) {
            contentStartsAtLine++
            if (isFrontMatterBoundry(line) && insideFrontMatter) {
                return extract
            }
            if (isFrontMatterBoundry(line) && !insideFrontMatter) {
                insideFrontMatter = true
                continue
            }
            if (insideFrontMatter && !isLineEmpty(line)) extract += line + "\n"
        }

        throw RuntimeException("There was a processing error. Please contact the api.gov.au team")
    }

    private fun splitPages(): List<String> {
        val thePages = mutableListOf<String>()
        var currentPage = ""
        for (line in content.lines()) {
            if (isH1(line) && currentPage.replace("\n", "") != "") {
                thePages.add(currentPage)
                currentPage = ""
            }
            currentPage += line + "\n"
        }

        thePages.add(currentPage)

        return thePages
    }


    private fun isFrontMatterBoundry(line: String) = line == "---"
    private fun isLineEmpty(line: String) = line.replace(" ", "") == ""
    private fun isH1(line: String) = line.startsWith("# ")

}
