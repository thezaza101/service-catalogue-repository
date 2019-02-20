package au.gov.api.servicecatalogue.repository

import java.net.URL


class GitHub{


    companion object{

        @JvmStatic
        fun getRawURI(actualURL:String):String{
            //val actualURI = "https://github.com               /apigovau/api-gov-au-definitions/blob/master/api-documentation.md"
            //val rawURIi   = "https://raw.githubusercontent.com/apigovau/api-gov-au-definitions/master/api-documentation.md"
            
            return actualURL.replace("github.com","raw.githubusercontent.com").replace("/blob/master/","/master/")
        }


        @JvmStatic
        fun getTextOfFlie(uri:String):String{
            return URL(getRawURI(uri)).readText() 
        }
    }
}
