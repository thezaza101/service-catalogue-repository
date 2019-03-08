package au.gov.api


import au.gov.api.servicecatalogue.Diff.*
import org.junit.Assert
import org.junit.Test

class DiffTests {

    val diffengine = TextDiff(MyersDiff(), MarkdownDiffOutputGenerator())
    val originalText = "The quick brown fox"

    @Test
    fun test_list_Gen() {
        val list = diffengine.generateDiffList("The quick brown fox jumped","The slow brown fox jumped over")
        Assert.assertEquals(5,list.count())
    }

    //#1 Equal Test
    @Test
    fun equal_text_pattern()
    {
        val stringToTest = "The quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "E"))
    }

    //#2 Add at start
    @Test
    fun add_at_start_pattern()
    {
        val stringToTest = "Once The quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "AE"))
    }

    //#3 Remove at start
    @Test
    fun remove_at_start_pattern()
    {
        val stringToTest = "quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "RE"))
    }

    //#4 Add at middle
    @Test
    fun add_at_middle_pattern()
    {
        val stringToTest = "The quick agile brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true, OutputPatternMatch(listOutput, "EAE"))
    }

    //#5 Remove at middle
    @Test
    fun remove_at_middle_pattern()
    {
        val stringToTest = "The quick fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true, OutputPatternMatch(listOutput, "ERE"))
    }

    //#6 Add at end
    @Test
    fun add_at_end_pattern()
    {
        val stringToTest = "The quick brown fox jumped"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true, OutputPatternMatch(listOutput, "EA"))
    }

    //#7 Remove at end
    @Test
    fun remove_at_end_pattern()
    {
        val stringToTest = "The quick brown"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true, OutputPatternMatch(listOutput, "ER"))
    }

    //#8 Update at start
    @Test
    fun update_at_start_pattern()
    {
        val stringToTest = "A quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "RAE"))
    }

    //#9 Update at middle
    @Test
    fun update_at_middle_pattern()
    {
        val stringToTest = "The quick blue fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "ERAE"))
    }

    //#10 Upadte at end
    @Test
    fun update_at_end_pattern()
    {
        val stringToTest = "The quick brown cat"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "ERA"))
    }

    //#11 Multiple add
    @Test
    fun multiple_add_pattern()
    {
        val stringToTest = "The quick agile brown fox jumped"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "EAEA"))
    }

    //#12 Multiple remove
    @Test
    fun multiple_remove_pattern()
    {
        val stringToTest = "quick fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "RERE"))
    }

    //#13 Multiple updates
    @Test
    fun multiple_update_pattern()
    {
        val stringToTest = "The slow brown cat"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputPatternMatch(listOutput, "ERAERA"))
    }




    fun OutputPatternMatch(list:List<Diffrence> , pattern:String) : Boolean
    {
        var result = true
        if (list.count() != pattern.length)
        {
            return false
        }
        for(index in 0..list.count()-1) {
            if(list[index].action != GetDiffAction(pattern[index]))
            {
                result = false
            }
        }
        return result
    }

    fun GetDiffAction (action:Char) : DiffAction
    {
        when (action)
        {
            'A' -> return DiffAction.Add
            'R' -> return DiffAction.Remove
            'E' -> return DiffAction.Equal
            else -> throw Exception("invalid action")
        }
    }

    //#1 Equal Test
    @Test
    fun equal_text_text()
    {
        val stringToTest = "The quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick brown fox")))
    }

    //#2 Add at start
    @Test
    fun add_at_start_text()
    {
        val stringToTest = "Once The quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("Once ","The quick brown fox")))
    }

    //#3 Remove at start
    @Test
    fun remove_at_start_text()
    {
        val stringToTest = "quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The ", "quick brown fox")))
    }

    //#4 Add at middle
    @Test
    fun add_at_middle_text()
    {
        val stringToTest = "The quick agile brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick ", "agile ", "brown fox")))
    }

    //#5 Remove at middle
    @Test
    fun remove_at_middle_text()
    {
        val stringToTest = "The quick fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick ", "brown ", "fox")))
    }

    //#6 Add at end
    @Test
    fun add_at_end_text()
    {
        val stringToTest = "The quick brown fox jumped"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick brown fox", " jumped")))
    }

    //#7 Remove at end
    @Test
    fun remove_at_end_text()
    {
        val stringToTest = "The quick brown"
        val  listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick brown", " fox")))
    }

    //#8 Update at start
    @Test
    fun update_at_start_text()
    {
        val stringToTest = "A quick brown fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The","A", " quick brown fox")))
    }

    //#9 Update at middle
    @Test
    fun update_at_middle_text()
    {
        val stringToTest = "The quick blue fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick b", "rown","lue", " fox")))
    }

    //#10 Upadte at end
    @Test
    fun update_at_end_text()
    {
        val stringToTest = "The quick brown cat"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick brown ", "fox", "cat")))
    }

    //#11 Multiple add
    @Test
    fun multiple_add_text()
    {
        val stringToTest = "The quick agile brown fox jumped"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The quick ", "agile ", "brown fox", " jumped")))
    }

    //#12 Multiple remove
    @Test
    fun multiple_remove_text()
    {
        val stringToTest = "quick fox"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The ", "quick", " brown", " fox")))
    }

    //#13 Multiple updates
    @Test
    fun multiple_update_text()
    {
        val stringToTest = "The slow brown cat"
        val listOutput = diffengine.generateDiffList(originalText,stringToTest)
        Assert.assertEquals(true,OutputTextMatch(listOutput, listOf("The ", "quick", "slow", " brown ", "fox", "cat")))
    }

    fun OutputTextMatch (list:List<Diffrence>, expectedList:List<String>) : Boolean
    {
        var result = true
        if (list.count() != expectedList.count())
        {
            return false
        }
        for(index in 0..list.count()-1) {
            if(list[index].value != expectedList[index])
            {
                result = false
                break
            }
        }
        return result
    }

}
