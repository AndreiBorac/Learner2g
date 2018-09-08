/***
 * ConfigServer.java
 * copyright (c) 2013 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.net.*;
import java.util.*;

public class ConfigServer
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
  
  static final String nil = "null";
  
  static class State
  {
    final AsciiTreeMap<String> map = (new AsciiTreeMap<String>());
    
    String lea(String val)
    {
      return ((val == null) ? nil : val);
    }
    
    String all()
    {
      StringBuilder out = (new StringBuilder());
      
      for (Map.Entry<String, String> entry : map.entrySet()) {
        out.append("'" + entry.getKey() + "' => '" + lea(entry.getValue()) + "'\n");
      }
      
      return out.toString();
    }
    
    String get(String key)
    {
      return lea(map.get(key));
    }
    
    String put(String key, String val)
    {
      String old;
      
      if (val.equals(nil)) {
        old = map.remove(key);
      } else {
        old = map.put(key, val);
      }
      
      return lea(old);
    }
    
    int getAsInteger(String key)
    {
      return Integer.parseInt(get(key));
    }
    
    String putAsInteger(String key, int val)
    {
      return put(key, Integer.toString(val));
    }
  }
  
  static enum Opcode
  {
    ALL
      {
        String effect(State state, String key, String val)
        {
          return state.all();
        }
      },
      
    GET
      {
        String effect(State state, String key, String val)
        {
          return state.get(key);
        }
      },
    
    PUT
      {
        String effect(State state, String key, String val)
        {
          String old = state.get(key);
          state.put(key, val);
          return old;
        }
      },
    
    INC
      {
        String effect(State state, String key, String val)
        {
          return state.putAsInteger(key, (state.getAsInteger(key) + 1));
        }
      },
    
    DEC
      {
        String effect(State state, String key, String val)
        {
          return state.putAsInteger(key, (state.getAsInteger(key) - 1));
        }
      };
    
    abstract String effect(State state, String key, String val);
  }
  
  public static class Server
  {
    public static void main(String[] args)
    {
      try {
        int argi = 0;
        
        final int port = Integer.parseInt(args[argi++]);
        
        final State state = (new State());
        
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
                         
                         while (true) {
                           Opcode opc = Opcode.valueOf(inp.readUTF());
                           String key = inp.readUTF();
                           String val = inp.readUTF();
                           
                           String ret;
                           
                           synchronized (state) {
                             ret = opc.effect(state, key, val);
                           }
                           
                           out.writeUTF(ret);
                           out.flush();
                         }
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
  
  public static class Client
  {
    public static void main(String[] args)
    {
      try {
        int argi = 0;
        
        final String host = args[argi++];
        final int    port = Integer.parseInt(args[argi++]);
        
        final String opc = args[argi++];
        final String key = args[argi++];
        final String val = args[argi++];
        
        final Socket server = (new Socket(host, port));
        
        final DataInputStream  dis = (new DataInputStream (new BufferedInputStream (server.getInputStream ())));
        final DataOutputStream dos = (new DataOutputStream(new BufferedOutputStream(server.getOutputStream())));
        
        for (String elm : (new String[] { opc, key, val })) {
          dos.writeUTF(elm);
        }
        
        dos.flush();
        
        System.out.println(dis.readUTF());
        
        server.close();
      } catch (Throwable e) {
        fatal(e);
      }
    }
  }
}
