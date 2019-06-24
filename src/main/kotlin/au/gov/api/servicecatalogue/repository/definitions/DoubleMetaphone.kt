package au.gov.api.servicecatalogue.repository.definitions

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
        fun getDoubleMetaphone(input:String) : String {
            var metaphoneData = MetaphoneData()



            return metaphoneData.toString()
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
                    _secondary.append(main);
                }
            }
        }

        override fun toString(): String {
            var ret = if (Alternative) _secondary.toString() else _primary.toString()

            //only give back 4 char metaph
            if (ret.length > 4) {
                ret = ret.substring(0, 4);
            }
            return ret;
        }
    }
}

