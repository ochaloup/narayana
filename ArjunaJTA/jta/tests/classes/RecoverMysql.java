import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import javax.sql.XAConnection;
import java.sql.Connection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.sql.XADataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

import org.junit.Test;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.jta.xa.XidImple;

/**
 * For running the test you need to:
 * 1) build narayana
 * 1a) git clone -b 4.17-mysql-recovery-test https://github.com/ochaloup/narayana.git
 * 1b) cd narayana
 * 1c) ./build.sh clean install -Prelease,community,all -fae -Dmaven.test.skip.exec=true -Didlj-enabled=true
 * 2) cd ArjunaJTA/jta
 * 3) open pom.xml and redefine dependency of jdbc driver to point on correct jar location
 * 4) run the test: mvn clean verify -Dtest=RecoverMysql
 * 5) stdout is put under target/surefire-results/RecoverMysql-out.txt
 */
public class RecoverMysql {

        private String userName = "crashrec";
        private String password = "crashrec";
        private String recoveryUserName = "crashrec";
        private String recoveryPassword = "crashrec";
        private String hostName = "localhost";
        private int portNumber = 3306;
        private String databaseName = "crashrec";
        private String dbUrl = "jdbc:mysql://" + hostName + ":" + portNumber + "/" + databaseName;


        public MysqlXADataSource getDatasource(String user, String pass) {
            com.mysql.jdbc.jdbc2.optional.MysqlXADataSource dataSource = new com.mysql.jdbc.jdbc2.optional.MysqlXADataSource();
            dataSource.setDatabaseName(databaseName);
            dataSource.setPort(portNumber);
            dataSource.setUrl(dbUrl);
            dataSource.setUser(user);
            dataSource.setPassword(pass);
            dataSource.setServerName(hostName);

            return dataSource;
        }

        @Test
        public void recoverMysql() throws SQLException, XAException {

                {
                        System.out.println("Cleaning test environment");
                        System.out.println("Rolling back all Xids for " + userName);
                        MysqlXADataSource dataSource = getDatasource(userName, password);
                        XAResource xaResource = dataSource.getXAConnection().getXAResource();
                        Xid[] recover = xaResource.recover(XAResource.TMSTARTRSCAN);
                        for (int i = 0; i < recover.length; i++) {
                                try {
                                        System.out.println("Rolling back: " + new XidImple(recover[i]));
                                        xaResource.rollback(recover[i]);
                                        System.out.println("Rolled back");
                                } catch (XAException e) {
                                        e.printStackTrace();
                                }
                        }

                        System.out.println("Delete data from table testentity");
                        Connection con = dataSource.getConnection();
                        try {
                            con.createStatement().execute("DELETE FROM testentity WHERE 1=1");
                        } catch (com.mysql.jdbc.exceptions.MySQLSyntaxErrorException see) {
                            if(see.getMessage().toLowerCase().contains("doesn't exist")) {
                              System.out.println("As deletion of table testentity fails table probably does not exist - creating it for " + userName);
                              con.createStatement().execute("CREATE TABLE testentity (id VARCHAR(255), a INT)");
                            } else {
                              see.printStackTrace();
                              throw see;
                            }
                        } finally {
                            con.close();
                        }
                }
                
                {
                        System.out.println("Rolling back all Xids for " + recoveryUserName);
                        MysqlXADataSource dataSource = getDatasource(recoveryUserName, recoveryPassword);
                        XAResource xaResource = dataSource.getXAConnection().getXAResource();
                        Xid[] recover = xaResource.recover(XAResource.TMSTARTRSCAN);
                        for (int i = 0; i < recover.length; i++) {
                                try {
                                        System.out.println("Rolling back: " + new XidImple(recover[i]));
                                        xaResource.rollback(recover[i]);
                                        System.out.println("Rolled back");
                                } catch (XAException e) {
                                        e.printStackTrace();
                                }
                        }
                        xaResource.recover(XAResource.TMENDRSCAN);

                        System.out.println("Cleaning testentity table after recovery");
                        Connection con = dataSource.getConnection();
                        try {
                          con.createStatement().execute("DELETE FROM testentity WHERE 1=1");
                        } catch (SQLException e) {
                          e.printStackTrace();
                          throw e;
                        } finally {
                          con.close();
                        }
                }
                
                {
                        System.out.println("Insertion data - stage preparation with user " + userName);
                        MysqlXADataSource dataSource = getDatasource(userName, password);
                        Connection con = dataSource.getConnection();
                        
                        try {
                          XAConnection xaConnection = dataSource.getXAConnection();
                          XAResource xaResource2 = xaConnection.getXAResource();
                          XidImple xid = new XidImple(new Uid(), true, 1);
                          xaResource2.start(xid, XAResource.TMNOFLAGS);
                          System.out.println("Executing statement INSERT INTO testentity");
                          con.createStatement().execute("INSERT INTO testentity (id, a) VALUES ('1', 1)");
                          System.out.println("Preparing xid: " + xid);
                          xaResource2.end(xid, XAResource.TMSUCCESS);
                          xaResource2.prepare(xid);
                          System.out.println("Prepared xid: " + xid);
                       } catch (XAException xae) {
                          xae.printStackTrace();
                          throw xae; 
                       } finally {
                          con.close();
                       }
                }
                
                {
                        System.out.println("Insertion data - stage commiting with user " + recoveryUserName);
                        MysqlXADataSource dataSource = getDatasource(recoveryUserName, recoveryPassword);
                        
                        XAResource xaResource = dataSource.getXAConnection().getXAResource();
                        Xid[] recover = xaResource.recover(XAResource.TMSTARTRSCAN);
                        int completed = 0;
                        for (int i = 0; i < recover.length; i++) {
                                try {
                                        System.out.println("Commiting: " + new XidImple(recover[i]));
                                        xaResource.commit(recover[i], false);
                                        System.out.println("Committed");
                                        completed++;
                                } catch (XAException e) {
                                        e.printStackTrace();
                                }
                        }
                        xaResource.recover(XAResource.TMENDRSCAN);
                        int expected = 1;
                        if (expected >= 0) {
                                assertTrue("Completed: " + completed + " Expected: " + expected, completed == expected);
                        }
                }

        }
}

