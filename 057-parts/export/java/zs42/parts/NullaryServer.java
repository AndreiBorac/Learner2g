/***
 * ConfigServer.java
 * copyright (c) 2013 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.net.*;
import java.util.*;

public class NullaryServer
{
  static void log(String line)
  {
    System.err.println(line);
    System.err.flush();
  }
  
  static void log(Throwable e)
  {
    e.printStackTrace(System.err);
    System.err.flush();
  }
  
  static RuntimeException fatal(Throwable e)
  {
    System.err.println("!!! ENTER FATAL EXCEPTION BACKTRACE !!!");
    log(e);
    System.err.println("!!! LEAVE FATAL EXCEPTION BACKTRACE !!!");
    System.err.flush();
    
    Runtime.getRuntime().halt(1);
    
    while (true);
  }
  
  static final F0<Boolean>                     allow_F0 = (new F0<Boolean>() { public Boolean invoke() { return Boolean.TRUE; } });
  
  static final F0<RuntimeException>            throw_F0 = (new F0<RuntimeException>() { public RuntimeException invoke() { return (new RuntimeException()); } });
  static final F1<RuntimeException, Throwable> throw_F1 = (new F1<RuntimeException, Throwable>() { public RuntimeException invoke(Throwable e) { return (new RuntimeException(e)); } });
  static final F1<Void, Throwable>             fatal_F1 = (new F1<Void, Throwable>() { public Void invoke(Throwable e) { throw fatal(e); } });
  
  public static void main(String[] args)
  {
    try {
      int argi = 0;
      
      final int port = Integer.parseInt(args[argi++]);
      
      AbstractServer.launch
        ((new F0<AbstractServer<Void>>()
          {
            public AbstractServer<Void> invoke()
            {
              return
                (new AbstractServer<Void>()
                 {
                   public void handle(Void context, Socket client)
                   {
                     try {
                       final DataInputStream  inp = (new DataInputStream (new BufferedInputStream (client.getInputStream ())));
                       final DataOutputStream out = (new DataOutputStream(new BufferedOutputStream(client.getOutputStream())));
                       
                       byte[] buf = (new byte[4096]);
                       
                       while (inp.read(buf, 0, buf.length) >= 1);
                     } catch (Throwable e) {
                       log(e); // try to continue operating regardless ...
                     }
                   }
                 });
            }
          }), null, port, 8, 8, allow_F0, fatal_F1, fatal_F1, fatal_F1);
    } catch (Throwable e) {
      fatal(e);
    }
  }
}
