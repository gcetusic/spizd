package com.nimium.spizd;

import java.io.*;
import javax.mail.*;
import java.util.*;
import java.net.*;

import org.vrspace.util.*;

/**
This class starts and maintains Connector threads.
@see Connector
*/
public class Runner {
  public static String propFilePath = "../etc/spizd.properties";
  /** maximum number of concurrent threads/connections to start*/
  public int maxThreads = 1000;
  /** if we are to decrease number of threads/concurrent connections after a connection fails*/
  public boolean decreaseThreads = false;
  /** report for each sucessfull connection */
  public boolean verboseConnect = false;
  /** report for each sucessfull login */
  public boolean verboseLogin = false;
  /** report for each unsucessfull connection */
  public boolean verboseFail = false;
  /** Print total threads started after this much started threads */
  public int reportLines = 1000;
  /** Print debug messages ? */
  public boolean debug = false;
  /** Close connections? Default: true */
  public boolean closeConnections = true;
  /** sleep for this many microseconds after starting a thread, 0 = no sleep */
  public long threadDelay = 0;
  /** TCP probe sets socket timeout to this many milliseconds; probe itself closes all sockets only after all threads have been started*/
  public int probeTimeout = 60000;
  /** Note: DateFormat is not synchronized */
  public static final java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  Properties props;

  // These are volatile to avoid thread-local caching. This still doesn't mean we get accurate statistics.
  //volatile int curThreads = 0;
  volatile int totThreads = 0;
  volatile int totConn = 0;
  volatile int curConn = 0;

  volatile boolean active = true;
  ThreadGroup group = new ThreadGroup( "Sessions" );
  DictionaryGenerator dictGen = new DictionaryGenerator();
  DictionaryReader dictRead = new DictionaryReader();
  Class connClass;
  String host;
  String login;
  String response = null;

  /**
  Initialization.
  @param host ip address or resolvable hostname
  @param login login name to use for connections; may be null, depending on connector
  @param connector a subclass of Connector
  @see Connector
  */
  public void init( String host, String login, Class connector ) throws IOException {
    connClass = connector;
    this.host = host;
    this.login = login;
    loadProperties();
  }
  /**
  Loads properties from ../etc/spizd.properties
  */
  public void loadProperties() throws IOException {
    URL url = Util.getLocation(this);
    String propUrl = url.getProtocol()+":"+Util.getDir(url.toString())+propFilePath;
    System.out.println("Loading properties from "+propUrl );
    props = Util.loadProperties( new Properties(), propUrl );

    String tmp = props.getProperty("spizd.maxThreads");
    if ( tmp != null ) maxThreads = Integer.parseInt(tmp);

    tmp = props.getProperty("spizd.close");
    if ( "false".equals( tmp ) ) closeConnections = false;
    else if ( "true".equals(tmp) ) closeConnections = true;

    tmp = props.getProperty("spizd.threadDelay");
    if ( tmp != null ) threadDelay = Integer.parseInt(tmp);

    tmp = props.getProperty("spizd.probeTimeout");
    if ( tmp != null ) probeTimeout = Integer.parseInt(tmp);

    tmp = props.getProperty("spizd.reportLines");
    if ( tmp != null ) reportLines = Integer.parseInt(tmp);

    tmp = props.getProperty("spizd.decreaseThreads");
    if ( "false".equals( tmp ) ) decreaseThreads = false;
    else if ( "true".equals(tmp) ) decreaseThreads = true;

    tmp = props.getProperty("spizd.verboseConnect");
    if ( "false".equals( tmp ) ) verboseConnect = false;
    else if ( "true".equals(tmp) ) verboseConnect = true;

    tmp = props.getProperty("spizd.verboseLogin");
    if ( "false".equals( tmp ) ) verboseLogin = false;
    else if ( "true".equals(tmp) ) verboseLogin = true;

    tmp = props.getProperty("spizd.verboseFail");
    if ( "false".equals( tmp ) ) verboseFail = false;
    else if ( "true".equals(tmp) ) verboseFail = true;

    tmp = props.getProperty("spizd.debug");
    if ( "false".equals( tmp ) ) debug = false;
    else if ( "true".equals(tmp) ) debug = true;

    // dict properties
    tmp = props.getProperty("dict.file");
    if ( tmp != null ) {
      dictGen.dictFile = Util.getDir(url.toString())+"../etc/"+tmp;
      dictRead.dictFile = dictGen.dictFile;
      dictRead.defaultLogin = this.login;
      System.out.println("Dictionary file: "+dictGen.dictFile);
    }

    tmp = props.getProperty("dict.separator");
    if ( tmp != null ) {
      dictRead.separator = tmp;
    }

    tmp = props.getProperty("spizd.circular");
    if ( "false".equals( tmp ) ) dictRead.circular = false;
    else if ( "true".equals(tmp) ) dictRead.circular = true;


    tmp = props.getProperty("dict.upMinLen");
    if ( tmp != null ) dictGen.upMinLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.upMaxLen");
    if ( tmp != null ) dictGen.upMaxLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.lowMinLen");
    if ( tmp != null ) dictGen.lowMinLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.lowMaxLen");
    if ( tmp != null ) dictGen.lowMaxLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.numMinLen");
    if ( tmp != null ) dictGen.numMinLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.numMaxLen");
    if ( tmp != null ) dictGen.numMaxLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.mixedLen");
    if ( tmp != null ) dictGen.mixedLen = Integer.parseInt(tmp);

    tmp = props.getProperty("dict.upperStart");
    if ( "false".equals( tmp ) ) dictGen.upperStart = false;
    else if ( "true".equals(tmp) ) dictGen.upperStart = true;

    tmp = props.getProperty("dict.numbersEnd");
    if ( "false".equals( tmp ) ) dictGen.numbersEnd = false;
    else if ( "true".equals(tmp) ) dictGen.numbersEnd = true;

    tmp = props.getProperty("dict.useNumbers");
    if ( "false".equals( tmp ) ) dictGen.useNumbers = false;
    else if ( "true".equals(tmp) ) dictGen.useNumbers = true;

    tmp = props.getProperty("dict.addNumbers");
    if ( "false".equals( tmp ) ) dictGen.addNumbers = false;
    else if ( "true".equals(tmp) ) dictGen.addNumbers = true;

    tmp = props.getProperty("dict.useUpper");
    if ( "false".equals( tmp ) ) dictGen.useUpper = false;
    else if ( "true".equals(tmp) ) dictGen.useUpper = true;

    tmp = props.getProperty("dict.addUpper");
    if ( "false".equals( tmp ) ) dictGen.addUpper = false;
    else if ( "true".equals(tmp) ) dictGen.addUpper = true;

    tmp = props.getProperty("dict.useLower");
    if ( "false".equals( tmp ) ) dictGen.useLower = false;
    else if ( "true".equals(tmp) ) dictGen.useLower = true;

    tmp = props.getProperty("dict.addLower");
    if ( "false".equals( tmp ) ) dictGen.addLower = false;
    else if ( "true".equals(tmp) ) dictGen.addLower = true;
  }
  /**
  Start probe to see maximum number of concurrent connections local and target system can handle.
  */
  public void probe() {
    //curThreads = 0;
    LinkedList threads = new LinkedList();
    for ( int i = 0; i < maxThreads; i++ ) {
      if ( i/100*100 == i ) {
        System.out.println( i );
      }
      ProbeThread t = new ProbeThread( host, connClass );
      try {
        (new Thread(t)).start();
        threads.add( t );
        //TODO: ?
        if ( threadDelay > 0 ) {
          Thread.sleep( threadDelay );
        }
      } catch ( OutOfMemoryError oome ) {
        //active = false;
        //maxThreads = curThreads;
        maxThreads = curThreads();
        System.out.println("OOM at thread "+maxThreads+" - maxThreads updated");
      } catch ( InterruptedException ie ) {
        active = false;
        //System.out.println("Interrupted, exiting.");
      }
    }
    //System.out.println( "Started "+curThreads+" threads for "+host );
    System.out.println( "Started "+threads.size()+" threads for "+host );
    Iterator it = threads.iterator();
    int cnt = 0;
    while ( it.hasNext() ) {
      ProbeThread t = (ProbeThread) it.next();
      if ( t.close() ) {
        cnt++;
      }
    }
    System.out.println(host+" capable of handling "+cnt+" connections" );
    maxThreads = cnt;
  }
  public int curThreads() {
    return group.activeCount();
  }
  /**
  Class implementing probes.
  */
  public class ProbeThread implements Runnable {
    String host;
    Class cls;
    Socket socket;
    boolean working = true;
    boolean readOK = false;
    public ProbeThread( String host, Class connectorClass ) {
      this.host = host;
      this.cls = connectorClass;
    }
    /**
    Read one line from socket, and terminate.
    */
    public void run() {
      if ( active ) {
        //curThreads++;
        try {
          Connector conn = (Connector) connClass.newInstance();
          socket = new Socket( host, conn.port() );
          socket.setSoTimeout( probeTimeout );
          BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()) );
          String line = in.readLine();
          if ( response == null && line != null) {
            response = line;
            System.out.println(line);
          }
          //readOK = true;
          readOK = (line != null);
        } catch ( InstantiationException ie ) {
          active = false;
          System.out.println("Startup failed: "+ie);
        } catch ( IllegalAccessException ie ) {
          active = false;
          System.out.println("Startup failed: "+ie);
        } catch ( UnknownHostException uhe ) {
          active = false;
          System.out.println("Startup failed: "+uhe);
        } catch ( SocketTimeoutException ste ) {
          // read timeout.
        } catch ( ConnectException ce ) {
          // ConnectException: SocketTimeOut
          if ( verboseFail ) System.out.println("Startup of thread "+group.activeCount()+" failed: "+ce);
        } catch ( SocketException se ) {
          // client side java.net.SocketException: Too many open files
          active = false;
          socket = null;
          System.out.println("Local connection limit reached at "+group.activeCount()+": "+se );
        } catch ( IOException ioe ) {
          active = false;
          socket = null;
          System.out.println("Connection limit reached? "+ioe );
        }
        //curThreads--;
      }
      working = false;
    }
    public boolean close() {
      try {
        while ( working ) {
          Thread.sleep(100);
        }
        if ( socket != null && socket.isConnected() ) {
          socket.close();
        }
      } catch ( IOException ioe ) {
        System.out.println( "Cannot close socket "+socket+" - "+ioe);
      } catch ( InterruptedException ie ) {
        System.out.println( "Interrupted");
      }
      return readOK;
    }
  }
  /**
  I'd rather avoid performance loss for synchronization, but we do need some statistics.
  synchronized void incThreads() {
    curThreads++;
    //Thread.yield();
  }
  synchronized void decThreads() {
    //Thread.yield();
    curThreads--;
  }
  */
  /**
  Start a fixed number of threads/connections and maintain it.
  */
  public void start() throws Exception {
    Connector instance = (Connector) connClass.newInstance();
    active = true;
    //curThreads = 0;
    // only way to ensure all started threads are stopped:
    Runtime.getRuntime().addShutdownHook( new Thread( new ShutdownHook(instance) ) );

    long time = System.currentTimeMillis();
    String line = null;
    int cnt = 0;
    // continue if in == null for HTTPConnector
    while ( active && (line = dictRead.readLine()) != null) {
      cnt++;
      //System.out.println("Line "+cnt+": "+line);
      if ( cnt/reportLines*reportLines == cnt ) {
        System.out.println( dateFormat.format( new Date() )+ " sessions " + cnt + " active threads "+group.activeCount() );
      }
      //RunnerThread t = new RunnerThread( host, login, line, connClass );
      Connection conn = new Connection( this, host, line, dictRead.login(), dictRead.password(), connClass );
      try {
        (new Thread(group, conn, "SPIZD"+cnt)).start();
        if ( threadDelay > 0 ) {
          Thread.sleep( threadDelay );
        }
      } catch ( OutOfMemoryError oome ) {
        //active = false;
        //maxThreads = curThreads;
        maxThreads = group.activeCount();
        System.out.println("OOM at thread "+maxThreads+" - maxThreads updated");
      } catch ( InterruptedException ie ) {
        active = false;
        System.out.println("Interrupted, exiting.");
      }
      while ( active && curThreads() >= maxThreads ) {
        Thread.yield();
      }
    }
    System.out.println( "DONE: "+totThreads+" concurrent "+cnt+" total threads started in "+(System.currentTimeMillis() - time)+" ms" );
    /*
    // wait for all threads to finish
    while ( curThreads > 0 ) {
      //Thread.yield();
      Thread.sleep(1000);
    }
    System.out.println( "Total concurrent connections: "+totConn+" content fetched in "+(System.currentTimeMillis() - time)+" ms" );
    System.out.println( instance.getStatistics() );
    */
  }
  public class ShutdownHook implements Runnable {
    Connector instance;
    long time = System.currentTimeMillis();
    public ShutdownHook( Connector instance ) {
      this.instance = instance;
    }
    public void run() {
      System.out.println( "Total concurrent connections: "+totConn+", content fetched in "+(System.currentTimeMillis() - time)+" ms" );
      if (instance.getStatistics() != null) System.out.println( instance.getStatistics() );
    }
  }
  /**
  Starts probing with given host and protocol
  */
  public static void main( String[] args ) throws Exception {
    if ( args.length < 2 ) {
      System.out.println( "Arguments: host protocol|port" );
      System.out.println( "Supported protocols: smtp pop imap ssh http" );
      System.out.println( "Port: any tcp port number" );
      System.exit(1);
    }
    String host = args[0];
    String proto = args[1];
    Class connClass = null;
    if ( "pop".equals( proto ) || "pop3".equals( proto ) ) {
      connClass = POPConnector.class;
    } else if ( "imap".equals( proto )) {
      connClass = IMAPConnector.class;
    } else if ( "http".equals( proto )) {
      connClass = HTTPConnector.class;
    } else if ( "ssh".equals( proto )) {
      connClass = SSHConnector.class;
    } else if ( "smtp".equals( proto )) {
      connClass = SMTPConnector.class;
    } else {
      // proto number as argument
      connClass = TCPConnector.class;
      try {
        TCPConnector.port = Integer.parseInt( proto );
      } catch (NumberFormatException nfe) {
        System.out.println( "Unsupported protocol: "+proto );
        System.exit(2);
      }
    }
    Runner r = new Runner();
    r.init( host, null, connClass );
    System.out.println("Probing "+host+":"+proto+" with "+r.maxThreads+" concurrent connections" );
    r.probe();
  }
}
