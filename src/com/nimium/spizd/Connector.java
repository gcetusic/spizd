package com.nimium.spizd;

import java.util.*;

/**
This interface is used by Runner threads.
@see Runner
@see Connection
*/
public interface Connector {
  /**
  Initialize the connector with given host and user
  */
  public void init( Connection c ) throws Exception;
  /**
  Connect.
  */
  public void connect() throws Exception;
  /**
  Login after sucessfull connection.
  @return true after sucessfull login, false otherwise
  */
  public boolean login() throws Exception;
  /**
  Close connection
  */
  public void close() throws Exception;
  /**
  Required for authentication.
  @see Auth
  @return login name
  */
  public String getLogin();
  /**
  Required for authentication.
  @see Auth
  @return password
  */
  public String getPassword();
  /**
  Return port required to connect.
  */
  public int port();
  /**
  Called by after processing is finished to fetch the statistics.
  */
  public String getStatistics();
}
