/***
 * NetworkConnection.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.learner2g.student2g;

import zs42.learner2g.lantern2g.ny4j.bind.*;

import zs42.ny4j.*;

import zs42.parts.Nothing;
import zs42.parts.Blazon;
import zs42.parts.Combo;
import zs42.parts.Log;
import zs42.parts.NoUTF;
import zs42.parts.HexStr;

import java.io.*;
import java.net.*;
import java.util.*;

public class NetworkConnection
{
  static abstract class AbstractNetworkEndpoint
  {
    final NetworkEndpointYield yield_network_endpoint;
    
    AbstractNetworkEndpoint(NetworkEndpointYield yield_network_endpoint)
    {
      this.yield_network_endpoint = yield_network_endpoint;
    }
    
    abstract void onInitialize(final String[] origin, final JNylus.Linkage linkage_incoming, final JNylus.Linkage linkage_outgoing, final NetworkIncomingYield yield_network_incoming, final NetworkOutgoingYield yield_network_outgoing);
    abstract void onClose();
  }
  
  static class EphemeralNetworkEndpoint extends AbstractNetworkEndpoint
  {
    Socket sok = null;
    
    InputStream  inp = null;
    OutputStream out = null;
    
    boolean eof_inp = false;
    
    boolean err_inp = false;
    boolean err_out = false;
    
    EphemeralNetworkEndpoint(NetworkEndpointYield yield_network_endpoint)
    {
      super(yield_network_endpoint);
    }
    
    private void drop()
    {
      eof_inp = true;
      err_inp = true;
      err_out = true;
    }
    
    void onInitialize(final String[] origin, final JNylus.Linkage linkage_incoming, final JNylus.Linkage linkage_outgoing, final NetworkIncomingYield yield_network_incoming, final NetworkOutgoingYield yield_network_outgoing)
    {
      boolean success = false;
      
      /****/ if (origin[1].equals("TCP4")) {
        try {
          String host = origin[2];
          int    port = Integer.parseInt(origin[3]);
          
          sok = (new Socket(host, port));
          
          inp = sok.getInputStream();
          out = sok.getOutputStream();
          
          success = true;
        } catch (IOException e) {
          drop();
        }
      } else if (origin[1].equals("HTTP")) {
        try {
          String host = origin[2];
          String path = origin[3];
          
          String url = "http://" + host + path;
          HttpURLConnection connection = ((HttpURLConnection)((new URL(url)).openConnection()));
          connection.setRequestMethod("GET");
          connection.setDoInput(true);
          connection.connect();
          
          inp = connection.getInputStream();
          
          err_out = true; // sending not supported through HTTP
          
          success = true;
        } catch (IOException e) {
          drop();
        }
      } else {
        drop();
      }
      
      NetworkIncomingAgent agent_network_incoming;
      NetworkOutgoingAgent agent_network_outgoing;
      
      if (success) {
        agent_network_incoming =
          (new NetworkIncomingAgent(linkage_incoming)
            {
              protected void onShutdown()
              {
                yield_network_incoming.doShutdown();
                
                linkage_incoming.station_callee.setTerminated();
              }
              
              protected void onShutdownIncoming()
              {
                if (sok != null) {
                  try {
                    sok.shutdownInput();
                  } catch (IOException e) {
                    Log.log(e); // but otherwise ignore
                  }
                }
                
                yield_network_incoming.doShutdownIncoming();
              }
              
              protected void onRecv(byte[] buf, int off, int lim)
              {
                int amt = 0;
                
                if (!eof_inp) {
                  try {
                    int len = (lim - off);
                    if (len <= 0) throw null;
                    amt = inp.read(buf, off, len);
                    if (amt <= 0) {
                      eof_inp = true;
                      amt = 0;
                    }
                  } catch (IOException e) {
                    err_inp = true;
                    eof_inp = true;
                    amt = 0;
                  }
                }
                
                yield_network_incoming.doRecv(buf, off, lim, amt);
              }
            });
        
        agent_network_outgoing =
          (new NetworkOutgoingAgent(linkage_outgoing)
            {
              protected void onShutdown()
              {
                yield_network_outgoing.doShutdown();
                
                linkage_outgoing.station_callee.setTerminated();
              }
              
              protected void onShutdownOutgoing()
              {
                if (sok != null) {
                  try {
                    sok.shutdownOutput();
                  } catch (IOException e) {
                    Log.log(e); // but otherwise ignore
                  }
                }
              }
              
              protected void onSend(byte[] buf, int off, int lim)
              {
                int amt = 0;
                
                if (!err_out) {
                  try {
                    int len = (lim - off);
                    if (len <= 0) throw null;
                    out.write(buf, off, lim);
                    amt = (lim - off);
                  } catch (IOException e) {
                    err_out = true;
                    amt = 0;
                  }
                }
                
                yield_network_outgoing.doSend(buf, off, lim, amt);
              }
            });
      } else {
        agent_network_incoming = null;
        agent_network_outgoing = null;
      }
      
      yield_network_endpoint.doInitialize(origin, linkage_incoming, linkage_outgoing, yield_network_incoming, yield_network_outgoing, success, agent_network_incoming, agent_network_outgoing);
    }
    
    void onClose()
    {
      if (sok != null) {
        try {
          sok.close();
        } catch (IOException e) {
          Log.log(e); // but otherwise ignore
        }
      }
      
      yield_network_endpoint.doClose();
    }
  }
  
  static class PersistentNetworkEndpoint extends AbstractNetworkEndpoint
  {
    private static final int timeout_ms = 5000; /* 5 sec */
    private static final int retry_ns = 1000000000; /* 1 sec */
    private static final int sleep_ms = 270; /* 0.27 sec */
    
    PersistentNetworkEndpoint(NetworkEndpointYield yield_network_endpoint)
    {
      super(yield_network_endpoint);
    }
    
    void onInitialize(final String[] origin, final JNylus.Linkage linkage_incoming, final JNylus.Linkage linkage_outgoing, final NetworkIncomingYield yield_network_incoming, final NetworkOutgoingYield yield_network_outgoing)
    {
      if (!(origin[1].equals("PSDO"))) throw null;
      
      final String host = origin[2];
      final String path = origin[3];
      final String name = origin[4];
      final String auth = origin[5];
      
      Log.log("PersistentNetworkEndpoint::onInitialize: host='" + host + "', path='" + path + "', name='" + name + "', auth='" + auth + "'");
      
      NetworkIncomingAgent agent_network_incoming =
        (new NetworkIncomingAgent(linkage_incoming)
          {
            long deadzone = (System.nanoTime() - retry_ns); /* set initial deadzone in the past */
            boolean estoppel = false;
            String marker = "ENTER";
            int serial = 0;
            
            protected void onShutdown()
            {
              yield_network_incoming.doShutdown();
              
              linkage_incoming.station_callee.setTerminated();
            }
            
            protected void onShutdownIncoming()
            {
              estoppel = true;
            }
            
            protected void onRecv(byte[] buf, int off, int lim)
            {
              while (true) {
                // check for estoppel
                {
                  if (estoppel) {
                    yield_network_incoming.doRecv(buf, off, lim, 0);
                    return;
                  }
                }
                
                long now;
                
                // wait to clear deadzone (to avoid issuing requests too frequently)
                // also set the next deadzone, since we're going to attempt a request below
                {
                  while (((now = System.nanoTime()) - deadzone) < 0) {
                    try {
                      Thread.sleep(sleep_ms);
                    } catch (InterruptedException e) {
                      throw (new RuntimeException(e));
                    }
                  }
                  
                  deadzone = (now + retry_ns);
                }
                
                int exp = 0;
                InputStream inp = null;
                
                try {
                  // connect
                  {
                    String url = ("http://" + host + path + "pull/" + name + "/" + auth + "/" + marker + "/" + (lim - off) + "/" + Math.max(0, Math.abs(now)));
                    
                    HttpURLConnection con = ((HttpURLConnection)((new URL(url)).openConnection()));
                    
                    con.setRequestMethod("GET");
                    con.setDoInput(true);
                    
                    con.setConnectTimeout(timeout_ms);
                    con.setReadTimeout(timeout_ms);
                    
                    con.connect();
                    
                    exp = con.getContentLength();
                    inp = con.getInputStream();
                  }
                  
                  int amt;
                  
                  // read response body
                  {
                    int pos = off;
                    
                    {
                      int rci;
                      
                      while ((pos < lim) && ((rci = inp.read(buf, pos, (lim - pos))) > 0)) pos += rci;
                      
                      // close the input stream
                      {
                        InputStream hup = inp;
                        inp = null;
                        hup.close();
                      }
                    }
                    
                    amt = (pos - off);
                  }
                  
                  // check for truncated or extended response (the former is bad, the latter is truncated)
                  {
                    if (amt < exp) throw (new IOException("truncated response; received " + amt + " < " + exp + " bytes"));
                    if (amt > exp) { Log.log("extended response; received " + amt + " > " + exp + " bytes"); amt = exp; }
                  }
                  
                  int next_marker_length;
                  
                  // extract next marker length (last byte)
                  {
                    if (amt < 1) throw null;
                    next_marker_length = (((int)(buf[off + amt - 1])) & 0xFF);
                    amt -= 1;
                  }
                  
                  // extract next marker (trailing bytes)
                  {
                    if (amt < next_marker_length) throw null;
                    
                    {
                      StringBuilder next_marker = (new StringBuilder());
                      
                      for (int pos = (off + amt - next_marker_length); pos < (off + amt); pos++) {
                        next_marker.append(((char)(((int)(buf[pos])) & 0xFF)));
                      }
                      
                      marker = next_marker.toString();
                      
                      // check for leave marker (signal that the stream ended and we shouldn't make more HTTP requests)
                      {
                        if (marker.equals("LEAVE")) {
                          estoppel = true;
                        }
                      }
                    }
                    
                    amt -= next_marker_length;
                  }
                  
                  // if we got something, report and exit
                  {
                    if (amt > 0) {
                      yield_network_incoming.doRecv(buf, off, lim, amt);
                      break;
                    }
                  }
                } catch (Throwable e1) {
                  try {
                    if (inp != null) {
                      InputStream hup = inp;
                      inp = null;
                      hup.close();
                    }
                  } catch (Throwable e2) {
                    Log.log(e2);
                  }
                  
                  Log.log(e1);
                }
                
                // we didn't get anything; continue loop
              }
            }
          });

      NetworkOutgoingAgent agent_network_outgoing =
        (new NetworkOutgoingAgent(linkage_outgoing)
          {
            protected void onShutdown()
            {
              yield_network_outgoing.doShutdown();
              
              linkage_outgoing.station_callee.setTerminated();
            }
            
            protected void onShutdownOutgoing()
            {
              // nothing to do
            }
            
            protected void onSend(byte[] buf, int off, int lim)
            {
              if ((off != 0) && (lim != buf.length)) throw null;
              
              //Log.log("onSend(buf=" + buf + "='" + NoUTF.bin2str(buf) + "')");
              
              long deadzone = (System.nanoTime() - retry_ns); /* set initial deadzone in the past */
              
              while (true) {
                long now;
                
                // wait to clear deadzone (to avoid issuing requests too frequently)
                // also set the next deadzone, since we're going to attempt a request below
                {
                  while (((now = System.nanoTime()) - deadzone) < 0) {
                    try {
                      Thread.sleep(sleep_ms);
                    } catch (InterruptedException e) {
                      throw (new RuntimeException(e));
                    }
                  }
                  
                  deadzone = (now + retry_ns);
                }
                
                int exp = 0;
                InputStream inp = null;
                
                try {
                  String url = ("http://" + host + path + "push-mesg/" + name + "/" + auth + "/" + HexStr.bin2hex(buf) + "/" + Math.max(0, Math.abs(now)));
                  
                  HttpURLConnection con = ((HttpURLConnection)((new URL(url)).openConnection()));
                  
                  con.setRequestMethod("GET");
                  con.setDoInput(true);
                  con.connect();
                  
                  exp = con.getContentLength();
                  inp = con.getInputStream();
                  
                  byte[] content = (new byte[exp]);
                  
                  // read fully
                  {
                    int pos = 0;
                    int amt;
                    
                    while ((pos < content.length) && ((amt = inp.read(content, pos, (content.length - pos))) > 0)) pos += amt;
                    if (pos != content.length) throw (new EOFException());
                  }
                  
                  InputStream hup = inp;
                  inp = null;
                  hup.close();
                  
                  String status = NoUTF.bin2str(content).trim();
                  
                  //Log.log("onSend(buf=" + buf + ") got status '" + status + "'");
                  
                  if ((status.equals("ACCEPTED") || status.equals("OUTDATED"))) {
                    yield_network_outgoing.doSend(buf, off, lim, (lim - off));
                    //Log.log("onSend(buf=" + buf + ") completing");
                    return;
                  }
                } catch (Throwable e1) {
                  try {
                    if (inp != null) {
                      InputStream hup = inp;
                      inp = null;
                      hup.close();
                    }
                  } catch (Throwable e2) {
                    Log.log(e2);
                  }
                  
                  Log.log(e1);
                }
              }
            }
          });
      
      yield_network_endpoint.doInitialize(origin, linkage_incoming, linkage_outgoing, yield_network_incoming, yield_network_outgoing, true, agent_network_incoming, agent_network_outgoing);
    }
    
    void onClose()
    {
      // no action required (assuming shutdown on each incoming/outgoing agent was invoked)
      yield_network_endpoint.doClose();
    }
  }
  
  public static NetworkEndpointAgent launch(final JNylus.Linkage linkage, final NetworkEndpointYield yield_network_endpoint)
  {
    return
      (new NetworkEndpointAgent(linkage)
        {
          protected AbstractNetworkEndpoint inner;
          
          protected void onShutdown()
          {
            yield_network_endpoint.doShutdown();
            
            linkage.station_callee.setTerminated();
          }
          
          protected void onInitialize(final String[] origin, final JNylus.Linkage linkage_incoming, final JNylus.Linkage linkage_outgoing, final NetworkIncomingYield yield_network_incoming, final NetworkOutgoingYield yield_network_outgoing)
          {
            /****/ if (origin[0].equals("E")) {
              inner = (new EphemeralNetworkEndpoint(yield_network_endpoint));
            } else if (origin[0].equals("P")) {
              inner = (new PersistentNetworkEndpoint(yield_network_endpoint));
            } else {
              throw null;
            }
            
            inner.onInitialize(origin, linkage_incoming, linkage_outgoing, yield_network_incoming, yield_network_outgoing);
          }
          
          protected void onClose()
          {
            if (inner != null) {
              inner.onClose();
              inner = null;
            }
          }
        });
  }
}
