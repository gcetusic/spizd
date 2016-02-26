package com.nimium.spizd;

import javax.mail.*;
import java.util.*;

public class IMAPConnector implements Connector {
  static Object propLock = new Object();
  static Properties props;
  Store store;
  String login;
  String password;
  Connection connection;
  static String proto; // imap, imaps
  static boolean tls = false; // imap-tls
  public void init( Connection c ) throws NoSuchProviderException {
    if ( props == null ) { // FIXME: this really should be static
      props = new Properties();
      props.put( "mail.store.protocol", proto );
      if ( tls ) {
        props.put( "mail."+proto+".starttls.enable", "true" );
        props.put( "mail."+proto+".ssl.trust", "*" ); // trust all hosts - starttls only, not for imaps
      }
      props.put( "mail.host", c.host );
      props.put( "mail.user", c.login );
      //props.put( "mail.debug", "true" );
    }
    this.login = c.login;
    this.connection = c;
    Session session = Session.getInstance( props, new Auth(this) );
    //Session session = Session.getDefaultInstance( props, new Auth(this) );
    //session.setDebug(true);
    store = session.getStore();
  }
  public String getLogin() { return login; }
  public String getPassword() { return password; }
  public int port() { return 143; }
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
    if ( "imap-tls".equals( proto ) ) {
      proto = "imap";
      tls = true;
    }
    String host = args[1];
    String user = null;
    if ( args.length > 2 ) user = args[2];
    Runner r = new Runner();
    r.init( host, user, IMAPConnector.class );
    r.start();
  }
}
