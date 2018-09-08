/***
 * MultipleOutputStream.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.io.*;

public class MultipleOutputStream extends OutputStream
{
  OutputStream[] streams;
  
  public MultipleOutputStream(OutputStream... streams)
  {
    this.streams = streams;
  }
  
  public void write(int val) throws IOException
  {
    for (OutputStream stream : streams) {
      stream.write(val);
    }
  }
  
  public void write(byte[] buf) throws IOException
  {
    for (OutputStream stream : streams) {
      stream.write(buf);
    }
  }
  
  public void write(byte[] buf, int off, int len) throws IOException
  {
    for (OutputStream stream : streams) {
      stream.write(buf, off, len);
    }
  }
  
  public void flush() throws IOException
  {
    for (OutputStream stream : streams) {
      stream.flush();
    }
  }
  
  public void close() throws IOException
  {
    for (OutputStream stream : streams) {
      stream.close();
    }
  }
}
