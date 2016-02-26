package com.nimium.spizd;

import javax.mail.*;

/**
Required for java mail API.
*/
public class Auth extends Authenticator {
  Connector conn;
  public Auth( Connector conn ) {
    this.conn = conn;
  }
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication( conn.getLogin(), conn.getPassword());
  }
}

