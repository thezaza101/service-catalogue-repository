package au.gov.api

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

import java.io.PrintWriter
import java.util.logging.Logger

import com.opentable.db.postgres.embedded.EmbeddedPostgres

class MockDataSource:DataSource{


    var pg = EmbeddedPostgres.start()


    override fun setLogWriter(pw: PrintWriter){}
    override fun getParentLogger():Logger { throw SQLException()}
    override fun setLoginTimeout(timeout:Int){}
    override fun getLoginTimeout() = 0
    override fun isWrapperFor(klass: Class<*>) = false
    override fun getLogWriter(): PrintWriter {throw SQLException()}
    override fun <T : Any?> unwrap(iface: Class<T>?): T { throw NotImplementedError() }
    override fun getConnection(): Connection{
        return pg.getPostgresDatabase().getConnection()
    }
    override fun getConnection(q:String, w:String) = getConnection()

	  /*fun testEmbeddedPg(){
		var pg = EmbeddedPostgres.start()
		var c = pg.getPostgresDatabase().getConnection()
		var s = c.createStatement()
		var rs = s.executeQuery("SELECT 1")
		Assert.assertTrue(rs.next())
		Assert.assertEquals(1, rs.getInt(1))
		Assert.assertFalse(rs.next())
	  }*/

}
