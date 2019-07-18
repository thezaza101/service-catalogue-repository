
import org.springframework.security.crypto.keygen.KeyGenerators.string

/*

This is Kotlin port of Anthony Tong Lee's 2008 implementation of the double metaphone algorithm

See http://www.ddj.com/cpp/184401251?pgno=1 for a discussion and original C++ implementation.

Copyright (c) 2008 Anthony Tong Lee

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
*/

class DoubleMetaphone {
    companion object {

        @JvmStatic
        fun getDoubleMetaphone(input: String): MetaphoneData {
            //if (input.length < 1) return null
            var metaphoneData = MetaphoneData()

            var current: Int = 0
            var last: Int = input.length - 1
            var workingString = input.toUpperCase() + "     "
            var isSlavoGermanic = (input.indexOf('W') > -1) || (input.indexOf('K') > -1) || (input.indexOf("CZ") > -1)
                    || (input.indexOf("WITZ") > -1)

            //skip these when at start of word
            if (workingString.startsWith(listOf("GN", "KN", "PN", "WR", "PS"), true)) {
                current += 1
            }

            //Initial 'X' is pronounced 'Z' e.g. 'Xavier'
            if (workingString[0] == 'X') {
                metaphoneData.Add("S") //'Z' maps to 'S'
                current += 1
            }

            loop@ while ((metaphoneData.PrimaryLength < 4) || (metaphoneData.SecondaryLength < 4)) {
                if (current >= input.length) {
                    break
                }
                when (workingString[current]) {
                    'A', 'E', 'I', 'O', 'U', 'Y' -> {
                        if (current == 0) {
                            //all init vowels now map to 'A'
                            metaphoneData.Add("A")
                        }
                        current += 1
                        continue@loop
                    }
                    'B' -> {
                        //"-mb", e.g", "dumb", already skipped over...
                        metaphoneData.Add("P")

                        if (workingString[current + 1] == 'B') {
                            current += 2
                        } else {
                            current += 1
                        }
                        continue@loop
                    }
                    '�' -> {
                        metaphoneData.Add("S")
                        current += 1
                        continue@loop
                    }
                    'C' -> {
                        //various germanic
                        if ((current > 1) && !workingString[current - 2].isVowel() && workingString.stringAt(
                                        (current - 1),
                                        listOf("ACH")
                                ) &&
                                ((workingString[current + 2] != 'I') && ((workingString[current + 2] != 'E') || workingString.stringAt(
                                        current - 2,
                                        listOf("BACHER", "MACHER")
                                )))
                        ) {
                            metaphoneData.Add("K")
                            current += 2
                            continue@loop
                        }

                        if ((current == 0) && workingString.stringAt(current, listOf("CAESAR"))) {
                            //special case 'caesar'
                            metaphoneData.Add("S")
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("CHIA"))) {
                            //italian 'chianti'
                            metaphoneData.Add("K")
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("CH"))) {
                            ////find 'michael'
                            if ((current > 0) && workingString.stringAt(current, listOf("CHAE"))) {
                                metaphoneData.Add("K", "X")
                                current += 2
                                continue@loop
                            }
                            if ((current == 0) && (workingString.stringAt((current + 1), listOf("HARAC", "HARIS")) ||
                                            workingString.stringAt((current + 1), listOf("HOR", "HYM", "HIA", "HEM"))) &&
                                    !workingString.stringAt(0, listOf("CHORE"))
                            ) {
                                //greek roots e.g. 'chemistry', 'chorus'
                                metaphoneData.Add("K")
                                current += 2
                                continue@loop
                            }
                            if ((workingString.stringAt(0, listOf("VAN ", "VON ")) || workingString.stringAt(
                                            0,
                                            listOf("SCH")
                                    )) // 'architect but not 'arch', 'orchestra', 'orchid'
                                    || workingString.stringAt(
                                            (current - 2),
                                            listOf("ORCHES", "ARCHIT", "ORCHID")
                                    ) || workingString.stringAt((current + 2), listOf("T", "S"))
                                    || ((workingString.stringAt(
                                            (current - 1),
                                            listOf("A", "O", "U", "E")
                                    ) || (current == 0)) //e.g., 'wachtler', 'wechsler', but not 'tichner'
                                            && workingString.stringAt(
                                            (current + 2),
                                            listOf("L", "R", "N", "M", "B", "H", "F", "V", "W", " ")
                                    ))
                            ) {
                                //germanic, greek, or otherwise 'ch' for 'kh' sound
                                metaphoneData.Add("K")

                            } else {
                                if (current > 0) {
                                    if (workingString.stringAt(0, listOf("MC"))) {
                                        //e.g., "McHugh"
                                        metaphoneData.Add("K")
                                    } else {
                                        metaphoneData.Add("X", "K")
                                    }
                                } else {
                                    metaphoneData.Add("X")
                                }
                                current += 2
                                continue@loop
                            }
                        }

                        if (workingString.stringAt(current, listOf("CZ")) && !workingString.stringAt(
                                        (current - 2),
                                        listOf("WICZ")
                                )
                        ) {
                            //e.g, 'czerny'
                            metaphoneData.Add("S", "X")
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt((current + 1), listOf("CIA"))) {
                            //e.g., 'focaccia'
                            metaphoneData.Add("X")
                            current += 3
                            continue@loop
                        }

                        if (workingString.stringAt(
                                        current,
                                        listOf("CC")
                                ) && !((current == 1) && (workingString[0] == 'M'))
                        ) {
                            //double 'C', but not if e.g. 'McClellan'
                            if (workingString.stringAt((current + 2), listOf("I", "E", "H")) && !workingString.stringAt(
                                            (current + 2),
                                            listOf("HU")
                                    )
                            ) {
                                //'bellocchio' but not 'bacchus'
                                if (((current == 1) && (workingString[current - 1] == 'A')) || workingString.stringAt(
                                                (current - 1),
                                                listOf("UCCEE", "UCCES")
                                        )
                                ) {
                                    //'accident', 'accede' 'succeed'
                                    metaphoneData.Add("KS")
                                } else {
                                    //'bacci', 'bertucci', other italian
                                    metaphoneData.Add("X")
                                }
                                current += 3
                                continue@loop
                            } else {
                                //Pierce's rule
                                metaphoneData.Add("K")
                                current += 2
                                continue@loop
                            }
                        }
                        if (workingString.stringAt(current, listOf("CK", "CG", "CQ"))) {
                            metaphoneData.Add("K")
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("CI", "CE", "CY"))) {
                            if (workingString.stringAt(current, listOf("CIO", "CIE", "CIA"))) {
                                //italian vs. english
                                metaphoneData.Add("S", "X")
                            } else {
                                metaphoneData.Add("S")
                            }
                            current += 2
                            continue@loop
                        }
                        //else
                        metaphoneData.Add("K")

                        //name sent in 'mac caffrey', 'mac gregor
                        if (workingString.stringAt((current + 1), listOf(" C", " Q", " G"))) {
                            current += 3
                        } else if (workingString.stringAt(
                                        (current + 1),
                                        listOf("C", "K", "Q")
                                ) && !workingString.stringAt((current + 1), listOf("CE", "CI"))
                        ) {
                            current += 2
                        } else {
                            current += 1
                        }
                        continue@loop
                    }
                    'D' -> {
                        if (workingString.stringAt(current, listOf("DG"))) {
                            if (workingString.stringAt((current + 2), listOf("I", "E", "Y"))) {
                                //e.g. 'edge'
                                metaphoneData.Add("J")
                                current += 3
                                continue@loop
                            } else {
                                //e.g. 'edgar'
                                metaphoneData.Add("TK")
                                current += 2
                                continue@loop
                            }
                        }

                        if (workingString.stringAt(current, listOf("DT", "DD"))) {
                            metaphoneData.Add("T")
                            current += 2
                            continue@loop
                        }

                        //else
                        metaphoneData.Add("T")
                        current += 1
                        continue@loop

                    }
                    'F' -> {
                        if (workingString[current + 1] == 'F') {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("F")
                        continue@loop
                    }
                    'G' -> {
                        if (workingString[current + 1] == 'H') {
                            if ((current > 0) && !workingString[current - 1].isVowel()) {
                                metaphoneData.Add("K")
                                current += 2
                                continue@loop
                            }

                            if (current < 3) {
                                //'ghislane', ghiradelli
                                if (current == 0) {
                                    if (workingString[current + 2] == 'I') {
                                        metaphoneData.Add("J")
                                    } else {
                                        metaphoneData.Add("K")
                                    }
                                    current += 2
                                    continue@loop
                                }
                            }

                            if (((current > 1) && workingString.stringAt(
                                            (current - 2),
                                            listOf("B", "H", "D")
                                    )) //e.g., 'bough'
                                    || ((current > 2) && workingString.stringAt(
                                            (current - 3),
                                            listOf("B", "H", "D")
                                    )) //e.g., 'broughton'
                                    || ((current > 3) && workingString.stringAt((current - 4), listOf("B", "H")))
                            ) {
                                //Parker's rule (with some further refinements) - e.g., 'hugh'
                                current += 2
                                continue@loop
                            } else {
                                //e.g., 'laugh', 'McLaughlin', 'cough', 'gough', 'rough', 'tough'
                                if ((current > 2) && (workingString[current - 1] == 'U') && workingString.stringAt(
                                                (current - 3),
                                                listOf("C", "G", "L", "R", "T")
                                        )
                                ) {
                                    metaphoneData.Add("F")
                                } else if ((current > 0) && workingString[current - 1] != 'I') {
                                    metaphoneData.Add("K")
                                }
                                current += 2
                                continue@loop
                            }
                        }

                        if (workingString[current + 1] == 'N') {
                            if ((current == 1) && workingString[0].isVowel() && !isSlavoGermanic) {
                                metaphoneData.Add("KN", "N")
                            } else {
                                //not e.g. 'cagney'
                                if (!workingString.stringAt(
                                                (current + 2),
                                                listOf("EY")
                                        ) && (workingString[current + 1] != 'Y') && !isSlavoGermanic
                                ) {
                                    metaphoneData.Add("N", "KN")
                                } else {
                                    metaphoneData.Add("KN")
                                }
                            }
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt((current + 1), listOf("LI")) && !isSlavoGermanic) {
                            //'tagliaro'
                            metaphoneData.Add("KL", "L")
                            current += 2
                            continue@loop
                        }

                        if ((current == 0) &&
                                ((workingString[current + 1] == 'Y') ||
                                        workingString.stringAt(
                                                (current + 1),
                                                listOf("ES", "EP", "EB", "EL", "EY", "IB", "IL", "IN", "IE", "EI", "ER")
                                        ))
                        ) {
                            //-ges-,-gep-,-gel-, -gie- at beginning
                            metaphoneData.Add("K", "J")
                            current += 2
                            continue@loop
                        }

                        if ((workingString.stringAt(
                                        (current + 1),
                                        listOf("ER")
                                ) || (workingString[current + 1] == 'Y')) && !workingString.stringAt(
                                        0,
                                        listOf("DANGER", "RANGER", "MANGER")
                                )
                                && !workingString.stringAt(
                                        (current - 1),
                                        listOf("E", "I")
                                ) && !workingString.stringAt((current - 1), listOf("RGY", "OGY"))
                        ) {
                            // -ger-,  -gy-
                            metaphoneData.Add("K", "J")
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt(
                                        (current + 1),
                                        listOf("E", "I", "Y")
                                ) || workingString.stringAt((current - 1), listOf("AGGI", "OGGI"))
                        ) {
                            // italian e.g, 'biaggi'

                            if ((workingString.stringAt(0, listOf("VAN ", "VON ")) || workingString.stringAt(
                                            0,
                                            listOf("SCH")
                                    )) || workingString.stringAt((current + 1), listOf("ET"))
                            ) {
                                //obvious germanic
                                metaphoneData.Add("K")
                            } else
                            //always soft if french ending
                                if (workingString.stringAt((current + 1), listOf("IER "))) {
                                    metaphoneData.Add("J")
                                } else {
                                    metaphoneData.Add("J", "K")
                                }
                            current += 2
                            continue@loop

                        }


                        if (workingString[current + 1] == 'G') {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("K")
                        continue@loop

                    }
                    'H' -> {
                        //only keep if first & before vowel or btw. 2 vowels
                        if (((current == 0) || workingString[current - 1].isVowel()) && workingString[current + 1].isVowel()) {
                            metaphoneData.Add("H")
                            current += 2
                        } else //also takes care of 'HH'
                        {
                            current += 1
                        }
                        continue@loop
                    }
                    'J' -> {
                        //obvious spanish, 'jose', 'san jacinto'
                        if (workingString.stringAt(current, listOf("JOSE")) || workingString.stringAt(
                                        0,
                                        listOf("SAN ")
                                )
                        ) {
                            if (((current == 0) && (workingString[current + 4] == ' ')) || workingString.stringAt(
                                            0,
                                            listOf("SAN ")
                                    )
                            ) {
                                metaphoneData.Add("H")
                            } else {
                                metaphoneData.Add("J", "H")
                            }
                            current += 1
                            continue@loop
                        }

                        if ((current == 0) && !workingString.stringAt(current, listOf("JOSE"))) {
                            metaphoneData.Add("J", "A") //Yankelovich/Jankelowicz
                        } else {
                            //spanish pron. of e.g. 'bajador'
                            if (workingString[current - 1].isVowel() && !isSlavoGermanic && ((workingString[current + 1] == 'A') || (workingString[current + 1] == 'O'))) {
                                metaphoneData.Add("J", "H")
                            } else if (current == last) {
                                metaphoneData.Add("J", " ")
                            } else if (!workingString.stringAt(
                                            (current + 1),
                                            listOf("L", "T", "K", "S", "N", "M", "B", "Z")
                                    ) && !workingString.stringAt((current - 1), listOf("S", "K", "L"))
                            ) {
                                metaphoneData.Add("J")
                            }
                        }

                        if (workingString[current + 1] == 'J') //it could happen!
                        {
                            current += 2
                        } else {
                            current += 1
                        }
                        continue@loop

                    }
                    'K' -> {
                        if (workingString[current + 1] == 'K') {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("K")
                        continue@loop
                    }
                    'L' -> {
                        if (workingString[current + 1] == 'L') {
                            //spanish e.g. 'cabrillo', 'gallegos'
                            if (((current == (input.length - 3)) && workingString.stringAt(
                                            (current - 1),
                                            listOf("ILLO", "ILLA", "ALLE")
                                    ))
                                    || ((workingString.stringAt((last - 1), listOf("AS", "OS")) || workingString.stringAt(
                                            last,
                                            listOf("A", "O")
                                    )) && workingString.stringAt((current - 1), listOf("ALLE")))
                            ) {
                                metaphoneData.Add("L", " ")
                                current += 2
                                continue@loop
                            }
                            current += 2
                        } else {
                            current += 1
                        }

                        metaphoneData.Add("L")
                        continue@loop
                    }
                    'M' -> {
                        if ((workingString.stringAt(
                                        (current - 1),
                                        listOf("UMB")
                                ) && (((current + 1) == last) || workingString.stringAt(
                                        (current + 2),
                                        listOf("ER")
                                ))) //'dumb','thumb'
                                || (workingString[current + 1] == 'M')
                        ) {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("M")
                        continue@loop
                    }
                    'N' -> {
                        if (workingString[current + 1] == 'N') {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("N")
                        continue@loop
                    }
                    'P' -> {
                        var exit = false
                        if (workingString[current + 1] == 'H') {
                            metaphoneData.Add("F")
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt((current + 1), listOf("P", "B"))) {
                            //also account for "campbell", "raspberry"
                            current += 2
                        } else {
                            current += 1
                        }

                        metaphoneData.Add("P")
                        continue@loop

                    }
                    'Q' -> {
                        if (workingString[current + 1] == 'Q') {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("K")
                        continue@loop
                    }
                    'R' -> {
                        //french e.g. 'rogier', but exclude 'hochmeier'
                        if ((current == last) && !isSlavoGermanic && workingString.stringAt(
                                        (current - 2),
                                        listOf("IE")
                                ) && !workingString.stringAt((current - 4), listOf("ME", "MA"))
                        ) {
                            metaphoneData.Add("", "R")
                        } else {
                            metaphoneData.Add("R")
                        }

                        if (workingString[current + 1] == 'R') {
                            current += 2
                        } else {
                            current += 1
                        }
                        continue@loop
                    }
                    'S' -> {
                        if (workingString.stringAt((current - 1), listOf("ISL", "YSL"))) {
                            //special cases 'island', 'isle', 'carlisle', 'carlysle'
                            current += 1
                            continue@loop
                        }
                        if ((current == 0) && workingString.stringAt(current, listOf("SUGAR"))) {
                            //special case 'sugar-'
                            metaphoneData.Add("X", "S")
                            current += 1
                            continue@loop
                        }
                        if (workingString.stringAt(current, listOf("SH"))) {
                            //germanic
                            if (workingString.stringAt((current + 1), listOf("HEIM", "HOEK", "HOLM", "HOLZ"))) {
                                metaphoneData.Add("S")
                            } else {
                                metaphoneData.Add("X")
                            }
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("SIO", "SIA")) || workingString.stringAt(
                                        current,
                                        listOf("SIAN")
                                )
                        ) {
                            //italian & armenian
                            if (!isSlavoGermanic) {
                                metaphoneData.Add("S", "X")
                            } else {
                                metaphoneData.Add("S")
                            }
                            current += 3
                            continue@loop
                        }

                        if (((current == 0) && workingString.stringAt(
                                        (current + 1),
                                        listOf("M", "N", "L", "W")
                                )) || workingString.stringAt((current + 1), listOf("Z"))
                        ) {
                            //german & anglicisations, e.g. 'smith' match 'schmidt', 'snider' match 'schneider'
                            //also, -sz- in slavic language altho in hungarian it is pronounced 's'
                            metaphoneData.Add("S", "X")
                            if (workingString.stringAt((current + 1), listOf("Z"))) {
                                current += 2
                            } else {
                                current += 1
                            }
                            continue@loop
                        }
                        if (workingString.stringAt(current, listOf("SC"))) {
                            //Schlesinger's rule
                            if (workingString[current + 2] == 'H') {
                                //dutch origin, e.g. 'school', 'schooner'
                                if (workingString.stringAt((current + 3), listOf("OO", "ER", "EN", "UY", "ED", "EM"))) {
                                    //'schermerhorn', 'schenker'
                                    if (workingString.stringAt((current + 3), listOf("ER", "EN"))) {
                                        metaphoneData.Add("X", "SK")
                                    } else {
                                        metaphoneData.Add("SK")
                                    }
                                    current += 3
                                    continue@loop
                                } else {
                                    if ((current == 0) && !workingString[3].isVowel() && (workingString[3] != 'W')) {
                                        metaphoneData.Add("X", "S")
                                    } else {
                                        metaphoneData.Add("X")
                                    }
                                    current += 3
                                    continue@loop
                                }
                            }


                            if (workingString.stringAt((current + 2), listOf("I", "E", "Y"))) {
                                metaphoneData.Add("S")
                                current += 3
                                continue@loop
                            }


                            //else
                            metaphoneData.Add("SK")
                            current += 3
                            continue@loop
                        }

                        if ((current == last) && workingString.stringAt((current - 2), listOf("AI", "OI"))) {
                            //french e.g. 'resnais', 'artois'
                            metaphoneData.Add("", "S")
                        } else {
                            metaphoneData.Add("S")
                        }

                        if (workingString.stringAt((current + 1), listOf("S", "Z"))) {
                            current += 2
                        } else {
                            current += 1
                        }
                        continue@loop
                    }
                    'T' -> {
                        if (workingString.stringAt(current, listOf("TION"))) {
                            metaphoneData.Add("X")
                            current += 3
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("TIA", "TCH"))) {
                            metaphoneData.Add("X")
                            current += 3
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("TH")) || workingString.stringAt(
                                        current,
                                        listOf("TTH")
                                )
                        ) {
                            //special case 'thomas', 'thames' or germanic
                            if (workingString.stringAt((current + 2), listOf("OM", "AM")) || workingString.stringAt(
                                            0,
                                            listOf("VAN ", "VON ")
                                    ) || workingString.stringAt(0, listOf("SCH"))
                            ) {
                                metaphoneData.Add("T")
                            } else {
                                metaphoneData.Add("O", "T")
                            }
                            current += 2
                            continue@loop
                        }

                        if (workingString.stringAt((current + 1), listOf("T", "D"))) {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("T")
                        continue@loop
                    }
                    'V' -> {
                        if (workingString[current + 1] == 'V') {
                            current += 2
                        } else {
                            current += 1
                        }
                        metaphoneData.Add("F")
                        continue@loop
                    }
                    'W' -> {

                        //can also be in middle of word
                        if (workingString.stringAt(current, listOf("WR"))) {
                            metaphoneData.Add("R")
                            current += 2
                            continue@loop
                        }

                        if ((current == 0) && (workingString[current + 1].isVowel() || workingString.stringAt(
                                        current,
                                        listOf("WH")
                                ))
                        ) {
                            //Wasserman should match Vasserman
                            if (workingString[current + 1].isVowel()) {
                                metaphoneData.Add("A", "F")
                            } else {
                                //need Uomo to match Womo
                                metaphoneData.Add("A")
                            }
                        }


                        if (((current == last) && workingString[current - 1].isVowel()) || workingString.stringAt(
                                        (current - 1),
                                        listOf("EWSKI", "EWSKY", "OWSKI", "OWSKY")
                                )
                                || workingString.stringAt(0, listOf("SCH"))
                        ) {
                            //Arnow should match Arnoff
                            metaphoneData.Add("", "F")
                            current += 1
                            continue@loop
                        }

                        if (workingString.stringAt(current, listOf("WICZ", "WITZ"))) {
                            //polish e.g. 'filipowicz'
                            metaphoneData.Add("TS", "FX")
                            current += 4
                            continue@loop
                        }
                        //else skip it
                        current += 1


                    }
                    'X' -> {
                        //french e.g. breaux
                        if (!((current == last) && (workingString.stringAt(
                                        (current - 3),
                                        listOf("IAU", "EAU")
                                ) || workingString.stringAt((current - 2), listOf("AU", "OU"))))
                        ) {
                            metaphoneData.Add("KS")
                        }

                        if (workingString.stringAt((current + 1), listOf("C", "X"))) {
                            current += 2
                        } else {
                            current += 1
                        }
                        continue@loop
                    }
                    'Z' -> {
                        //chinese pinyin e.g. 'zhao'
                        if (workingString[current + 1] == 'H') {
                            metaphoneData.Add("J")
                            current += 2
                            continue@loop
                        } else if (workingString.stringAt(
                                        (current + 1),
                                        listOf("ZO", "ZI", "ZA")
                                ) || (isSlavoGermanic && ((current > 0) && workingString[current - 1] != 'T'))
                        ) {
                            metaphoneData.Add("S", "TS")
                        } else {
                            metaphoneData.Add("S")
                        }

                        if (workingString[current + 1] == 'Z') {
                            current += 2
                        } else {
                            current += 1
                        }

                    }
                    else -> {
                        current += 1
                        continue@loop
                    }
                }
            }

            return metaphoneData
        }

        fun Char.isVowel(): Boolean {
            return (this == 'A') || (this == 'E') || (this == 'I') || (this == 'O') || (this == 'U') || (this == 'Y')
        }

        fun String.startsWith(strings: List<String>, ignoreCase: Boolean = true): Boolean {
            strings.forEach { if (it.startsWith(this)) return true }
            return false
        }

        fun String.stringAt(startIndex: Int, strings: List<String>, ignoreCase: Boolean = true): Boolean {
            var idx = startIndex
            if (idx < 0) {
                idx = 0
            }
            strings.forEach { if (it.indexOf(it, idx, ignoreCase) >= startIndex) return true }
            return false

        }
    }

    class MetaphoneData {
        val _primary = StringBuilder(5)
        val _secondary = StringBuilder(5)

        var Alternative: Boolean = false
        var PrimaryLength: Int = _primary.length
        var SecondaryLength: Int = _secondary.length

        fun Add(main: String?) {
            if (main != null) {
                _primary.append(main)
                _secondary.append(main)
            }
        }

        fun Add(main: String?, alternative: String?) {
            if (main != null) {
                _primary.append(main)
            }

            if (alternative != null) {
                Alternative = true
                if (alternative.trim().length > 0) {
                    _secondary.append(alternative)
                }
            } else {
                if (main != null && main.trim().length > 0) {
                    _secondary.append(main)
                }
            }
        }

        override fun toString(): String {
            val primary = _primary.toString()
            val secondary = _secondary.toString()

            return if (primary !== secondary) {
                "[\"" + _primary.toString() + "\",\"" + _secondary.toString() + "\"]"
            } else {
                "[\"" + _primary.toString() + "\"]"
            }
        }

        fun toList():List<String> {
            val primary = _primary.toString()
            val secondary = _secondary.toString()

            return if (primary != secondary) {
                listOf(_primary.toString(),_secondary.toString())
            } else {
                listOf(_primary.toString())
            }
        }
    }
}

