/***
 * Inspector.java
 * copyright (c) 2011 by andrei borac
 ***/

package gotm.onlf.splitter.server;

import java.io.*;

import zs42.parts.*;

import gotm.onlf.splitter.common.*;

import static gotm.onlf.splitter.common.Constants.*;

import static gotm.onlf.utilities.Utilities.*;

public class Inspector
{
  static char sanitize(char x)
  {
    if (x == ' ') return x;
    
    if (('$' <= x) && (x <= '&')) return x;
    if (('(' <= x) && (x <= ')')) return x;
    if (('*' <= x) && (x <= '/')) return x;
    if (('0' <= x) && (x <= '9')) return x;
    if ((':' <= x) && (x <= '@')) return x;
    if (('A' <= x) && (x <= 'Z')) return x;
    if (('[' <= x) && (x <= '[')) return x;
    if ((']' <= x) && (x <= ']')) return x;
    if (('^' <= x) && (x <= '_')) return x;
    if (('a' <= x) && (x <= 'z')) return x;
    if (('{' <= x) && (x <= '}')) return x;
    
    return '~';
  }
  
  private static String b2s(byte[] arr, int off, int lim)
  {
    if (!((off <= lim) && (lim <= arr.length))) throw null;
    
    char[] out = (new char[(lim - off)]);
    
    for (int i = 0; i < out.length; i++) {
      out[i] = sanitize(((char)(((int)(arr[off + i])) & 0xFF)));
    }
    
    return (new String(out));
  }
  
  public static void main(String[] args)
  {
    try {
      final DataInputStream  dis = (new DataInputStream(System.in));
      final DataOutputStream dos = (new DataOutputStream(System.out));
      
      (new Thread()
        {
          public void main(String[] args) {
            try {
              while (true) {
                synchronized (dos) {
                  dos.flush();
                }
                
                sleep(250);
              }
            } catch (Throwable e) {
              fatal(e);
            }
          }
        }).start();
      
      final int  [] length      = (new int  [1]);
      final long [] instance_id = (new long [1]);
      final byte [] token       = (new byte [1]);
      
      final byte [] buffer = (new byte[MAXIMUM_FEEDBACK_PACKET_LENGTH]);
      
      F0<Void> handler;
      
      if (args.length != 1) {
        throw (new RuntimeException("bad usage"));
      }
      
      /****/ if (args[0].equals("diet")) {
        handler =
          (new F0<Void>()
           {
             public Void invoke()
             {
               try {
                 if (token[0] != RET_PACKET_RECEIVED) {
                   dos.writeInt(length[0] + 9);
                   dos.writeLong(instance_id[0]);
                   dos.writeByte(token[0]);
                   dos.write(buffer, 0, length[0]);
                 }
                 
                 return null;
               } catch (Throwable e) {
                 throw (new RuntimeException(e));
               }
             }
           });
      } else if (args[0].equals("dump")) {
        handler =
          (new F0<Void>()
           {
             private int counter = 100000;
             
             private String header()
             {
               return ("" + (counter++));
             }
             
             public Void invoke()
             {
               try {
                 switch (token[0])
                 {
                   case RET_RESERVED:
                   {
                     dos.writeBytes(header() + ": reserved: " + instance_id[0] + ": " + b2s(buffer, 0, length[0]) + "\n");
                     break;
                   }
                   
                   case RET_FEEDBACK:
                   {
                     dos.writeBytes(header() + ": feedback: " + instance_id[0] + ": " + b2s(buffer, 0, length[0]) + "\n");
                     break;
                   }
                   
                   case RET_COMMAND_REPLY:
                   {
                     dos.writeBytes(header() + ": cmdreply: " + instance_id[0] + ": " + b2s(buffer, 0, length[0]) + "\n");
                     break;
                   }
                   
                   case RET_PACKET_RECEIVED:
                   {
                     if (length[0] == (3 * 4)) {
                       DataInputStream dis = (new DataInputStream(new ByteArrayInputStream(buffer, 0, length[0])));
                       dos.writeBytes(header() + ": pcktrcpt: " + instance_id[0] + ": stream_id(" + dis.readInt() + "), source_tc(" + dis.readInt() + "), client_tc(" + dis.readInt() + ")\n");
                     } else {
                       dos.writeBytes(header() + ": pcktrcpt: (invalid)\n");
                     }
                     
                     break;
                   }
                   
                   case RET_QUEUE_SIZES:
                   {
                     if (length[0] == (2 * 4)) {
                       DataInputStream dis = (new DataInputStream(new ByteArrayInputStream(buffer, 0, length[0])));
                       dos.writeBytes(header() + ": queuelen: " + instance_id[0] + ": a(" + dis.readInt() + "), v(" + dis.readInt() + ")\n");
                     } else if (length[0] == (4 * 4)) {
                       DataInputStream dis = (new DataInputStream(new ByteArrayInputStream(buffer, 0, length[0])));
                       dos.writeBytes(header() + ": queuelen: " + instance_id[0] + ": a(" + dis.readInt() + "), v(" + dis.readInt() + "), underflow(" + dis.readInt() + "), overflow(" + dis.readInt() + ")\n");
                     } else {
                       dos.writeBytes(header() + ": queuelen: (invalid)\n");
                     }
                     
                     break;
                   }
                   
                   default:
                   {
                     dos.writeBytes(header() + ": nodecode: " + instance_id[0] + " (" + length[0] + " bytes)\n");
                     break;
                   }
                 }
                 
                 return null;
               } catch (Throwable e) {
                 throw (new RuntimeException(e));
               }
             }
           });
      } else {
        throw (new RuntimeException("bad usage"));
      }
      
      try {
        while (true) {
          length[0] = dis.readInt();
          
          // read instance id
          if ((length[0] < 8)) throw null;
          length[0] -= 8;
          instance_id[0] = dis.readLong();
          
          // read token
          if ((length[0] < 1)) throw null;
          length[0] -= 1;
          token[0] = dis.readByte();
          
          // read remainder
          if (!(length[0] < MAXIMUM_FEEDBACK_PACKET_LENGTH)) throw null;
          dis.readFully(buffer, 0, length[0]);
          
          synchronized (dos) {
            handler.invoke();
          }
        }
      } catch (EOFException e) {
        synchronized (dos) {
          dos.flush();
        }
      }
    } catch (Throwable e) {
      fatal(e);
    }
  }
}
