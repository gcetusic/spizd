package com.nimium.spizd;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

public class SMTPConnector implements Connector {
  static Properties props;
  String login;
  String password;
  Connection connection;
  Transport transport;
  static boolean tls = false;
  static String proto; //smtp, smtps
  Session session;
  static Runner runner;
  // parameters read from properties file:
  static int mailMessages = 0;
  static String mailSubject = "SpizdTest";
  static String mailBody = "testing test testis";
  static String mailFrom;
  static String mailTo;
  public void init( Connection c ) throws NoSuchProviderException {
    if ( props == null ) { // FIXME: this really should be static
      props = new Properties();
      props.put( "mail.transport.protocol", proto );
      if ( tls ) {
        props.put( "mail."+proto+".starttls.enable", "true" );
        props.put( "mail."+proto+".ssl.trust", "*" ); // trust all hosts - starttls only, for imaps does not work
      }
      props.put( "mail."+proto+".host", c.host );
      //if ( auth ) props.put( "mail.smtp.auth", "true" ); // CHECKME: i have no clue what's this for
    }
    this.login = c.login;
    this.connection = c;
    session = Session.getDefaultInstance( props, new Auth(this) );
    //session.setDebug(true);
    transport = session.getTransport();
  }
  public String getLogin() { return login; }
  public String getPassword() { return password; }
  public int port() { return 25; }
  public void connect() throws MessagingException {
    this.password = connection.password;
    transport.connect(); //javax.mail.AuthenticationFailedException
    for ( int i = 0; i < mailMessages; i++ ) {
      send();
    }
  }

  public void send() throws MessagingException {
    Message message = new MimeMessage(session);

    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(mailFrom));
    message.addFrom(new InternetAddress[] { new InternetAddress(mailTo) });

    message.setSubject(mailSubject);
    message.setContent(mailBody, "text/plain");

    transport.send(message);
  }

  /**
  Nothing to do; login performed by connect() method
  @return true
  */
  public boolean login() {
    return true;
  }
  public void close() throws Exception {
    transport.close();
  }
  public String getStatistics() {
    return null;
  }
  /**
  Load smtp-specific properties contained in Runner object; Runner must have
  been initialized, or Runner.loadProperties() called prior to this call.
  */
  public static void loadProperties( Runner r ) {
    runner = r;

    String tmp = r.props.getProperty("mail.messages");
    if ( tmp != null ) {
      try {
        mailMessages = Integer.parseInt(tmp);
      } catch ( NumberFormatException nfe ) {
        System.out.println("ERROR: Cannot read mail.messages property: "+tmp+" - "+nfe);
      }
    }

    tmp = r.props.getProperty("mail.subject");
    if ( tmp != null ) mailSubject = tmp;

    tmp = r.props.getProperty("mail.body");
    if ( tmp != null ) mailBody = tmp;

    tmp = r.props.getProperty("mail.from");
    if ( tmp != null ) mailFrom = tmp;

    tmp = r.props.getProperty("mail.to");
    if ( tmp != null ) mailTo = tmp;

  }
  public static void main( String[] args ) throws Exception {
    if ( args.length < 2 ) {
      System.out.println( "Arguments: host [user]" );
      System.out.println( "           host must be provided in command line" );
      System.out.println( "           if user name is not provided, it's read from dictionary file" );
      System.exit(1);
    }
    proto = args[0];
    if ( "smtp-tls".equals( proto ) ) {
      proto = "smtp";
      tls = true;
    }
    String host = args[1];
    String user = null;
    if ( args.length > 2 ) user = args[2];
    Runner r = new Runner();
    r.init( host, user, SMTPConnector.class );
    loadProperties(r);
    r.start();
  }
}
