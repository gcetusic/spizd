package com.nimium.spizd;

import javax.mail.*;
import java.util.*;

public class POPConnector implements Connector {
  static Properties props;
  String login;
  Connection connection;
  Store store;
  String password;
  static String proto; // pop3, pop3s
  static boolean tls = false; // pop3-tls
  public void init( Connection conn ) throws NoSuchProviderException {
    if ( props == null ) {
      props = new Properties();
      props.put( "mail.store.protocol", proto );
      props.put( "mail.transport.protocol", proto );
      if ( tls ) props.put( "mail.pop3.starttls.enable", "true" );
      props.put( "mail.host", conn.host );
      props.put( "mail.user", conn.login );
    }
    this.login=conn.login;
    this.connection = conn;
    Session session = Session.getDefaultInstance( props, new Auth(this) );
    store = session.getStore();
  }
  public String getLogin() { return login; }
  public String getPassword() { return password; }
  public int port() { return 110; }
  public void connect() throws MessagingException {
    this.password = connection.password;
    store.connect(); //javax.mail.AuthenticationFailedException
  }
  /**
  Nothing to do; login performed by connect() method
  @return true
  */
  public boolean login() {
    return true;
  }
  public void close() throws Exception {
    store.close();
  }
  public String getStatistics() {
    return null;
  }
  public static void main( String[] args ) throws Exception {
    if ( args.length < 2 ) {
      System.out.println( "Arguments: host [user]" );
      System.out.println( "           host must be provided in command line" );
      System.out.println( "           if user name is not provided, it's read from dictionary file" );
      System.exit(1);
    }
    proto = args[0];
    if ( "pop3-tls".equals( proto ) ) {
      proto = "pop3";
      tls = true;
    }
    String host = args[1];
    String user = null;
    if ( args.length > 2 ) user = args[2];
    Runner r = new Runner();
    r.init( host, user, POPConnector.class );
    r.start();
  }
}
