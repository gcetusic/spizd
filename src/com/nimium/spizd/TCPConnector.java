package com.nimium.spizd;

import java.util.*;
import java.net.*;
import java.io.*;

/**
Generic TCP connector used for probe only.
*/
public class TCPConnector implements Connector {
  static int port;
  String host;
  Socket socket;
  public void init( Connection c ) {
    this.host = c.host;
  }
  public String getLogin() { return null; }
  public String getPassword() { return null; }
  public int port() { return port; }
  public void connect() throws IOException {
    socket = new Socket( host, port );
  }
  public boolean login() {
    return false;
  }
  public void close() throws Exception {
    socket.close();
  }
  public String getStatistics() {
    return null;
  }
}
