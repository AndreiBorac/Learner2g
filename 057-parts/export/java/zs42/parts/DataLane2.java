/***
 * DataLane2.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.security.*;

/***
 * DataLane2 is a basic distributed application framework, with
 * emphasis on replay debugging.
 ***/
public class DataLane2
{
  private static final int ONE_SECOND_MS = 1000;
  private static final int ONE_MINUTE_MS = (60 * ONE_SECOND_MS);
  private static final int ONE_HOUR_MS   = (60 * ONE_MINUTE_MS);
  
  private static final int SHAZ = 32;
  
  private static final String FILE_NONE = "/dev/null";

  private static final String SETTING_AUTH                                  = "datalane2.auth";
  private static final String SETTING_KMAC                                  = "datalane2.kmac";
  private static final String SETTING_UPSTREAM_PEERS                        = "datalane2.upstream.peers";
  private static final String SETTING_DOWNSTREAM_PORT                       = "datalane2.downstream.port";
  private static final String SETTING_TICKLE_INTERVAL_MS                    = "datalane2.tickle-interval-ms";
  private static final String SETTING_REQUEST_SERVER_PORT                   = "datalane2.request-server.port";
  private static final String SETTING_REQUEST_SERVER_READ_TIMEOUT           = "datalane2.request-server.read-timeout";
  private static final String SETTING_REQUEST_SERVER_MAXIMUM_REQUEST_LENGTH = "datalane2.request-server.maximum-request-length";
  private static final String SETTING_REQUEST_SERVER_REQUEST_TERMINATOR     = "datalane2.request-server.request-terminator";
  
  private static final NoUTF.Filter FILTER_TOKENIZE = (new NoUTF.Filter("\u0000\u0020//"));
  private static final NoUTF.Filter FILTER_TOKENIZE_FIELDS = (new NoUTF.Filter("\u0000\u0020::"));
  
  public static class Packet
  {
    final TreeMap<String, String> attrs = (new TreeMap<String, String>());
    final TreeMap<String, byte[]> items = (new TreeMap<String, byte[]>());
    
    public Packet()
    {
    }
    
    public void clear()
    {
      attrs.clear();
      items.clear();
    }
    
    public Packet setAttr(String key, String val)
    {
      attrs.put(key, val);
      
      return this;
    }
    
    public boolean hasAttr(String key)
    {
      return attrs.containsKey(key);
    }
    
    public String getAttr(String key)
    {
      String val = attrs.get(key);
      
      if (val == null) throw null;
      
      return val;
    }
    
    public Packet setItem(String key, byte[] val)
    {
      items.put(key, val);
      
      return this;
    }
    
    public boolean hasItem(String key)
    {
      return items.containsKey(key);
    }
    
    public byte[] getItem(String key)
    {
      byte[] val = items.get(key);
      
      if (val == null) throw null;
      
      return val;
    }
    
    public void writeTo(DataOutputStream out) throws IOException
    {
      // attrs
      {
        out.writeInt(attrs.size());
        
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
          SimpleIO.writeByteArrayString(out, entry.getKey());
          SimpleIO.writeByteArrayString(out, entry.getValue());
        }
      }
      
      // items
      {
        out.writeInt(items.size());
        
        for (Map.Entry<String, byte[]> entry : items.entrySet()) {
          SimpleIO.writeByteArrayString(out, entry.getKey());
          SimpleIO.writeByteArray(out, entry.getValue());
        }
      }
    }
    
    public static Packet readFrom(DataInputStream inp) throws IOException
    {
      Packet packet = (new Packet());
      
      // attrs
      {
        int attrc = inp.readInt();
        
        while (attrc-- > 0) {
          packet.attrs.put(SimpleIO.readByteArrayString(inp), SimpleIO.readByteArrayString(inp));
        }
      }
      
      // items
      {
        int itemc = inp.readInt();
        
        while (itemc-- > 0) {
          packet.items.put(SimpleIO.readByteArrayString(inp), SimpleIO.readByteArray(inp));
        }
      }
      
      return packet;
    }
  }
  
  public static abstract class Response
  {
    boolean dropConnection = false;
    
    public Response alsoDropConnection()
    {
      dropConnection = true;
      return this;
    }
    
    public abstract void writeTo(OutputStream out) throws Exception;
  }
  
  public static class NullResponse extends Response
  {
    public final void writeTo(OutputStream out)
    {
      // nothing to do
    }
  }
  
  public static class ByteArrayResponse extends Response
  {
    final byte[] buf;
    final int    off;
    final int    lim;
    
    public ByteArrayResponse(byte[] buf, int off, int lim)
    {
      this.buf = buf;
      this.off = off;
      this.lim = lim;
    }
    
    public ByteArrayResponse(byte[] buf)
    {
      this(buf, 0, buf.length);
    }
    
    public void writeTo(OutputStream out) throws Exception
    {
      out.write(buf, off, lim);
    }
  }
  
  public static abstract class Application
  {
    protected abstract Object doInitializeAndGetMonitor(Environment environment) throws Exception;
    protected abstract boolean isQuiescent() throws Exception;
    
    /* shared input */
    protected abstract void onBroadcast(ArrayList<Packet> gen, Packet inp) throws Exception;
    
    /* local input */
    protected abstract void onTickle(ArrayList<Packet> gen) throws Exception;
    
    /* local input */
    protected abstract Response onRequest(ArrayList<Packet> gen, String src, byte[] inp) throws Exception;
  }
  
  public static abstract class FeedbackApplication extends Application
  {
    protected abstract void onBroadcastLoopback(ArrayList<Packet> gen, Packet inp) throws Exception;
    protected abstract void onTickleLoopback(ArrayList<Packet> gen) throws Exception;
    protected abstract Response onRequestLoopback(ArrayList<Packet> gen, String src, byte[] inp) throws Exception;
    
    private final void loop(ArrayList<Packet> gen, int off) throws Exception
    {
      while (off < gen.size()) {
        onBroadcastLoopback(gen, gen.get(off++));
      }
    }
    
    protected final void onBroadcast(ArrayList<Packet> gen, Packet inp) throws Exception
    {
      int off = gen.size();
      
      onBroadcastLoopback(gen, inp);
      
      loop(gen, off);
    }
    
    protected final void onTickle(ArrayList<Packet> gen) throws Exception
    {
      int off = gen.size();
      
      onTickleLoopback(gen);
      
      loop(gen, off);
    }
    
    protected final Response onRequest(ArrayList<Packet> gen, String src, byte[] inp) throws Exception
    {
      int off = gen.size();
      
      Response ret = onRequestLoopback(gen, src, inp);
      
      loop(gen, off);
      
      return ret;
    }
  }
  
  public static abstract class ApplicationWrapper extends Application
  {
    final Application inner;
    
    public ApplicationWrapper(Application inner)
    {
      this.inner = inner;
    }
  }
  
  public static abstract class ReplayLoggingApplicationWrapper extends ApplicationWrapper
  {
    private static final int TYPE_BROADCAST = ('B' & 0xFF);
    private static final int TYPE_TICKLE    = ('T' & 0xFF);
    private static final int TYPE_REQUEST   = ('R' & 0xFF);
    
    private DataOutputStream out = null;
    
    private boolean previouslyQuiescent = true;
    
    public ReplayLoggingApplicationWrapper(Application inner)
    {
      super(inner);
    }
    
    protected abstract DataOutputStream logRotate() throws Exception;
    
    protected Object doInitializeAndGetMonitor(Environment environment) throws Exception
    {
      out = logRotate();
      
      return inner.doInitializeAndGetMonitor(environment);
    }
    
    protected boolean isQuiescent() throws Exception
    {
      final boolean quiescent = inner.isQuiescent();
      
      if (quiescent && !previouslyQuiescent) {
        if (out != null) {
          out.flush();
          out.close();
        }
        
        out = logRotate();
      }
      
      previouslyQuiescent = quiescent;
      
      return quiescent;
    }
    
    protected void onBroadcast(ArrayList<Packet> gen, Packet inp) throws Exception
    {
      out.writeByte(TYPE_BROADCAST);
      inp.writeTo(out);
      out.flush();
      
      inner.onBroadcast(gen, inp);
    }
    
    protected void onTickle(ArrayList<Packet> gen) throws Exception
    {
      out.writeByte(TYPE_TICKLE);
      out.flush();
      
      inner.onTickle(gen);
    }
    
    protected Response onRequest(ArrayList<Packet> gen, String src, byte[] inp) throws Exception
    {
      out.writeByte(TYPE_REQUEST);
      SimpleIO.writeByteArrayString(out, src);
      SimpleIO.writeByteArray(out, inp);
      out.flush();
      
      return inner.onRequest(gen, src, inp);
    }
    
    public static void replay(Application inner, DataInputStream inp) throws Exception
    {
      final Object monitor = inner.doInitializeAndGetMonitor(null);
      
      boolean eof_expected = false;
      
      try {
        while (true) {
          eof_expected = true;
          
          int type = (inp.readByte() & 0xFF);
          
          eof_expected = false;
          
          switch (type) {
          case TYPE_BROADCAST:
            {
              synchronized (monitor) {
                inner.onBroadcast((new ArrayList<Packet>()), Packet.readFrom(inp));
              }
              
              break;
            }
            
          case TYPE_TICKLE:
            {
              synchronized (monitor) {
                inner.onTickle((new ArrayList<Packet>()));
              }
              
              break;
            }
            
          case TYPE_REQUEST:
            {
              synchronized (monitor) {
                inner.onRequest((new ArrayList<Packet>()), SimpleIO.readByteArrayString(inp), SimpleIO.readByteArray(inp));
              }
              
              break;
            }
            
          default:
            {
              throw null;
            }
          }
        }
      } catch (EOFException e) {
        if (eof_expected) {
          // nothing to do
        } else {
          throw (new RuntimeException(e));
        }
      }
    }
  }
  
  public static class Environment
  {
    final byte[] auth;
    final byte[] kmac;
    
    final Application application;
    
    final ArrayList<Packet> history = (new ArrayList<Packet>());
    final ArrayList<LinkedBlockingQueue<Packet>> targets = (new ArrayList<LinkedBlockingQueue<Packet>>());
    
    boolean allowTruncation = false;
    
    Environment(byte[] auth, byte[] kmac, Application application)
    {
      this.auth = auth;
      this.kmac = kmac;
      
      this.application = application;
    }
    
    public String toString()
    {
      return ("DataLane2::Environment(history.size()=" + history.size() + ", targets.size()=" + targets.size() + ")");
    }
    
    final ThreadLocal<ByteArrayOutputStream> encodePacketBuffer = (new ThreadLocal<ByteArrayOutputStream>() { protected ByteArrayOutputStream initialValue() { return (new ByteArrayOutputStream()); } } );
    
    byte[] encodePacketToBytes(Packet packet) throws IOException
    {
      ByteArrayOutputStream tmp = encodePacketBuffer.get();
      
      tmp.reset();
      
      packet.writeTo((new DataOutputStream(tmp)));
      
      return tmp.toByteArray();
    }
    
    final ThreadLocal<MessageDigest> encodePacketDigest = (new ThreadLocal<MessageDigest>() { protected MessageDigest initialValue() { try { return MessageDigest.getInstance("SHA-256"); } catch (Throwable e) { throw Log.fatal(e); } } } );
    
    void encodePacketWithMac(Packet packet, DataOutputStream out) throws IOException
    {
      byte[] data = encodePacketToBytes(packet);
      
      MessageDigest md = encodePacketDigest.get();
      
      md.reset();
      md.update(kmac);
      byte[] calc = md.digest(data);
      
      out.write(calc);
      SimpleIO.writeByteArray(out, data);
    }
    
    Packet decodePacketWithMac(DataInputStream inp, boolean trusted) throws IOException
    {
      MessageDigest md = encodePacketDigest.get();
      
      byte[] csum = (new byte[md.getDigestLength()]);
      
      inp.readFully(csum);
      
      byte[] data = SimpleIO.readByteArray(inp);
      
      md.reset();
      md.update(kmac);
      byte[] calc = md.digest(data);
      
      if (!trusted) {
        for (int i = 0; i < calc.length; i++) {
          if (csum[i] != calc[i]) throw null;
        }
      }
      
      return Packet.readFrom((new DataInputStream((new ByteArrayInputStream(data)))));
    }
    
    LinkedBlockingQueue<Packet> newTarget()
    {
      synchronized (this) {
        LinkedBlockingQueue<Packet> target = (new LinkedBlockingQueue<Packet>());
        
        for (Packet packet : history) {
          target.add(packet);
        }
        
        targets.add(target);
        
        return target;
      }
    }
    
    void postPacketFromTarget(LinkedBlockingQueue<Packet> target_shadow, Packet packet)
    {
      try {
        synchronized (this) {
          history.add(packet);
          
          for (LinkedBlockingQueue<Packet> target : targets) {
            if (target != target_shadow) {
              target.put(packet);
            }
          }
        }
      } catch (InterruptedException e) {
        throw (new RuntimeException(e));
      }
    }
    
    void postPacketFromTargetAllClear(LinkedBlockingQueue<Packet> target_shadow, ArrayList<Packet> packets, boolean quiescent)
    {
      if (quiescent) {
        synchronized (this) {
          if (allowTruncation) {
            history.clear();
          }
        }
      }
      
      /*
        even if the application is quiescent, we must keep
        transmitting packets to existing peers ...
       */
      
      for (Packet packet : packets) {
        postPacketFromTarget(target_shadow, packet);
      }
      
      packets.clear();
    }
    
    void startRecvDaemon(final LinkedBlockingQueue<Packet> target, final DataInputStream inp)
    {
      Log.startLoggedThread
        ((new Log.LoggedThread()
          {
            protected void run() throws Exception
            {
              while (true) {
                Packet packet = decodePacketWithMac(inp, false);
                postPacketFromTarget(target, packet);
              }
            }
          }));
    }
    
    void startSendDaemon(final LinkedBlockingQueue<Packet> target, final DataOutputStream out)
    {
      Log.startLoggedThread
        ((new Log.LoggedThread()
          {
            protected void run() throws Exception
            {
              while (true) {
                Packet packet = target.take();
                encodePacketWithMac(packet, out);
                out.flush();
              }
            }
          }));
    }
    
    void startDaemons(Socket peer, boolean amServer) throws IOException
    {
      final LinkedBlockingQueue<Packet> target = newTarget();
      
      DataInputStream  inp = (new DataInputStream (new BufferedInputStream (peer.getInputStream ())));
      DataOutputStream out = (new DataOutputStream(new BufferedOutputStream(peer.getOutputStream())));
      
      if (amServer) {
        // check auth rather than sending data to anyone who connects
        {
          byte[] recv = (new byte[auth.length]);
          
          inp.readFully(recv);
          
          for (int i = 0; i < recv.length; i++) {
            if (recv[i] != auth[i]) throw null;
          }
        }
      } else {
        // send auth
        {
          out.write(auth);
          out.flush();
        }
      }
      
      startRecvDaemon(target, inp);
      startSendDaemon(target, out);
    }
    
    void run(final AsciiTreeMap<String> settings, final ArrayList<LinkedBlockingQueue<Packet>> injectors) throws Exception
    {
      final Object monitor;
      
      /* initialize (and get monitor ...) */
      {
        monitor = application.doInitializeAndGetMonitor(this);
      }
      
      /* networking - data lane - client side (upstream) */
      {
        String peer = settings.get(SETTING_UPSTREAM_PEERS);
        
        if (!(peer.equals(""))) {
          String[] pair = NoUTF.tokenize(peer, FILTER_TOKENIZE_FIELDS);
          
          if (!(pair.length == 2)) throw (new RuntimeException("DataLane2::run: illegal upstream peer specification '" + peer + "'"));
          
          final String host = pair[0];
          final int    port = Integer.parseInt(pair[1]);
          
          Log.startLoggedThread
            ((new Log.LoggedThread()
              {
                protected void run() throws Exception
                {
                  final Socket socket = (new Socket(host, port));
                  
                  startDaemons(socket, false);
                  
                  /*
                    now that we've created the upstream target, all
                    "announce" messages intended for upstream have been
                    saved, so we can allow truncating history on
                    quiescence beyond this point.
                  */
                  
                  allowTruncation = true;
                }
              }));
        } else {
          allowTruncation = true;
        }
      }
      
      /* networking - data lane - server side (downstream) */
      {
        final int port = Integer.parseInt(settings.get(SETTING_DOWNSTREAM_PORT));
        
        AbstractServer.launch
        (port,
         (new F1<Void, Socket>()
          {
            public Void invoke(Socket client)
            {
              try {
                startDaemons(client, true);
                
                /* we need to hang here; otherwise, AbstractServer would close the connection */
                {
                  while (true) {
                    Thread.sleep(ONE_HOUR_MS);
                  }
                }
              } catch (Throwable e) {
                throw Log.fatal(e);
              }
            }
          }));
      }
      
      final LinkedBlockingQueue<Packet> program = newTarget();
      
      /* tickler */
      {
        final int tickle_interval_ms = Integer.parseInt(settings.get(SETTING_TICKLE_INTERVAL_MS));
        
        Log.startLoggedThread
          ((new Log.LoggedThread()
            {
              protected void run() throws Exception
              {
                final ArrayList<Packet> gen = (new ArrayList<Packet>());
                
                while (true) {
                  boolean quiescent;
                  
                  synchronized (monitor) {
                    application.onTickle(gen);
                    quiescent = application.isQuiescent();
                  }
                  
                  postPacketFromTargetAllClear(program, gen, quiescent);
                  
                  Thread.sleep(tickle_interval_ms);
                }
              }
            }));
      }
      
      /* networking - request server */
      {
        final int port = Integer.parseInt(settings.get(SETTING_REQUEST_SERVER_PORT));
        final int read_timeout = Integer.parseInt(settings.get(SETTING_REQUEST_SERVER_READ_TIMEOUT));
        final int maximum_request_length = Integer.parseInt(settings.get(SETTING_REQUEST_SERVER_MAXIMUM_REQUEST_LENGTH));
        
        final byte[] request_terminator;
        
        {
          ArrayList<Byte> bytes = (new ArrayList<Byte>());
          
          for (String bytei : NoUTF.tokenize(settings.get(SETTING_REQUEST_SERVER_REQUEST_TERMINATOR), FILTER_TOKENIZE)) {
            int bytev = Integer.parseInt(bytei);
            
            if (!((-128 <= bytev) && (bytev < 256))) throw (new RuntimeException("DataLane2::run: illegal byte value '" + bytev + "' in request terminator specification"));
            
            bytes.add(((byte)(bytev)));
          }
          
          request_terminator = (new byte[bytes.size()]);
          
          for (int i = 0; i < request_terminator.length; i++) {
            request_terminator[i] = bytes.get(i);
          }
        }
        
        AbstractServer.launch
        (port,
         (new F1<Void, Socket>()
          {
            public Void invoke(Socket client)
            {
              try {
                client.setSoTimeout(read_timeout);
                
                final String src = ((InetSocketAddress)(client.getRemoteSocketAddress())).getAddress().getHostAddress();
                
                InputStream  inp = client.getInputStream();
                OutputStream out = client.getOutputStream();
                
                boolean ignore_input = false;
                
                final byte[] buf = (new byte[maximum_request_length]);
                int          off = 0;
                int          lim = 0;
                
                final ArrayList<Packet> gen = (new ArrayList<Packet>());
                
                outer:
                while (true) {
                  // process
                  {
                    // handle available requests
                    {
                      if (ignore_input) {
                        off = lim;
                      } else {
                        int bot;
                        
                        while ((bot = PartsUtils.locate(buf, off, lim, request_terminator)) != -1) {
                          byte[] req = PartsUtils.extract(buf, off, lim);
                          Response res;
                          
                          boolean quiescent;
                          
                          synchronized (monitor) {
                            res = application.onRequest(gen, src, req);
                            quiescent = application.isQuiescent();
                          }
                          
                          postPacketFromTargetAllClear(program, gen, quiescent);
                          
                          res.writeTo(out);
                          out.flush();
                          
                          if (res.dropConnection) {
                            client.shutdownOutput();
                            ignore_input = true;
                          }
                          
                          off = bot + request_terminator.length;
                        }
                      }
                    }
                    
                    // repage
                    {
                      int bot = lim;
                      
                      for (lim = 0; off < bot; off++) {
                        buf[lim++] = buf[off];
                      }
                      
                      off = 0;
                    }
                  }
                  
                  // read
                  {
                    if (lim < buf.length) {
                      int amt = inp.read(buf, lim, (buf.length - lim));
                      
                      if (amt <= 0) {
                        throw (new RuntimeException("encountered EOF"));
                      }
                      
                      lim += amt;
                    } else {
                      throw (new RuntimeException("request too long"));
                    }
                  }
                }
              } catch (Throwable e) {
                /*
                  here we don't obey the usual fail-fast principle;
                  instead, we hope that the application state is
                  intact and other requests can still be serviced
                  without problems.
                 */
                Log.log(e);
              }
              
              try {
                /*
                  closing the client socket will be attempted by
                  AbstractServer also, but we want log errors and the
                  AbstractServer code doesn't. so we close it here.
                 */
                client.close();
              } catch (Throwable e) {
                Log.log(e);
              }
              
              return null;
            }
          }));
      }
      
      /* injectors */
      {
        for (final LinkedBlockingQueue<Packet> injector : injectors) {
          final LinkedBlockingQueue<Packet> target = newTarget();
          
          Log.startLoggedThread
            ((new Log.LoggedThread()
              {
                public void run() throws Exception
                {
                  while (true) {
                    postPacketFromTarget(target, injector.take());
                  }
                }
              }));
        }
      }
      
      /* loop - dequeue shared packets from other nodes */
      {
        final ArrayList<Packet> gen = (new ArrayList<Packet>());
        
        while (true) {
          Packet packet = program.take();
          
          boolean quiescent;
          
          synchronized (monitor) {
            application.onBroadcast(gen, packet);
            quiescent = application.isQuiescent();
          }
          
          postPacketFromTargetAllClear(program, gen, quiescent);
        }
      }
    }
  }
  
  public static void run(final AsciiTreeMap<String> settings, final Application application, final ArrayList<LinkedBlockingQueue<Packet>> injectors)
  {
    try {
      final byte[] auth = HexStr.hex2bin(settings.get(SETTING_AUTH));
      final byte[] kmac = HexStr.hex2bin(settings.get(SETTING_KMAC));
      
      for (byte[] shaz : (new byte[][] { auth, kmac })) {
        if (!(shaz.length == SHAZ)) throw (new RuntimeException("DataLane2::run: illegal auth/kmac specification (" + shaz.length + " bytes instead of the expected " + SHAZ + ")"));
      }
      
      (new Environment(auth, kmac, application)).run(settings, injectors);
    } catch (Throwable e) {
      throw Log.fatal(e);
    }
  }
}
