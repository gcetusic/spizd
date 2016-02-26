package com.nimium.spizd;

import java.util.*;
import java.io.*;

public class DictionaryGenerator {
  HashSet set = new HashSet();
  byte[] lower = new byte['z'-'a'+1];
  byte[] upper = new byte['Z'-'A'+1];
  byte[] number = new byte[10];
  byte[] source;
  public int upMinLen = 1;
  public int upMaxLen = 2;
  public int lowMinLen = 4;
  public int lowMaxLen = 8;
  public int numMinLen = 2;
  public int numMaxLen = 3;
  public boolean upperStart = true;
  public boolean numbersEnd = true;
  public boolean useLower = true;
  public boolean useUpper = true;
  public boolean useNumbers = true;
  public boolean mixed = false;
  public int mixedLen = 4;
  public boolean addUpper = true;
  public boolean addLower = true;
  public boolean addNumbers = true;
  public String dictFile = "dictionary.txt";
  static final String LS = System.getProperty("line.separator");
  public boolean debug = true;
  public void init() {
    int cnt = 0;
    for ( byte c = 'a'; c <= 'z'; c++ ) {
      //debug(c);
      lower[cnt++] = c;
    }
    cnt = 0;
    for ( byte c = 'A'; c <= 'Z'; c++ ) {
      //debug(c);
      upper[cnt++] = c;
    }
    cnt = 0;
    for ( byte c = '0'; c <= '9'; c++ ) {
      //debug(c);
      number[cnt++] = c;
    }
    cnt = 0;
    if ( useLower ) cnt += lower.length;
    if ( useUpper ) cnt += upper.length;
    if ( useNumbers ) cnt += number.length;
    source = new byte[ cnt ];
    cnt = 0;
    if ( useLower ) {
      System.arraycopy( lower, 0, source, cnt, lower.length );
      cnt += lower.length;
    }
    if ( useUpper ) {
      System.arraycopy( upper, 0, source, cnt, upper.length );
      cnt += upper.length;
    }
    if ( useNumbers ) {
      System.arraycopy( number, 0, source, cnt, number.length );
      cnt += number.length;
    }
  }
  long getMemory() {
    return Runtime.getRuntime().totalMemory();
  }
  int row = 0;
  void generate(byte[][] buff, byte[]array, int pos, int length) {
    for ( int i = 0; i < array.length; i++ ) {
      buff[row][pos] = array[i];
      //debug( row+" "+pos+" "+array[i]+" "+String.valueOf(buff[row]) ); // debug
      if ( pos < length-1 ) {
        generate( buff, array, pos+1, length );
      } else if ( row < buff.length - 1 ) {
        row++;
        for ( int j = 0; j < pos; j ++ ) {
          buff[row][j] = buff[row-1][j];
        }
      }
    }
  }
  public byte[][] generate(byte[] array, int length) {
    int len = (int)Math.pow(array.length,length);
    long mem = ((long)len*(long)length);
    if ( mem > getMemory() ) {
      println( "generate: allocating array of "+mem+" bytes, available "+getMemory()+" - NOT ENOUGH MEMORY AVAILABLE!" );
    } else {
      debug( "generate: allocating array of "+mem+" bytes, available "+getMemory() );
    }
    byte[][] buff = new byte[len][length];
    debug( "generate: allocated array of "+mem+" bytes, available "+getMemory() );
    row = 0;
    generate( buff, array, 0, length );
    //debug(buff.length+" "+row); //debug
    return buff;
  }
  void generateFile(PrintWriter out, byte[] buff, byte[]array, int pos, int length) {
    for ( int i = 0; i < array.length; i++ ) {
      buff[pos] = array[i];
      //debug( row+" "+pos+" "+array[i]+" "+String.valueOf(buff[row]) ); // debug
      if ( pos < length-1 ) {
        generateFile( out, buff, array, pos+1, length );
      } else {
        out.println( new String( buff ));
      }
    }
  }
  public File generateFile(byte[] array, int length) throws IOException {
    File file = File.createTempFile( "generate", ".txt" );
    int len = (int)Math.pow(array.length,length);
    int lineLen = length+LS.length();
    long mem = ((long)len*(long)(lineLen));
    debug( "generate: allocating file of "+len+" lines "+mem+" bytes: "+file);
    row = 0;
    byte[] buff = new byte[length];
    PrintWriter out = new PrintWriter( new FileWriter( file));
    generateFile( out, buff, array, 0, length );
    out.close();
    //debug(buff.length+" "+row); //debug
    return file;
  }
  public byte[][] combine( byte[][] buf1, byte[][] buf2 ) {
    int len = buf1.length*buf2.length;
    int length = buf1[0].length+buf2[0].length;
    long mem = ((long)len*(long)length);
    if ( mem > getMemory() ) {
      println( "combine: allocating array of "+mem+" bytes, available "+getMemory()+" - NOT ENOUGH MEMORY AVAILABLE!" );
    } else {
      debug( "combine: allocating array of "+mem+" bytes, available "+getMemory() );
    }
    byte[][] ret = new byte[len][length];
    debug( "combine: allocated array of "+mem+" bytes, available "+getMemory() );
    int y = 0;
    for ( int i = 0; i < buf1.length; i++ ) {
      for ( int j = 0; j < buf2.length; j++ ) {
        int x = 0;
        for ( int k = 0; k < buf1[i].length; k++ ) {
          ret[y][x] = buf1[i][k];
          x++;
        }
        for ( int k = 0; k < buf2[j].length; k++ ) {
          ret[y][x] = buf2[j][k];
          x++;
        }
        y++;
      }
    }
    return ret;
  }
  public File combineFile( File file1, File file2 ) throws IOException {
    File file = File.createTempFile( "combine", ".txt" );
    PrintWriter out = new PrintWriter( new FileWriter( file));

    int sepLen = LS.length();
    int lines = 0;

    BufferedReader in1 = new BufferedReader( new FileReader( file1 ));
    debug( "combine: generating file "+file);
    int y = 0;
    String line1 = null;
    long mem = 0;
    while ( (line1 = in1.readLine()) != null ) {
      BufferedReader in2 = new BufferedReader( new FileReader( file2 ));
      String line2 = null;
      while ( (line2 = in2.readLine()) != null ) {
        out.println(line1+line2);
        mem += line1.length()+line2.length()+sepLen;
        lines++;
      }
      in2.close();
    }
    in1.close();
    out.close();
    debug( "combine: allocated file of "+lines+" lines "+mem+" bytes: "+file );
    return file;
  }
  public File generateFile() throws IOException {
    File file = null;
    if ( mixed ) {
      file = generateFile( source, mixedLen );
    } else {
      LinkedList uppers = new LinkedList();
      LinkedList lowers = new LinkedList();
      LinkedList numbers = new LinkedList();
      LinkedList combined = new LinkedList();
      LinkedList dict = new LinkedList();
      if ( useUpper ) {
        println( "Uppercase length "+upMinLen+"-"+upMaxLen );
        for ( int i = upMinLen; i <= upMaxLen; i++ ) {
          println( "Uppercase length "+i );
          File tmp = generateFile( upper, i );
          uppers.add( tmp );
          if ( addUpper ) dict.add( tmp );
        }
      }
      if ( useLower ) {
        println( "Lowercase length "+lowMinLen+"-"+lowMaxLen );
        for ( int i = lowMinLen; i <= lowMaxLen; i++ ) {
          println( "Lowercase length "+i );
          File tmp = generateFile( lower, i );
          lowers.add( tmp );
          if ( addLower ) dict.add( tmp );
        }
      }
      if ( useNumbers ) {
        println( "Numbers length "+numMinLen+"-"+numMaxLen );
        for ( int i = numMinLen; i <= numMaxLen; i++ ) {
          println( "Numbers length "+i );
          File tmp = generateFile( number, i );
          numbers.add( tmp );
          if ( addNumbers ) numbers.add( tmp );
        }
      }
      if ( upperStart ) {
        println("Combining uppercase ("+uppers.size()+") with lowercase ("+lowers.size()+")" );
        Iterator upperIt = uppers.iterator();
        int outCnt=0;
        while ( upperIt.hasNext() ) {
          outCnt++;
          File upper = (File) upperIt.next();
          if ( useLower ) {
            int intCnt = 0;
            Iterator lowerIt = lowers.iterator();
            while ( lowerIt.hasNext() ) {
              intCnt++;
              println("Combining uppercase ("+outCnt+"/"+uppers.size()+") with lowercase ("+intCnt+"/"+lowers.size()+")" );
              File lower = (File) lowerIt.next();
              File out = combineFile( upper, lower );
              combined.add( out );
            }
          }
        }
      } else {
        // TODO: two-way combine
        throw new UnsupportedOperationException( "Not implemented" );
      }
      if ( numbersEnd ) {
        int outCnt=0;
        println("Combining mixedcase ("+combined.size()+") with numbers ("+numbers.size()+")" );
        Iterator combIt = combined.iterator();
        while ( combIt.hasNext() ) {
          outCnt++;
          File comb = (File) combIt.next();
          if ( useNumbers ) {
            int intCnt = 0;
            Iterator numIt = numbers.iterator();
            while ( numIt.hasNext() ) {
              intCnt++;
              println("Combining mixedcase ("+outCnt+"/"+combined.size()+") with numbers ("+intCnt+"/"+numbers.size()+")" );
              File num = (File) numIt.next();
              File out = combineFile( comb, num );
              dict.add( out );
            }
          }
        }
      } else {
        // TODO: two-way combine
        throw new UnsupportedOperationException( "Not implemented" );
      }
      file = new File(dictFile);
      PrintWriter out = new PrintWriter(new FileWriter(file));
      Iterator dictIt = dict.iterator();
      int lines = 0;
      int bytes = 0;
      while( dictIt.hasNext() ) {
        File tmp = (File) dictIt.next();
        BufferedReader in = new BufferedReader( new FileReader( tmp ));
        String line = null;
        while ( (line = in.readLine()) != null ) {
          out.println( line );
          lines++;
          bytes+=(line.length()+LS.length());
        }
        in.close();
      }
      out.close();
      deleteFiles(uppers);
      deleteFiles(lowers);
      deleteFiles(numbers);
      deleteFiles(combined);
      deleteFiles(dict);
      println("Wrote dictionary "+dictFile+" - "+lines+" lines "+bytes+" bytes");
    }
    return file;
  }
  void deleteFiles( LinkedList files ) {
    Iterator it = files.iterator();
    while ( it.hasNext() ) {
      File file = (File) it.next();
      if ( file.delete() ) {
        println( "Deleted "+file );
      } else {
        println( "Can not delete "+file );
      }
    }
  }
  public void print( byte[][] buff ) {
    for ( int i = 0; i < buff.length; i++ ) {
      println( new String(buff[i]) );
    }
  }
  public void println( String arg ) {
    System.out.println( arg );
  }
  public void debug( String arg ) {
    if ( debug ) println( arg );
  }
  public void test() throws IOException {
    upMinLen=1;
    upMaxLen=1;
    lowMinLen=3;
    lowMaxLen=7;
    numMinLen=1;
    numMaxLen=3;
    init();
    generateFile();

    /*
    init();
    generateFile();
    */

    /*
    mixed = true;
    mixedLen = 4;
    init();
    generateFile();
    */

    //generateFile( lower, 6 );
    //combineFile( generateFile( lower, 6 ), generateFile( number, 2 ));
    //print( generate( lower, 6 ));
    //print( combine( generate(upper,1), combine( generate( lower, 4 ), generate( number, 2 ))));
    //print( combine( generate( lower, 6 ), generate(number, 2) ) );
    //combine( generate( upper, 1 ), combine( generate( lower, 6 ), generate(number, 2) ));
  }
  public static void main(String[] args) throws Exception {
    //(new DictionaryGenerator()).test();
    Runner r = new Runner(); // this creates DictionaryGenerator too
    r.loadProperties(); //loads DG properties too
    r.dictGen.init();
    r.dictGen.generateFile();
  }
}
