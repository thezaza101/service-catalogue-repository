
import au.gov.api.servicecatalogue.repository.definitions.Definition
import au.gov.api.servicecatalogue.repository.definitions.DictionaryService
import org.junit.Assert
import org.junit.Test



class DictionaryServiceTest{

    val service:DictionaryService = DictionaryService()

    @Test
    fun Test_Dict_correction(){
        Assert.assertEquals("Electronic Contact Facsimile Area Code",service.runQuery("ElectroCotacsimiAreaode",getTestDefList()))
        Assert.assertEquals("Address",service.runQuery("Badress",getTestDefList()))
        Assert.assertEquals("Surname",service.runQuery("Firname",getTestDefList()))
        Assert.assertEquals("Superannuation Fund Details Annual Salary For Contributions Amount",service.runQuery("Superannuation Fund Details Annual Salary For Amount",getTestDefList()))
        Assert.assertEquals("Tax file number",service.runQuery("Tx fl Nmr",getTestDefList()))
        Assert.assertEquals("Higher Education Provider code",service.runQuery("Higher Education code",getTestDefList()))
    }

    fun getTestDefList():MutableList<Definition>
    {
        val output:MutableList<Definition> = mutableListOf()

        output.add(Definition("Electronic Contact Facsimile Area Code","Taxation and revenue collection","","","","",arrayOf<String>(),"",arrayOf<String>(),mapOf(),""))
        output.add(Definition("Address","Core Entity","","","","",arrayOf<String>(),"",arrayOf<String>(),mapOf(),""))
        output.add(Definition("Surname","Financial Insolvency","","","","",arrayOf<String>(),"",arrayOf<String>(),mapOf(),""))
        output.add(Definition("Superannuation Fund Details Annual Salary For Contributions Amount","Super Stream","","","","",arrayOf<String>(),"",arrayOf<String>(),mapOf(),""))
        output.add(Definition("Tax file number","Financial Statistics","","","","",arrayOf<String>(),"",arrayOf<String>(),mapOf(),""))
        output.add(Definition("Higher Education Provider code","Education","","","","",arrayOf<String>(),"",arrayOf<String>(),mapOf(),""))

        return output
    }

}
