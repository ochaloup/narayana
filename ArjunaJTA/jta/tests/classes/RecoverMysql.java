import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.sql.XADataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

import org.junit.Test;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.jta.xa.XidImple;

public class RecoverMysql {

        private String userName = "crashrec";
        private String password = "crashrec";
        private String recoveryUserName = "crashrec";
        private String recoveryPassword = "crashrec";
        private String hostName = "db01.mw.lab.eng.bos.redhat.com";
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
            /*
            oracle.jdbc.xa.client.OracleXADataSource dataSource = new oracle.jdbc.xa.client.OracleXADataSource();
            dataSource.setDriverType("thin");
            dataSource.setPortNumber(1521);
            dataSource.setNetworkProtocol("tcp");
            dataSource.setUser(userName);
            dataSource.setPassword(userName);
            dataSource.setServerName(hostName);
            dataSource.setDatabaseName(databaseName);
            */
            return dataSource;
        }

        @Test
        public void recoverMysql() throws SQLException, XAException {

                {
                        System.out.println("Rolling back for " + userName);
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

                        System.out.println("Cleaning testentity table after recover");
                        dataSource.getConnection().createStatement().execute("DELETE FROM testentity WHERE 1=1");
                }

                {
                        System.out.println("Rolling back for " + recoveryUserName);
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
                        dataSource.getConnection().createStatement().execute("DELETE FROM testentity WHERE 1=1");
                }

                {
                        System.out.println("Insertion for " + userName);
                        MysqlXADataSource dataSource = getDatasource(userName, password);
                        try {
                          XAConnection xaConnection = dataSource.getXAConnection();
                          XAResource xaResource2 = xaConnection.getXAResource();
                          XidImple xid = new XidImple(new Uid(), true, 1);
                          xaResource2.start(xid, XAResource.TMNOFLAGS);
                          System.out.println("Executing statement INSERT INTO testentity");
                          xaConnection.getConnection().createStatement().execute("INSERT INTO testentity (id, a) VALUES ('1', 1)");
                          System.out.println("Preparing: " + xid);
                          xaResource2.prepare(xid);
                          System.out.println("Prepared");
                       } catch (XAException xae) {
                          xae.printStackTrace();
                          throw xae; 
                       }
                }

                {
                        System.out.println("Insertion for " + recoveryUserName);
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

