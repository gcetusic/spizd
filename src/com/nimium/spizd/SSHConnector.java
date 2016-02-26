package com.nimium.spizd;

import java.util.*;
import ch.ethz.ssh2.*;

/**
SSH Connector
*/
public class SSHConnector implements Connector {
  String host;
  String login;
  String password;
  ch.ethz.ssh2.Connection conn;
  com.nimium.spizd.Connection connection;
  /**
  Initialize the connector with given host and user
  @param host IP address or resolvable hostname
  @param user User login name to connecti with
  */
  public void init( com.nimium.spizd.Connection conn ) throws Exception {
    this.host = conn.host;
    this.login = conn.login;
    this.connection = conn;
  }
  /**
  Connect.
  @param password password to connect with
  */
  public void connect() throws Exception {
    //TODO
    this.password = connection.password;
    conn = new ch.ethz.ssh2.Connection( host );
    conn.connect( null, 30000, 60000 );
    if ( conn.authenticateWithPassword( login, password ) ) {
      System.out.println( "CONNECTED: "+password );
    }
    conn.close();
  }
  /**
  Nothing to do; login performed by connect() method
  @return true
  */
  public boolean login() {
    return true;
  }
  public void close() throws Exception {
    conn.close();
  }
  /**
  Required for authentication.
  @see Auth
  @return login name
  */
  public String getLogin() {
    return login;
  }
  /**
  Required for authentication.
  @see Auth
  @return password
  */
  public String getPassword() {
    return password;
  }
  /**
  Return port required to connect.
  */
  public int port() {
    return 22;
  }
  /**
  Called by after processing is finished to fetch the statistics.
  */
  public String getStatistics() {
    return null;
  }
  public static void main( String[] args ) throws Exception {
    if ( args.length < 2 ) {
      System.out.println( "Arguments: host user" );
      System.exit(1);
    }
    String host = args[0];
    String user = args[1];
    Runner r = new Runner();
    r.init( host, user, SSHConnector.class );
    // Note: default confing of sshd contains MaxStartups "10:30:60"
    // meaning 10 unauthenticated connections are allowed
    // 10-60 have 30% disconnect chance
    r.start();
  }
}
