package com.nimium.spizd;

import java.util.*;

import org.tinyradius.util.*;
import org.tinyradius.packet.*;

/*
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
*/

public class RADIUSConnector implements Connector {
  String login;
  String password;

  String host;
  static String secret;

  Connection connection;
  static Runner runner;
  boolean loggedin = false;

  static boolean accounting = false;
  static boolean addSession = true;
  static int interim = 0;
  static long requestDelay = 0;
  static int soTimeout = 3000; // 3 sec default

  static HashMap acctAttrs = new HashMap();
  static HashMap authAttrs = new HashMap();

  // statistics:
  static long authSent = 0;
  static long authAccept = 0;
  static long authReject = 0;
  static long acctStartSent = 0;
  static long acctStartRecv = 0;
  static long acctStopSent = 0;
  static long acctStopRecv = 0;
  static long acctInterimSent = 0;
  static long acctInterimRecv = 0;
  static long authTime;
  static long acctTime;
  static long maxAcctTime=0;
  static long maxAuthTime=0;

  RadiusClient client;

  // disabling log4j log:
  /*
  static {
    BasicConfigurator.configure();
    Logger logger = Logger.getLogger("org.tinyradius");
    logger.setLevel( Level.OFF );
  }
  */

  public void init( Connection conn ) throws Exception {
    this.connection = conn;
    this.login=conn.login;
    this.host = conn.host;
  }
  public String getLogin() { return login; }
  public String getPassword() { return password; }
  public int port() { return 110; }

  /**
  Add attributes to request.
  @throws java.lang.IllegalArgumentException: unknown attribute type, value is empty
  */
  void addAttributes( RadiusPacket packet, Map map ) {
    Iterator it = map.entrySet().iterator();
    while ( it.hasNext() ) {
      Map.Entry pair = (Map.Entry) it.next();
      String name = (String) pair.getKey();
      String value = (String) pair.getValue();
      packet.addAttribute( name, value );
    }
  }

  public void connect() throws Exception {
    client = new RadiusClient( host, secret );
    if ( soTimeout > 0 ) client.setSocketTimeout( soTimeout );
    this.password = connection.password;

    AccessRequest request = new AccessRequest( login, password );
    // add configured attributes to request:
    addAttributes( request, authAttrs );

    /*
    Perform authentification.
    Exceptions thrown:
      SocketTimeoutException
      org.tinyradius.util.RadiusException: response authenticator invalid
    */
    authSent++;
    long time = System.currentTimeMillis();
    RadiusPacket packet = client.authenticate( request );
    time = System.currentTimeMillis()-time;
    authTime+=time;
    if ( time > maxAuthTime ) maxAuthTime = time;

    if ( runner.debug ) System.out.println( packet );
    if ( packet.getPacketType() == packet.ACCESS_REJECT ) {
      loggedin = false;
      authReject++;
    } else if ( packet.getPacketType() == packet.ACCESS_ACCEPT ) {
      loggedin = true;
      authAccept++;
    } else {
      System.out.println("Unknown response: "+packet.getPacketType()+" "+packet.getPacketTypeName());
    }

    if ( loggedin && accounting ) {
      // types:ACCT_STATUS_TYPE_START, ACCT_STATUS_TYPE_STOP, ACCT_STATUS_TYPE_ACCOUNTING_ON, ACCT_STATUS_TYPE_ACCOUNTING_OFF, ACCT_STATUS_TYPE_INTERIM_UPDATE

      // start session
      AccountingRequest acc = new AccountingRequest(login, AccountingRequest.ACCT_STATUS_TYPE_START);
      if ( addSession ) acc.addAttribute( "Acct-Session-Id", Thread.currentThread().getName() );
      addAttributes( acc, acctAttrs );
      acctStartSent++;
      time = System.currentTimeMillis();
      packet = client.account(acc);
      time = System.currentTimeMillis()-time;
      acctTime+=time;
      acctStartRecv++;
      if ( time > maxAcctTime ) maxAcctTime = time;
      if ( runner.debug ) System.out.println( packet );
      if ( requestDelay > 0 ) Thread.sleep( requestDelay );

      // send given number of interim packets
      for ( int i = 0; i < interim; i++ ) {
        acc = new AccountingRequest(login, AccountingRequest.ACCT_STATUS_TYPE_INTERIM_UPDATE);
        if ( addSession ) acc.addAttribute( "Acct-Session-Id", Thread.currentThread().getName() );
        addAttributes( acc, acctAttrs );
        acctInterimSent++;
        time = System.currentTimeMillis();
        packet = client.account(acc);
        time = System.currentTimeMillis()-time;
        acctTime+=time;
        acctInterimRecv++;
        if ( time > maxAcctTime ) maxAcctTime = time;
        if ( runner.debug ) System.out.println( packet );
        if ( requestDelay > 0 ) Thread.sleep( requestDelay );
      }

      // stop session
      acc = new AccountingRequest(login, AccountingRequest.ACCT_STATUS_TYPE_STOP);
      if ( addSession ) acc.addAttribute( "Acct-Session-Id", Thread.currentThread().getName() );
      addAttributes( acc, acctAttrs );
      acctStopSent++;
      time = System.currentTimeMillis();
      packet = client.account(acc);
      time = System.currentTimeMillis()-time;
      acctTime+=time;
      acctStopRecv++;
      if ( time > maxAcctTime ) maxAcctTime = time;
      if ( runner.debug ) System.out.println( packet );
      if ( requestDelay > 0 ) Thread.sleep( requestDelay ); // CHECKME: should we sleep here?
    }
    client.close();
  }
  /**
  Nothing to do; login performed by connect() method
  @return true if radius server responded by Access-Accept, false otherwise
  */
  public boolean login() {
    return loggedin;
  }
  public void close() throws Exception {
    client.close();
  }
  /**
  TODO: measure time for each session, avg response time.
  */
  public String getStatistics() {
    StringBuilder ret = new StringBuilder();
    ret.append("Auth requests: ");
    ret.append(authSent);
    ret.append(" Received accept/reject: ");
    ret.append(authAccept);
    ret.append("/");
    ret.append(authReject);
    ret.append("\n");
    ret.append("Accounting start sent/received: ");
    ret.append(acctStartSent);
    ret.append("/");
    ret.append(acctStartRecv);
    ret.append("\n");
    ret.append("Accounting stop sent/received: ");
    ret.append(acctStopSent);
    ret.append("/");
    ret.append(acctStopRecv);
    ret.append("\n");
    ret.append("Accounting interim sent/received: ");
    ret.append(acctInterimSent);
    ret.append("/");
    ret.append(acctInterimRecv);
    ret.append("\n");
    ret.append("Auth total time: ");
    ret.append(authTime);
    ret.append(" ms\n");
    ret.append("Acct total time: ");
    ret.append(acctTime);
    ret.append(" ms\n");
    ret.append("Auth max response time: ");
    ret.append(maxAuthTime);
    ret.append(" ms\n");
    ret.append("Acct max response time: ");
    ret.append(maxAcctTime);
    ret.append(" ms\n");
    if (authAccept+authReject > 0 ) {
      ret.append("Auth avg response time: ");
      ret.append(authTime/(authAccept+authReject));
      ret.append(" ms\n");
    }
    if ( acctStartRecv+acctStopRecv+acctInterimRecv > 0 ) {
      ret.append("Acct avg response time: ");
      ret.append(acctTime/(acctStartRecv+acctStopRecv+acctInterimRecv));
      ret.append(" ms\n");
    }
    //ret.append("Avg response time: ");
    //ret.append((authTime+acctTime)/(authAccept+authReject+acctStartRecv+acctStopRecv+acctInterimRecv));
    //ret.append(" ms\n");
    return ret.toString();
  }
  /**
  Load radius-specific properties contained in Runner object; Runner must have
  been initialized, or Runner.loadProperties() called prior to this call.
  */
  public static void loadProperties( Runner r ) {
    runner = r;
    secret = r.props.getProperty("radius.secret");

    String tmp = r.props.getProperty("radius.accounting");
    if ( "true".equals( tmp ) ) {
      accounting = true;
    } else if ( "false".equals( tmp )) {
      accounting = false;
    } else {
      System.out.println("ERROR: Cannot read radius.accounting property: "+tmp );
    }

    tmp = r.props.getProperty("radius.addsession");
    if ( "true".equals( tmp ) ) {
      addSession = true;
    } else if ( "false".equals( tmp )) {
      addSession = false;
    } else {
      System.out.println("ERROR: Cannot read radius.addsession property: "+tmp );
    }

    tmp = r.props.getProperty("radius.interim");
    if ( tmp != null ) {
      try {
        interim = Integer.parseInt(tmp);
      } catch ( NumberFormatException nfe ) {
        System.out.println("ERROR: Cannot read radius.interim property: "+tmp+" - "+nfe);
      }
    }

    tmp = r.props.getProperty("radius.requestdelay");
    if ( tmp != null ) {
      try {
        requestDelay = Integer.parseInt(tmp);
      } catch ( NumberFormatException nfe ) {
        System.out.println("ERROR: Cannot read radius.requestdelay property: "+tmp+" - "+nfe);
      }
    }

    tmp = r.props.getProperty("radius.sotimeout");
    if ( tmp != null ) {
      try {
        soTimeout = Integer.parseInt(tmp);
      } catch ( NumberFormatException nfe ) {
        System.out.println("ERROR: Cannot read radius.sotimeout property: "+tmp+" - "+nfe);
      }
    }

    // load auth & acc radius attributes:
    Enumeration e = r.props.propertyNames();
    while ( e.hasMoreElements() ) {
      String name = (String) e.nextElement();
      String val = r.props.getProperty( name );
      if ( name.startsWith( "radius.auth." ) ) {
        name = name.substring( "radius.auth.".length() );
        System.out.println( "AUTH ATTR: "+name+" = "+val );
        authAttrs.put( name, val );
      } else if ( name.startsWith( "radius.acct." )) {
        name = name.substring( "radius.acct.".length() );
        System.out.println( "ACCT ATTR: "+name+" = "+val );
        acctAttrs.put( name, val );
      } else {
        // ignore
      }
    }

  }

  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      System.out.println( "Arguments: host [user]" );
      System.out.println( "           host must be provided in command line" );
      System.out.println( "           if user name is not provided, it's read from dictionary file" );
      System.exit(1);
    }
    String host = args[0];
    String user = null;
    if ( args.length > 1 ) user = args[1];
    Runner r = new Runner();
    r.init( host, user, RADIUSConnector.class );
    loadProperties(r);
    r.start();
  }
}
