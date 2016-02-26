package com.nimium.spizd;

import java.util.*;
import java.io.*;

/**
Reads dictionary file
*/
public class DictionaryReader {
  public String dictFile = "dictionary.txt";
  public String separator = ":";
  public boolean circular = false;
  BufferedReader in = null;
  String login;
  String password;
  String defaultLogin;
  /**
  Read and parse a line from input file
  */
  public String readLine() throws IOException {
    String ret = null;
    if ( in == null ) in = new BufferedReader( new FileReader( dictFile ));
    ret = in.readLine();
    if ( ret == null && circular ) {
      in.close();
      in = new BufferedReader( new FileReader( dictFile ));
      ret = in.readLine();
    }
    if ( ret != null ) {
      int pos = 0;
      if (defaultLogin == null && (pos = ret.indexOf(separator)) > 0 ) {
        login = ret.substring(0,pos);
        password = ret.substring(pos+1);
      } else {
        //login = null;
        login = defaultLogin;
        password = ret;
      }
    }
    return ret;
  }
  public String login() {
    return login;
  }
  public String password() {
    return password;
  }
}
