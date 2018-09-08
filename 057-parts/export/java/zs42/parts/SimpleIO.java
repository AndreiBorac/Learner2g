/***
 * SimpleIO.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.parts;

import java.io.*;

public class SimpleIO
{
  public static final int SLURP_BLOCK_SIZE = 8192;
  
  public static void liquidate(Closeable closeable)
  {
    try {
      closeable.close();
    } catch (Throwable e) {
      // ignored
    }
  }
  
  public static void liquidate(Closeable... closeables)
  {
    for (Closeable closeable : closeables) {
      liquidate(closeable);
    }
  }
  
  public static byte[] readByteArray(DataInputStream inp) throws IOException
  {
    byte[] arr = (new byte[inp.readInt()]);
    inp.readFully(arr);
    return arr;
  }
  
  public static void writeByteArray(DataOutputStream out, byte[] arr) throws IOException
  {
    out.writeInt(arr.length);
    out.write(arr);
  }
  
  public static String readByteArrayString(DataInputStream inp) throws IOException
  {
    return PartsUtils.arr2str(readByteArray(inp));
  }
  
  public static void writeByteArrayString(DataOutputStream out, String str) throws IOException
  {
    writeByteArray(out, PartsUtils.str2arr(str));
  }
  
  public static byte[] slurp(InputStream inp)
  {
    try {
      ByteArrayOutputStream out = (new ByteArrayOutputStream());
      
      byte[] buf = (new byte[SLURP_BLOCK_SIZE]);
      
      int amt;
      
      while ((amt = inp.read(buf, 0, buf.length)) > 0) {
        out.write(buf, 0, amt);
      }
      
      return out.toByteArray();
    } catch (Exception e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static byte[] slurp(String filename)
  {
    try {
      final FileInputStream inp = (new FileInputStream(filename));;
      
      try {
        return slurp(inp);
      } finally {
        liquidate(inp);
      }
    } catch (IOException e) {
      throw (new RuntimeException(e));
    }
  }
  
  public static void streamCopy(InputStream inp, OutputStream out, long len, byte[] aux) throws IOException
  {
    while (len > 0) {
      int amt = inp.read(aux, 0, ((int)(Math.min(len, aux.length))));
      out.write(aux, 0, amt);
      len -= amt;
    }
  }
  
  public static void streamCopy(InputStream inp, File out, long len, byte[] aux) throws IOException
  {
    FileOutputStream fos = (new FileOutputStream(out));
    
    try {
      streamCopy(inp, fos, len, aux);
    } finally {
      liquidate(fos);
    }
  }
  
  public static void streamCopy(File inp, OutputStream out, long len, byte[] aux) throws IOException
  {
    FileInputStream fis = (new FileInputStream(inp));
    
    try {
      streamCopy(fis, out, len, aux);
    } finally {
      liquidate(fis);
    }
  }
  
  public static void streamCopy(File inp, OutputStream out, byte[] aux) throws IOException
  {
    streamCopy(inp, out, inp.length(), aux);
  }
  
  public static void streamCopy(File inp, File out, long len, byte[] aux) throws IOException
  {
    FileInputStream fis = (new FileInputStream(inp));
    
    try {
      streamCopy(fis, out, len, aux);
    } finally {
      liquidate(fis);
    }
  }
  
  public static void streamCopy(File inp, File out, byte[] aux) throws IOException
  {
    streamCopy(inp, out, inp.length(), aux);
  }
}
