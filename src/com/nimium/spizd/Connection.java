package com.nimium.spizd;

import java.io.*;
import javax.mail.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.vrspace.util.*;

/**
Connector thread implementation: connection contains all the data required to
connect, and calls appropriate connectors.
@see Runner
@see Connector
*/
public class Connection implements Runnable {
  public Runner runner;
  public String host;
  public String login;
  public String password;
  /** line read from dictionary/urllist file */
  public String line;
  /** connector class */
  public Class cls;
  // only purpose of this queue is keeping references to connections, thus keeping them open after thread death
  static LinkedBlockingQueue connQ = new LinkedBlockingQueue();
  public Connection( Runner runner, String host, String line, String login, String password, Class connectorClass ) {
    this.runner = runner;
    this.host = host;
    this.line = line;
    this.login = login;
    this.password = password;
    this.cls = connectorClass;
  }
  /**
  Starts a Connector and processes it's results.
  */
  public void run() {
    if ( runner.debug ) System.out.println("Connection starting" );
    //FIXME: try to close connector after exception occurs
    if ( runner.active ) {
      //runner.curThreads++;
      //runner.incThreads();
      if ( runner.curThreads() > runner.totThreads ) runner.totThreads = runner.curThreads();
      if ( runner.debug ) System.out.println("Attempting connection to "+this.host+" as "+this.login+"/"+this.password );
      Connector conn = null;
      try {
        conn = (Connector) cls.newInstance();
        conn.init( this );
        runner.curConn++;
        if ( runner.curConn > runner.totConn ) runner.totConn = runner.curConn;
        //conn.connect( this.password );
        conn.connect();
        if ( runner.verboseConnect ) System.out.println("CONNECTED: "+conn.getLogin()+"/"+conn.getPassword());
        //active = false;
        boolean loggedIn = conn.login();
        if ( runner.verboseLogin ) System.out.println("LOGGED IN: "+loggedIn);
        if ( runner.closeConnections ) {
          conn.close();
          runner.curConn--;
        } else {
          // not closing sockets is not enough to keep connections open forever - GC closes them in finalization
          // so we also need to keep them referenced somewhere outside of this thread's context
          connQ.add( conn );
        }
      } catch ( InstantiationException ie ) {
        runner.active = false;
        System.out.println("FATAL: Startup failed: "+ie);
      } catch ( IllegalAccessException ie ) {
        runner.active = false;
        System.out.println("FATAL: Startup failed: "+ie);
      } catch ( AuthenticationFailedException afe ) {
        if ( runner.verboseLogin ) System.out.println("Login failed : "+login+"/"+password);
      } catch ( MessagingException me ) {
        // this is not fatal error in fact, i.e. server may timeout during ssl handshake
        //runner.active = false;
        //System.out.println("FATAL: Startup of thread "+runner.curThreads+" failed: "+me);
        if ( runner.verboseFail ) System.out.println("Startup of thread "+runner.curThreads()+" failed: "+me);
        if ( runner.decreaseThreads ) runner.maxThreads = runner.curThreads() - 1;
        if ( me.getCause() instanceof javax.net.ssl.SSLHandshakeException || me.getCause() instanceof com.sun.mail.iap.ProtocolException ) {
          System.out.println("\nThis may mean server sent self-signed certificate, run bin/installcert.sh\n");
          runner.active = false;
        }
      } catch ( UnknownHostException uhe ) {
        runner.active = false;
        System.out.println("FATAL: Startup failed: "+uhe);
      } catch ( NoRouteToHostException nrthe ) {
        System.out.println("FATAL: Startup of thread "+runner.curThreads()+" failed: "+nrthe);
        runner.active = false;
        runner.curConn--;
      } catch ( SocketException se ) {
        // max open sockets
        //active = false;
        if ( runner.verboseFail ) System.out.println("Startup of thread "+runner.curThreads()+" failed: "+se);
        if ( runner.decreaseThreads ) runner.maxThreads = runner.curThreads() - 1;
        runner.curConn--;
      } catch ( RuntimeException re ) {
        // this catches NullPointerExceptions and such
        System.out.println("FATAL: Startup of thread "+runner.curThreads()+" failed: "+re);
        runner.active = false;
        throw re;
      } catch ( SocketTimeoutException ste ) {
        if ( runner.verboseFail ) System.out.println("Execution of thread "+runner.curThreads()+" failed: "+ste);
        try {
          conn.close();
        } catch ( Exception e ) {
          e.printStackTrace(System.out);
        }
      } catch ( Exception e ) {
        try {
          conn.close();
        } catch ( Exception whatever ) {
          whatever.printStackTrace(System.out);
        }
        //active = false;
        if ( runner.verboseFail ) System.out.println("Startup of thread "+runner.curThreads()+" failed: "+e);
        if ( runner.debug ) e.printStackTrace(System.out);
        runner.curConn--;
      }
      //runner.curThreads--;
      //runner.decThreads();
    }
  }
}

