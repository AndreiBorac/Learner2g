/***
 * Deliver2g.java
 * copyright (c) 2012 by andrei borac
 ***/

/***
 * Deliver2g is a full-out replacement for Station2g. It utilizes
 * DataLane for an el-cheapo distributed programming environment.
 ***/

/***
 * OLD:
 * 
 * Deliver2g is a cache for answering HTTP requests intended for an
 * "upstream" Station2g server. Some requests are passed through while
 * others are answered based on cached data. Data in the cache is
 * updated periodically based on a timeout. Updates are through a
 * special HTTP request with unlimited response length called a
 * "cache-update". Deliver2g caches do themselves support
 * "cache-update" for clients, allowing Deliver2g caches to serve as
 * upstream servers, enabling networks of arbitrary depths with
 * limited fan-out.
 * 
 * On startup, Deliver2g caches send a special HTTP request called a
 * "cache-notify" that instructs the root server that it may redirect
 * requests to it. It is intended that the root server answer requests
 * until the first cache-notify is received, after which it should
 * redirect clients to a random (or round-robin selected) cache rather
 * than answering any request.
 ***/

package zs42.learner2g.deliver2g;

import zs42.learner2g.groupir2g.*;

import zs42.buff.*;

import zs42.parts.*;

import zs42.splitter.common.*;

import java.io.*;
import java.nio.charset.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.security.*;

import java.lang.management.*;

import static zs42.parts.DataLane2.Packet;

public class Deliver2g
{
  private static final int SHAZ = (256 / 8);
  
  private static final long ONE_SECOND_NS = 1000000000L;
  
  private static final long KEEP_ALIVE_NS = (10 * ONE_SECOND_NS);
  
  private static final NoUTF.Filter FILTER_TOKENIZE        = (new NoUTF.Filter("\u0000\u0020//"));
  private static final NoUTF.Filter FILTER_TOKENIZE_FIELDS = (new NoUTF.Filter("\u0000\u0020::"));
  
  private static final byte[] NEWLINE_BYTES = (new byte[] { ((byte)('\n')) });
  
  /***
   * GATEKEEPER -- loads immutables.
   ***/
  
  static class Resource
  {
    final String name;
    final String mime;
    final byte[] data;
    
    Resource(String name, String mime, byte[] data)
    {
      this.name = name;
      this.mime = mime;
      this.data = data;
    }
    
    static Resource fromSettings(final AsciiTreeMap<String> settings, String name, String prefix)
    {
      String mime = settings.get((prefix + ".mime"));
      byte[] data = SimpleIO.slurp(settings.get((prefix + ".file")));
      
      return (new Resource(name, mime, data));
    }
  }
  
  static class Channel
  {
    final String name;
    final String organization_name;
    
    final int pxH;
    final int pxW;
    
    final AsciiTreeMap<Boolean> teacher_credentials;
    final AsciiTreeMap<String>  student_credentials;
    final AsciiTreeMap<Boolean> rewatch_credentials;
    
    final AsciiTreeMap<ArrayList<Long>> recordings;
    
    Channel(String name, String organization_name, int pxH, int pxW, AsciiTreeMap<Boolean> teacher_credentials, AsciiTreeMap<String> student_credentials, AsciiTreeMap<Boolean> rewatch_credentials, AsciiTreeMap<ArrayList<Long>> recordings)
    {
      this.name = name;
      this.organization_name = organization_name;
      
      this.pxH = pxH;
      this.pxW = pxW;
      
      this.teacher_credentials = teacher_credentials;
      this.student_credentials = student_credentials;
      this.rewatch_credentials = rewatch_credentials;
      
      this.recordings = recordings;
    }
    
    static Channel fromSettings(final AsciiTreeMap<String> settings, String name, String organization_name, String prefix, String recordings_directory)
    {
      final int H = Integer.parseInt(settings.get((prefix + ".screen.H")));
      final int W = Integer.parseInt(settings.get((prefix + ".screen.W")));
      
      final AsciiTreeMap<Boolean> teacher_credentials = (new AsciiTreeMap<Boolean>());
      final AsciiTreeMap<String>  student_credentials = (new AsciiTreeMap<String>());
      final AsciiTreeMap<Boolean> rewatch_credentials = (new AsciiTreeMap<Boolean>());
      
      final AsciiTreeMap<ArrayList<Long>> recordings = (new AsciiTreeMap<ArrayList<Long>>());
      
      for (String teacher_credential : NoUTF.tokenize(settings.get((prefix + ".teacher-credentials")), FILTER_TOKENIZE)) {
        teacher_credentials.put(teacher_credential, true);
      }
      
      {
        int id_unknown = 1;
        
        for (String student_credential : NoUTF.tokenize(settings.get((prefix + ".student-credentials")), FILTER_TOKENIZE)) {
          String[] pair = NoUTF.tokenize(student_credential, FILTER_TOKENIZE_FIELDS);
          if (pair.length == 1) pair = (new String[] { pair[0], ("guest-" + (id_unknown++)) });
          if (!(pair.length == 2)) throw null;
          student_credentials.put(pair[0], pair[1]);
        }
        
        for (String rewatch_credential : NoUTF.tokenize(settings.get((prefix + ".rewatch-credentials")), FILTER_TOKENIZE)) {
          rewatch_credentials.put(rewatch_credential, true);
        }
      }
      
      {
        final String path_prefix = (recordings_directory + File.separator + name + File.separator);
        
        for (String recording : NoUTF.tokenize(settings.get((prefix + ".recordings")), FILTER_TOKENIZE)) {
          ArrayList<Long> lengths = (new ArrayList<Long>());
          
          int index = 0;
          
          File file;
          
          while ((file = (new File(path_prefix + recording + index + "i"))).isFile()) {
            lengths.add(file.length());
            index++;
          }
          
          recordings.put(recording, lengths);
        }
      }
      
      return (new Channel(name, organization_name, H, W, teacher_credentials, student_credentials, rewatch_credentials, recordings));
    }
  }
  
  static class Organization
  {
    final String name;
    final String support_link_href;
    final AsciiTreeMap<Channel> channels;
    
    Organization(String name, String support_link_href, AsciiTreeMap<Channel> channels)
    {
      this.name = name;
      this.support_link_href = support_link_href;
      this.channels = channels;
    }
    
    static Organization fromSettings(final AsciiTreeMap<String> settings, String name, String prefix, String recordings_directory)
    {
      final String support_link_href = settings.get((prefix + ".support-link-href"));
      final AsciiTreeMap<Channel> channels = (new AsciiTreeMap<Channel>());
      
      for (String channel : NoUTF.tokenize(settings.get((prefix + ".channels")), FILTER_TOKENIZE)) {
        channels.put(channel, Channel.fromSettings(settings, channel, name, (prefix + ".channel." + channel), recordings_directory));
      }
      
      return (new Organization(name, support_link_href, channels));
    }
  }
  
  static class Gatekeeper
  {
    final String src_test;
    
    final String host_http;
    final int port_http;
    final int port_splitter;
    final String archive_directory;
    final String recordings_directory;
    final String control_credential;
    
    final AsciiTreeMap<Resource> resources;
    final AsciiTreeMap<Organization> organizations;
    final AsciiTreeMap<Channel> student_credentials;
    
    Gatekeeper(String src_test, String host_http, int port_http, int port_splitter, String archive_directory, String recordings_directory, String control_credential, AsciiTreeMap<Resource> resources, AsciiTreeMap<Organization> organizations, AsciiTreeMap<Channel> student_credentials)
    {
      this.src_test = src_test;
      
      this.host_http = host_http;
      this.port_http = port_http;
      this.port_splitter = port_splitter;
      this.archive_directory = archive_directory;
      this.recordings_directory = recordings_directory;
      this.control_credential = control_credential;
      
      this.resources = resources;
      this.organizations = organizations;
      this.student_credentials = student_credentials;
    }
    
    static Gatekeeper fromSettings(final AsciiTreeMap<String> settings, String prefix)
    {
      final String src_test = settings.get((prefix + ".src-test"));
      
      final String host_http = settings.get((prefix + ".host-http"));
      final int port_http = Integer.parseInt(settings.get((prefix + ".port-http")));
      final int port_splitter = Integer.parseInt(settings.get((prefix + ".port-splitter")));
      final String archive_directory = settings.get((prefix + ".archive-directory"));
      final String recordings_directory = settings.get((prefix + ".recordings-directory"));
      final String control_credential = settings.get((prefix + ".control-credential"));
      
      final AsciiTreeMap<Resource> resources = (new AsciiTreeMap<Resource>());
      final AsciiTreeMap<Organization> organizations = (new AsciiTreeMap<Organization>());
      
      for (String resource : NoUTF.tokenize(settings.get((prefix + ".resources")), FILTER_TOKENIZE)) {
        resources.put(resource, Resource.fromSettings(settings, resource, (prefix + ".resource." + resource)));
      }
      
      for (String organization : NoUTF.tokenize(settings.get((prefix + ".organizations")), FILTER_TOKENIZE)) {
        organizations.put(organization, Organization.fromSettings(settings, organization, (prefix + ".organization." + organization), recordings_directory));
      }
      
      final AsciiTreeMap<Channel> student_credentials = (new AsciiTreeMap<Channel>());
      
      for (Organization organization : organizations.values()) {
        for (Channel channel : organization.channels.values()) {
          for (String student_credential : channel.student_credentials.keySet()) {
            student_credentials.put(student_credential, channel);
          }
        }
      }
      
      return (new Gatekeeper(src_test, host_http, port_http, port_splitter, archive_directory, recordings_directory, control_credential, resources, organizations, student_credentials));
    }
  }
  
  /***
   * DEFINITIONS -- common to splitter server and station application
   ***/
  
  static final String ATTR_PURPOSE           = "prps";
  static final String ATTR_SENDER            = "sndr";
  static final String ATTR_SESSION           = "sess";
  static final String ATTR_STREAM_IDENTIFIER = "idfy";
  
  static final String ATTR_ORGANIZATION = "orgz";
  static final String ATTR_CHANNEL      = "chnl";
  
  static final String ITEM_PAYLOAD = "pyld";
  
  static final String ENUM_PURPOSE_KEEP_ALIVE        = "kplv";
  static final String ENUM_PURPOSE_ANNOUNCE_SENDER   = "annc";
  static final String ENUM_PURPOSE_INITIATE_SESSION  = "ises";
  static final String ENUM_PURPOSE_KILL_ALL_SESSIONS = "kall";
  static final String ENUM_PURPOSE_GROUPIR_PAYLOAD   = "grpr";
  static final String ENUM_PURPOSE_FEEDBACK_PAYLOAD  = "fdbk";
  
  static final String ENUM_SESSION_UNKNOWN = "????";
  
  /***
   * SPLITTER SERVER -- handles splitter connections, writing
   * Packet objects to the injector.
   ***/
  
  static final AtomicInteger splitterConnectionCounter = (new AtomicInteger(0));
  
  static void handle_splitter_connection(byte[] global_teacher_kmac, LinkedBlockingQueue<Packet> injector, Socket client)
  {
    final long connectionId = splitterConnectionCounter.incrementAndGet();
    
    InputStream  inp = null;
    OutputStream out = null;
    
    try {
      final String src = ((InetSocketAddress)(client.getRemoteSocketAddress())).getAddress().getHostAddress();
      
      Log.log(("splitter connection from src='" + src + "'"));
      
      final BufferCentral central = (new BufferCentral(9));
      
      final ByteArrayOutputStream capture_encoding;
      final SplitterCommon.Groupir.ChecksumAssistant assistant;
      final SplitterCommon.Groupir.Packet splitter_common_packet;
      final SplitterCommon.Groupir.PacketOutputStream packet_output_stream;
      final SimpleDeque<Groupir2g.Packet> packet_queue;
      final Groupir2g.SteamRoller steam_roller;
      
      Buffer.xB buffer_tail;
      
      Log.log(("will expect KMAC '" + HexStr.bin2hex(global_teacher_kmac) + "'"));
      capture_encoding = (new ByteArrayOutputStream());
      assistant = (new SplitterCommon.Groupir.ChecksumAssistant.KMAC(global_teacher_kmac));
      splitter_common_packet = (new SplitterCommon.Groupir.Packet(central, false, false, false, false));
      packet_output_stream = (new SplitterCommon.Groupir.PacketOutputStream(central, (new SplitterCommon.Groupir.ChecksumAssistant.SHA2()), capture_encoding));
      packet_queue = (new SimpleDeque<Groupir2g.Packet>());
      steam_roller = (new Groupir2g.SteamRoller());
      
      buffer_tail = central.acquireB();
      
      inp = client.getInputStream();
      out = client.getOutputStream();
      
      final byte[] buf = (new byte[65536]);
      int amt;
      
      while (true) {
        Log.log(("on connectionId " + connectionId + " resuming reading at " + System.nanoTime()));
        if (!((amt = inp.read(buf, 0, buf.length)) > 0)) break;
        Log.log(("on connectionId " + connectionId + " got " + amt + " bytes " + System.nanoTime()));
        
        // append obtained bytes to tail buffer
        {
          Buffer.nB buffer_tail_n = buffer_tail.append();
          Buffer.sB.copy(buffer_tail_n, buf, 0, amt);
          buffer_tail_n.release();
        }
        
        // process tail buffer
        buffer_tail = Groupir2g.decode(central, assistant, buffer_tail, packet_queue, steam_roller);
        steam_roller.release();
        
        while (!(packet_queue.isEmpty())) {
          // populate packet fields from groupir packet
          {
            Groupir2g.Packet groupir_packet = packet_queue.removeFirst();
            
            Log.log(("station splitter got packet with dJ='" + groupir_packet.dJ + "', dI='" + groupir_packet.dI + "', dS='" + groupir_packet.dS + "', dB='" + groupir_packet.dB + "'"));
            
            Buffer.oI oI = groupir_packet.dI.iterate();
            
            {
              int source_tc_hi = oI.rI();
              int source_tc_lo = oI.rI();
              //packet.ustc = ((((long)(source_tc_hi)) << 32) | (((long)(source_tc_lo)) & 0x00000000FFFFFFFFL)); // NOT INTERESTED
            }
            
            Groupir2g.StreamIdentifier streamIdentifier = null;
            byte[] captureEncoding;
            
            {
              int stream_id = oI.rI();
              
              switch (stream_id) {
              case 0: streamIdentifier = Groupir2g.StreamIdentifier.COMMAND;               break;
              case 1: streamIdentifier = Groupir2g.StreamIdentifier.AUDIO;                 break;
              case 6: streamIdentifier = Groupir2g.StreamIdentifier.NWED_EVENT;            break;
              case 7: streamIdentifier = Groupir2g.StreamIdentifier.ETCH_JARFILE_FRAGMENT; break;
              case 8: streamIdentifier = Groupir2g.StreamIdentifier.ETCH_EVENT_BUNDLE;     break;
              case 9: streamIdentifier = Groupir2g.StreamIdentifier.FROB_CODE;             break;
              default: /* ignore the packet (below) */
              }
              
              if (streamIdentifier == null) {
                Log.log(("ignored packet with strange stream identifier " + stream_id)); // this was commented earlier -- perhaps it is harmless in some cases (???) dubious ...
                oI.release();
                continue; // ignore the packet (WARNING: THIS IS PROBABLY A MEMORY LEAK)
              }
            }
            
            if (oI.remaining() > 0) {
              //packet.versatile = oI.rI(); // NOT INTERESTED
            }
            
            oI.release();
            
            // get standard encoding, and then RELEASE THE PACKET BUFFERS
            {
              splitter_common_packet.bufJ = groupir_packet.dJ;
              splitter_common_packet.bufI = groupir_packet.dI;
              splitter_common_packet.bufS = groupir_packet.dS;
              splitter_common_packet.bufB = groupir_packet.dB;
              
              splitter_common_packet.send(packet_output_stream, true);
              
              splitter_common_packet.release();
              
              captureEncoding = capture_encoding.toByteArray();
              
              capture_encoding.reset();
            }
            
            Log.log(("adding packet with streamIdentifier='" + streamIdentifier + "' and encoding.length='" + captureEncoding.length + "'"));
            
            // inject packet
            /*
              note that there is no way to know what session the
              packet is intended for at this point, so the system as a
              whole currently supports only one session at a time,
              even if other parts of the code seem to support multiple
              sessions.
             */
            {
              Packet packet = (new Packet());
              
              packet.setAttr(ATTR_PURPOSE, ENUM_PURPOSE_GROUPIR_PAYLOAD);
              packet.setAttr(ATTR_SESSION, ENUM_SESSION_UNKNOWN);
              packet.setAttr(ATTR_STREAM_IDENTIFIER, ("" + streamIdentifier.ordinal()));
              packet.setItem(ITEM_PAYLOAD, captureEncoding);
              
              injector.put(packet);
            }
          }
        }
      }
    } catch (Throwable e) {
      Log.log(e);
    }
    
    try {
      /*
        closing the client socket will be attempted by AbstractServer
        also, but we want log errors and the AbstractServer code
        doesn't. so we close it here.
      */
      client.close();
    } catch (Throwable e) {
      Log.log(e);
    }
  }
  
  /***
   * STATION APPLICATION -- handles HTTP connections and distributed
   * chatter.
   ***/
  
  static class StationApplication extends DataLane2.FeedbackApplication
  {
    final Gatekeeper gatekeeper;
    final boolean amRoot;
    final byte[] global_teacher_kmac;
    
    StationApplication(Gatekeeper gatekeeper, boolean amRoot, byte[] global_teacher_kmac)
    {
      this.gatekeeper = gatekeeper;
      this.amRoot = amRoot;
      this.global_teacher_kmac = global_teacher_kmac;
    }
    
    /***
     * enter state variables
     ***/
    
    final SecureRandom secureRandom = (new SecureRandom());
    
    byte[] newSecureRandomBytes(int count)
    {
      byte[] out = (new byte[count]);
      secureRandom.nextBytes(out);
      return out;
    }
    
    // true when forwarding is "locked" -- do not forward clients except those matching a test machine IP
    boolean forwardingLocked = true;
    
    // contains announces we've seen from non-root server (only non-root servers send announces)
    final ArrayList<String> announcedPeers = (new ArrayList<String>());
    
    // maps peer names to number of redirects
    final HashMap<String, Integer> peerUsage = (new HashMap<String, Integer>());
    
    static String formSessionIdentifier(Channel channel)
    {
      return (channel.organization_name + "." + channel.name);
    }
    
    class Session
    {
      static final int CODE_LENGTH_BYTES = 8;
      
      final Channel channel;
      
      final String sessioni;
      final String announce;
      
      final byte[] teacher_kmac;
      
      final String teacher_authenticator;
      final AsciiTreeMap<Boolean> student_authenticators = (new AsciiTreeMap<Boolean>());
      
      final ArrayList<Packet> packets = (new ArrayList<Packet>());
      
      final ArrayList<byte[]> messages = (new ArrayList<byte[]>());
      
      String newAuthenticator()
      {
        return HexStr.bin2hex(newSecureRandomBytes(CODE_LENGTH_BYTES));
      }
      
      Session(Channel channel_shadow)
      {
        channel = channel_shadow;
        
        sessioni = formSessionIdentifier(channel);
        announce = (sessioni + "/" + System.nanoTime());
        
        teacher_kmac = newSecureRandomBytes(SplitterCommon.Groupir.ChecksumAssistant.KMAC.SHAZ);
        
        teacher_authenticator = newAuthenticator();
        
        Log.log(("session " + announce + " created with teacher_kmac='" + HexStr.bin2hex(teacher_kmac) + "', teacher_authenticator='" + teacher_authenticator + "'"));
      }
      
      String newStudentAuthenticator(String src, String credential)
      {
        String authenticator = newAuthenticator();
        
        while (authenticator.equals(teacher_authenticator) || student_authenticators.containsKey(authenticator)) {
          authenticator = newAuthenticator();
        }
        
        student_authenticators.put(authenticator, Boolean.TRUE);
        
        Log.log(("session " + announce + " allocating authenticator " + authenticator + " to src='" + src + "' with credential='" + credential + "'"));
        
        return authenticator;
      }
      
      void start()
      {
        sessions.put(sessioni, this);
        recent_sessioni = sessioni;
        
        Log.startLoggedThread
          ((new Log.LoggedThread()
            {
              final int  WORK_MS = (10 * 1000); /* every 5 seconds */
              final long STROBE_NS = (10 * 1000000000L); /* every 10 seconds */
              final long TIMEOUT_NS = (5 * 60 * 1000000000L); /* 5 minutes */
              
              FileOutputStream fos_pkt = null;
              FileOutputStream fos_msg = null;
              
              int packeti = 0;
              int messagei = 0;
              
              SimpleDeque<byte[]> packet_writes  = (new SimpleDeque<byte[]>());
              SimpleDeque<byte[]> message_writes = (new SimpleDeque<byte[]>());
              
              long touch_ns = System.nanoTime();
              
              void init() throws IOException
              {
                String path = gatekeeper.archive_directory + File.separator + channel.organization_name + File.separator + channel.name;
                String base = path + File.separator + "utc" + System.currentTimeMillis() + "ms.";
                
                (new File(path)).mkdirs();
                
                fos_pkt = (new FileOutputStream(base + "pkt"));
                fos_msg = (new FileOutputStream(base + "msg"));
              }
              
              long last_strobe_ns = System.nanoTime();
              
              void strobe_maybe()
              {
                long now_ns = System.nanoTime();
                
                if ((now_ns - last_strobe_ns) > STROBE_NS) {
                  int total = 0;
                  
                  Log.log(("entering strobe @" + System.nanoTime()));
                  
                  synchronized (StationApplication.this) {
                    for (Packet packet : packets) {
                      total += packet.getAttr(ATTR_PURPOSE).length();
                      total += packet.getAttr(ATTR_SESSION).length();
                      
                      byte[] payload = packet.getItem(ITEM_PAYLOAD);
                      
                      if (payload.length > 0) {
                        for (int i = 0; i < payload.length; i += 4096) {
                          total += payload[i];
                        }
                        
                        total += payload[payload.length - 1];
                      }
                    }
                  }
                  
                  Log.log(("leaving strobe @" + System.nanoTime()));
                  
                  if (total == 1337) {
                    Log.log(("incredible strobe!"));
                  }
                  
                  last_strobe_ns = now_ns;
                }
              }
              
              boolean work() throws IOException
              {
                boolean alive;
                
                long now_ns = System.nanoTime();
                
                synchronized (StationApplication.this) {
                  alive = (sessions.get(sessioni) == Session.this);
                  
                  if (!alive) {
                    Log.log(("session not alive because replaced"));
                  }
                  
                  if (packeti < packets.size()) {
                    while (packeti < packets.size()) {
                      packet_writes.addLast(packets.get(packeti++).getItem(ITEM_PAYLOAD));
                    }
                    
                    touch_ns = now_ns;
                  }
                  
                  if (messagei < messages.size()) {
                    while (messagei < messages.size()) {
                      message_writes.addLast(messages.get(messagei++));
                    }
                    
                    /*
                      actually, arrival of messages should not prevent
                      the session from closing; hence, the line below
                      is now commented out
                     */
                    
                    //touch_ns = now_ns;
                  }
                }
                
                if (alive && ((now_ns - touch_ns) > TIMEOUT_NS)) {
                  alive = false;
                  
                  Log.log(("session not alive because timed out"));
                }
                
                if (!packet_writes.isEmpty()) {
                  while (!packet_writes.isEmpty()) {
                    fos_pkt.write(packet_writes.removeFirst());
                  }
                  
                  fos_pkt.flush();
                }
                
                if (!message_writes.isEmpty()) {
                  while (!message_writes.isEmpty()) {
                    fos_msg.write(message_writes.removeFirst());
                    fos_msg.write(NEWLINE_BYTES);
                  }
                  
                  fos_msg.flush();
                }
                
                strobe_maybe();
                
                if (!alive) {
                  try {
                    if (fos_pkt != null) {
                      FileOutputStream hup = fos_pkt;
                      fos_pkt = null;
                      hup.close();
                    }
                  } catch (Throwable e) {
                    Log.log(e);
                  }
                  
                  try {
                    if (fos_msg != null) {
                      FileOutputStream hup = fos_msg;
                      fos_msg = null;
                      hup.close();
                    }
                  } catch (Throwable e) {
                    Log.log(e);
                  }
                }
                
                return alive;
              }
              
              public void run()
              {
                try {
                  init();
                  
                  while (true) {
                    Log.log(("enter work @" + System.nanoTime()));
                    
                    if (!work()) {
                      break;
                    }
                    
                    Log.log(("leave work @" + System.nanoTime()));
                    
                    Thread.sleep(WORK_MS);
                  }
                } catch (Throwable e) {
                  Log.log(e);
                }
                
                // de-list session (careful ... only if the current mapping point to -this- session object)
                {
                  synchronized (StationApplication.this) {
                    if (sessions.get(sessioni) == Session.this) {
                      sessions.remove(sessioni);
                    }
                  }
                }
                
                Log.log(("session " + sessioni + " delisted"));
              }
            }));
      }
    }
    
    // key for sessions map is obtained by formSessionIdentifier(channel)
    final AsciiTreeMap<Session> sessions = (new AsciiTreeMap<Session>());
    
    // key most recently inserted into sessions map
    String recent_sessioni = "";
    
    /***
     * leave state variables
     ***/
    
    DataLane2.Environment environment = null;
    
    protected Object doInitializeAndGetMonitor(DataLane2.Environment environment) throws Exception
    {
      this.environment = environment;
      
      return this;
    }
    
    protected boolean isQuiescent() throws Exception
    {
      /*
        the question of quiescence: is it OK to clear the history at
        this point? would a joining node be OK starting from a blank
        slate without receiving previous events?
        
        well, any joining node is going to be non-root, so any state
        information in announcedPeers is irrelevant. all nodes need to
        know about sessions though ... so, basically, as long as the
        sessions map is empty the application is "quiescent" ...
      */
      
      return (sessions.size() == 0);
    }
    
    protected void onBroadcastLoopback(ArrayList<Packet> gen, Packet inp) throws Exception
    {
      String purpose = inp.getAttr(ATTR_PURPOSE).intern();
      
      /****/ if (purpose == ENUM_PURPOSE_KEEP_ALIVE) {
        // nothing to do
      } else if (purpose == ENUM_PURPOSE_ANNOUNCE_SENDER) {
        {
          String announcedPeer = inp.getAttr(ATTR_SENDER);
          
          announcedPeers.add(announcedPeer);
          peerUsage.put(announcedPeer, 0);
        }
      } else if (purpose == ENUM_PURPOSE_INITIATE_SESSION) {
        {
          String organization_name = inp.getAttr(ATTR_ORGANIZATION);
          String channel_name      = inp.getAttr(ATTR_CHANNEL);
          
          Log.log(("got broadcast session initiation organization_name='" + organization_name + "' channel_name='" + channel_name + "'"));
          
          Channel channel = gatekeeper.organizations.get(organization_name).channels.get(channel_name);
          
          /*
            N.B. each Deliver2g node will generate a different random
            one-time access code for the session. this does not matter
            for now, as clients are not allowed to hop between
            Deliver2g nodes.
           */
          
          Session session = (new Session(channel));
          session.start();
        }
      } else if (purpose == ENUM_PURPOSE_KILL_ALL_SESSIONS) {
        {
          sessions.clear();
        }
      } else if (purpose == ENUM_PURPOSE_GROUPIR_PAYLOAD) {
        {
          Session session;
          
          if ((session = sessions.get(recent_sessioni)) != null) {
            session.packets.add(inp);
          } else {
            Log.log(("ignoring groupir because session expired"));
          }
        }
      } else if (purpose == ENUM_PURPOSE_FEEDBACK_PAYLOAD) {
        {
          Session session;
          
          if ((session = sessions.get(inp.getAttr(ATTR_SESSION))) != null) {
            session.messages.add(inp.getItem(ITEM_PAYLOAD));
          } else {
            Log.log(("ignoring feedback because no current session"));
          }
        }
      } else {
        throw null;
      }
    }
    
    boolean first_tickle = true;
    long last_keep_alive = System.nanoTime();
    
    protected void onTickleLoopback(ArrayList<Packet> gen) throws Exception
    {
      // announce
      {
        if (first_tickle) {
          first_tickle = false;
          
          if (!amRoot) {
            Packet packet = (new Packet());
            
            packet.setAttr(ATTR_PURPOSE, ENUM_PURPOSE_ANNOUNCE_SENDER);
            packet.setAttr(ATTR_SENDER, gatekeeper.host_http);
            
            gen.add(packet);
          }
        }
      }
      
      // keep-alive
      {
        long now = System.nanoTime();
        
        if ((now - last_keep_alive) > KEEP_ALIVE_NS) {
          last_keep_alive = now;
          
          Packet packet = (new Packet());
          
          packet.setAttr(ATTR_PURPOSE, ENUM_PURPOSE_KEEP_ALIVE);
          
          gen.add(packet);
        }
      }
    }
    
    private static final String[] quash_strings;
    
    static
    {
      quash_strings = (new String[(1 << Byte.SIZE)]);
      
      for (int i = 0; i < quash_strings.length; i++) {
        quash_strings[i] = ("%" + HexStr.bin2hex((new byte[] { ((byte)(i)) })));
      }
      
      for (char[] range : (new char[][] { { '+', '/' } /* plus, minus, dot, slash */, { '0', '9' }, { 'A', 'Z' }, { '_', '_' } /* underscore */, { 'a', 'z' } })) {
        for (char x = range[0]; x <= range[1]; x++) {
          quash_strings[((int)(x))] = (("" + x));
        }
      }
    }
    
    private static String quash_bytes(byte[] buf, int off, int lim)
    {
      StringBuilder out = (new StringBuilder());
      
      for (int pos = off; pos < lim; pos++) {
        int val = (((int)(buf[pos])) & 0xFF);
        out.append(quash_strings[val]);
      }
      
      return out.toString();
    }
    
    private static String quash_bytes(byte[] buf)
    {
      return quash_bytes(buf, 0, buf.length);
    }
    
    private static String quash_bytes_between(byte[] buf, int off, int lim, byte[] head, byte[] tail)
    {
      int top, bot;
      
      if ((top = PartsUtils.locate(buf, off, lim, head)) != -1) {
        top += head.length;
        
        if ((bot = PartsUtils.locate(buf, top, lim, tail)) != -1) {
          return quash_bytes(buf, top, bot);
        }
      }
      
      return null;
    }
    
    static final Charset UTF_8;
    
    static
    {
      try {
        UTF_8 = Charset.forName("UTF-8");
      } catch (Exception e) {
        throw (new RuntimeException(e));
      }
    }
    
    static String weave(String[] template, String... argument)
    {
      if (template.length != (argument.length + 1)) throw null;
      
      StringBuilder out = (new StringBuilder());
      
      for (int i = 0; i < argument.length; i++) {
        out.append(template[i]);
        out.append(argument[i]);
      }
      
      out.append(template[argument.length]);
      
      return out.toString();
    }
    
    static final String HTTP_SERVER_LINE = "Server: zs42.learner2g.Deliver2g\r\n";
    static final String HTTP_ERROR_MESSAGE_BODY = "-FAIL\n";
    static final String HTTP_CACHE_CONTROL_VOLATILE_LINE = "Cache-Control: private, no-cache, no-transform, max-age=1, s-maxage=1\r\nPragma: no-cache\r\n";
    
    static final byte[] HTTP_ENTER_TO_REDIRECT = ("HTTP/1.1 302 Found\r\n" + HTTP_SERVER_LINE + HTTP_CACHE_CONTROL_VOLATILE_LINE + "Location: ").getBytes(UTF_8);
    static final byte[] HTTP_REDIRECT_TO_CONTENT_LENGTH = ("\r\nContent-Type: text/html\r\nContent-Length: ").getBytes(UTF_8);
    
    static final byte[] HTTP_ENTER_TO_CONTENT_TYPE = ("HTTP/1.1 200 OK\r\n" + HTTP_SERVER_LINE + HTTP_CACHE_CONTROL_VOLATILE_LINE + "Content-Type: ").getBytes(UTF_8);
    static final byte[] HTTP_CONTENT_TYPE_TEXT_PLAIN = ("text/plain").getBytes(UTF_8);
    static final byte[] HTTP_CONTENT_TYPE_TEXT_HTML = ("text/html").getBytes(UTF_8);
    static final byte[] HTTP_CONTENT_TYPE_APPLICATION_OCTET_STREAM = ("application/octet-stream").getBytes(UTF_8);
    static final byte[] HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH = ("\r\nContent-Length: ").getBytes(UTF_8);
    static final byte[] HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY = ("\r\n\r\n").getBytes(UTF_8);
    static final byte[] HTTP_MESSAGE_BODY_TO_LEAVE = ("").getBytes(UTF_8);
    
    static final byte[] HTTP_CRLF = ("\r\n").getBytes(UTF_8);
    static final byte[] HTTP_CRLF_CRLF = ("\r\n\r\n").getBytes(UTF_8);
    static final byte[] HTTP_GET_REQUEST = ("GET ").getBytes(UTF_8);
    static final byte[] HTTP_ENTER_URL = ("").getBytes(UTF_8);
    static final byte[] HTTP_LEAVE_URL = (" ").getBytes(UTF_8);
    static final byte[] HTTP_ENTER_FORWARDED_FOR = ("\r\nX-Forwarded-For: ").getBytes(UTF_8);
    static final byte[] HTTP_LEAVE_FORWARDED_FOR = ("\r\n").getBytes(UTF_8);
    static final byte[] HTTP_METHOD_NOT_ALLOWED = ("HTTP/1.1 405 Method Not Allowed\r\n" + HTTP_SERVER_LINE + HTTP_CACHE_CONTROL_VOLATILE_LINE + "Allow: GET\r\nX-Rationale: this-server-only-supports-GET-requests\r\n\r\n").getBytes(UTF_8);
    static final byte[] HTTP_ERROR_RESPONSE = ("HTTP/1.1 200 OK\r\n" + HTTP_SERVER_LINE + HTTP_CACHE_CONTROL_VOLATILE_LINE + "Content-Type: text/plain\r\nContent-Length: " + HTTP_ERROR_MESSAGE_BODY.length() + "\r\n\r\n" + HTTP_ERROR_MESSAGE_BODY).getBytes(UTF_8);
    
    static final String ENTER_SPAN_1 = "<span style=\"font-weight: bold; color: ";
    static final String ENTER_SPAN_2 = ";\">";
    static final String ENTER_SPAN_GOOD = ENTER_SPAN_1 + "green" + ENTER_SPAN_2;
    static final String ENTER_SPAN_EVIL = ENTER_SPAN_1 + "red" + ENTER_SPAN_2;
    static final String ENTER_SPAN_DATA = ENTER_SPAN_1 + "blue" + ENTER_SPAN_2;
    static final String LEAVE_SPAN = "</span>";
    
    static final boolean DEBUG_APPLETS = false;
    
    static final String ERROR_NOENT      = "317";
    static final String ERROR_PERM       = "319";
    static final String ERROR_WOULDBLOCK = "329";
    
    static final String HTML_SUPPORT_FRAGMENT = "<p>If you need assistance, please chat with <a href=\"http://interact.goodsofthemind.com/action?module=support&amp;stage=1&amp;shibb=varza\">technical support</a> (you may wish to open the link in a new window). A technologist will be available to assist you at or around the dates and times at which classes are taking place.</p>";
    static final String HTML_PREMABLE_FRAGMENT = "<p>The applet should load soon. After the applet loads, it will open a new window automatically, where you will be able to see what the instructor is writing. Also, you should be able to hear what the instructor is saying.</p><p>" + ENTER_SPAN_EVIL + "Please do not ask the instructor for technical support!" + LEAVE_SPAN + " During class, the instructor will be too busy teaching to provide technical support. The messaging field in the applet is for discussion and questions related to the class topics only. If you experience technical problems, please use the technical support chat.</p>" + HTML_SUPPORT_FRAGMENT + "<p>" + ENTER_SPAN_GOOD + "We hope you enjoy the class!" + LEAVE_SPAN + "</p>";
    static final String HTML_COMEBACK_FRAGMENT = "<p>Java not working? Run the fast and easy Java installer from <a href=\"http://www.java.com\">www.java.com</a>!</p>";
    
    static final String[] HTML_PAGE = ("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n<html><head><title>Online Class Gateway (@@@)</title></head><body style=\"font-family: monospace\"><p>Welcome to the online class gateway!</p>@@@</body>").split("@@@");
    static final String[] HTML_ERROR_PAGE = (weave(HTML_PAGE, "Error @@@", "<p>Couldn't proceed because <span style=\"font-weight: bold; color: red;\">@@@</span>.</p>" + HTML_SUPPORT_FRAGMENT)).split("@@@");
    static final String[] HTML_COVER_PAGE = (weave(HTML_PAGE, "OK", "<p>Your " + ENTER_SPAN_GOOD + "ticket is valid" + LEAVE_SPAN + ", and the requested channel \"@@@\" exists, but " + ENTER_SPAN_EVIL + "class has not started yet" + LEAVE_SPAN + ". When class starts, this page will change and allow you to join the class. Please check again closer to the starting time of the class. If you think class is supposed to have started by now, then please do contact technical support for assistance.</p><p>Server time indicates " + ENTER_SPAN_DATA + "@@@" + LEAVE_SPAN + " minutes and " + ENTER_SPAN_DATA + "@@@" + LEAVE_SPAN + " seconds past the hour.</p><p><a href=\"@@@\">&gt;&gt; Check Again &lt;&lt;</a> to see whether class has started yet or not.</p><p><a href=\"@@@\">&gt;&gt; Access Recordings &lt;&lt;</a> of previously held sessions of this class.</p>" + HTML_SUPPORT_FRAGMENT)).split("@@@");
    static final String[] HTML_NOTES_PAGE = (weave(HTML_PAGE, "OK", "<p>Your " + ENTER_SPAN_GOOD + "ticket is valid" + LEAVE_SPAN + ", @@@ there are @@@ recording(s) available for viewing at this time.</p><p>Hints:</p><ul><li>Each session may have been split into one or more parts.</li><li>In some cases, it may be necessary to restart your browser between watching each part to avoid running out of memory. This depends upon whether the applet is successfully stopped or not.</li></ul><p>The following recordings are available for viewing:</p><ul>@@@</ul></p>")).split("@@@");
    static final String   HTML_NOTES_DENIED_PAGE = (weave(HTML_PAGE, "Access Denied", "<p>Your ticket is not enabled for recordings access.</p>"));
    static final String[] HTML_APPLET_TEACHER = (weave(HTML_PAGE, "Teacher's Applet", "<applet code=\"zs42.learner2g.teacher2g.Teacher2g\" archive=\"/resource/teacher-applet/utc@@@ms.jar\" height=\"100\" width=\"500\"><param name=\"SETTINGS\" value=\"ORIGIN PSEUDO_HOST PSEUDO_PATH PSEUDO_NAME SUPPORT_REWIND ENFORCE_REWIND ASSISTANT ASSISTANT_KMAC ETCH_H ETCH_W ETCH_UPS\"></param><param name=\"SETTING_ORIGIN\" value=\"pseudo\"></param><param name=\"SETTING_PSEUDO_HOST\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_PATH\" value=\"/\"></param><param name=\"SETTING_PSEUDO_NAME\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_AUTH\" value=\"@@@\"></param><param name=\"SETTING_SUPPORT_REWIND\" value=\"false\"></param><param name=\"SETTING_ENFORCE_REWIND\" value=\"false\"></param><param name=\"SETTING_ASSISTANT\" value=\"KMAC\"></param><param name=\"SETTING_ASSISTANT_KEY\" value=\"@@@\"></param><param name=\"SETTING_ETCH_H\" value=\"@@@\"></param><param name=\"SETTING_ETCH_W\" value=\"@@@\"></param><param name=\"SETTING_ETCH_UPS\" value=\"30\"></param></applet>" + HTML_COMEBACK_FRAGMENT)).split("@@@");
    static final String[] HTML_APPLET_STUDENT = (weave(HTML_PAGE, "Student's Applet", HTML_PREMABLE_FRAGMENT + "<applet code=\"zs42.learner2g.student2g.Student2g\" archive=\"/resource/student-applet/utc@@@ms.jar\" height=\"100\" width=\"500\"><param name=\"SETTINGS\" value=\"ORIGIN PSEUDO_HOST PSEUDO_PATH PSEUDO_NAME PSEUDO_AUTH STUDENT_NAME SUPPORT_REWIND ENFORCE_REWIND ASSISTANT ETCH_H ETCH_W ETCH_UPS" + (DEBUG_APPLETS ? " USE_128_BIT_VECTORS" : "") + "\"></param><param name=\"SETTING_ORIGIN\" value=\"pseudo\"></param><param name=\"SETTING_PSEUDO_HOST\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_PATH\" value=\"/\"></param><param name=\"SETTING_PSEUDO_NAME\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_AUTH\" value=\"@@@\"><param name=\"SETTING_STUDENT_NAME\" value=\"@@@\"></param><param name=\"SETTING_SUPPORT_REWIND\" value=\"false\"></param><param name=\"SETTING_ENFORCE_REWIND\" value=\"false\"></param><param name=\"SETTING_ASSISTANT\" value=\"SHA2\"></param></param><param name=\"SETTING_ETCH_H\" value=\"@@@\"></param><param name=\"SETTING_ETCH_W\" value=\"@@@\"></param><param name=\"SETTING_ETCH_UPS\" value=\"30\"></param>" + (DEBUG_APPLETS ? "<param name=\"SETTING_USE_128_BIT_VECTORS\" value=\"true\"></param>" : "") + "</applet>" + HTML_COMEBACK_FRAGMENT)).split("@@@");
    static final String[] HTML_APPLET_NOTES = (weave(HTML_PAGE, "Student's Applet", "<p>You are accessing a recorded session; this time, there is no instructor!</p><p>The applet should load soon.</p><applet code=\"zs42.learner2g.student2g.Student2g\" archive=\"/resource/student-applet/utc@@@ms.jar\" height=\"100\" width=\"500\"><param name=\"SETTINGS\" value=\"ORIGIN COOKED_HOST COOKED_PATH SUPPORT_REWIND ENFORCE_REWIND ASSISTANT ETCH_H ETCH_W ETCH_UPS" + (DEBUG_APPLETS ? " USE_128_BIT_VECTORS" : "") + "\"></param><param name=\"SETTING_ORIGIN\" value=\"cooked\"></param><param name=\"SETTING_COOKED_HOST\" value=\"@@@\"></param><param name=\"SETTING_COOKED_PATH\" value=\"@@@\"></param><param name=\"SETTING_SUPPORT_REWIND\" value=\"true\"></param><param name=\"SETTING_ENFORCE_REWIND\" value=\"true\"></param><param name=\"SETTING_ASSISTANT\" value=\"NONE\"></param></param><param name=\"SETTING_ETCH_H\" value=\"@@@\"></param><param name=\"SETTING_ETCH_W\" value=\"@@@\"></param><param name=\"SETTING_ETCH_UPS\" value=\"30\"></param>" + (DEBUG_APPLETS ? "<param name=\"SETTING_USE_128_BIT_VECTORS\" value=\"true\"></param>" : "") + "</applet>" + HTML_COMEBACK_FRAGMENT)).split("@@@");
    
    static class SpecificResponse extends DataLane2.Response
    {
      final ArrayList<Packet> gen;
      
      boolean sendErrorMessageInstead = false;
      
      final ByteArrayOutputStream buf = (new ByteArrayOutputStream());
      
      String path = null;
      
      SpecificResponse(ArrayList<Packet> gen)
      {
        this.gen = gen;
      }
      
      public SpecificResponse alsoDropConnection()
      {
        super.alsoDropConnection();
        
        return this;
      }
      
      public SpecificResponse alsoSendErrorMessageInstead()
      {
        sendErrorMessageInstead = true;
        
        return this;
      }
      
      public SpecificResponse doFAIL()
      {
        return alsoSendErrorMessageInstead().alsoDropConnection();
      }
      
      public SpecificResponse alsoSendFileContents(String path)
      {
        this.path = path;
        
        return this;
      }
      
      public void writeTo(OutputStream out) throws Exception
      {
        if (sendErrorMessageInstead) {
          out.write(HTTP_ERROR_RESPONSE);
        } else {
          buf.writeTo(out);
          
          if (path != null) {
            sendfile(path, out);
          }
        }
      }
      
      private final void sendfile(String path, OutputStream out)
      {
        try {
          final FileInputStream inp = (new FileInputStream(path));
          
          try {
            byte[] buf = (new byte[65536]);
            
            int amt;
            
            while ((amt = inp.read(buf, 0, buf.length)) >= 0) {
              out.write(buf, 0, amt);
            }
          } finally {
            SimpleIO.liquidate(inp);
          }
        } catch (Throwable e) {
          throw (new RuntimeException(e));
        }
      }
    }
    
    private String zeropad(int required, String content)
    {
      while (content.length() < required) {
        content = ("0" + content);
      }
      
      return content;
    }
    
    private SpecificResponse write_redirect(SpecificResponse res, String href) throws IOException
    {
      byte[] body = ("<html><head><title>Redirecting ...</title></head><body><p>The resource you are looking for has been temporarily relocated to <a href=\"" + href + "\">" + href + "</a>. Your browser should redirect automatically in a few seconds; if not, please follow the link to continue.</p></body></html>").getBytes(UTF_8);
      
      res.buf.write(HTTP_ENTER_TO_REDIRECT);
      res.buf.write(href.getBytes(UTF_8));
      res.buf.write(HTTP_REDIRECT_TO_CONTENT_LENGTH);
      res.buf.write(("" + body.length).getBytes(UTF_8));
      res.buf.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      res.buf.write(body);
      res.buf.write(HTTP_MESSAGE_BODY_TO_LEAVE);
      
      return res;
    }
    
    private SpecificResponse write_blob_page(SpecificResponse res, byte[] mime, byte[] blob) throws IOException
    {
      res.buf.write(HTTP_ENTER_TO_CONTENT_TYPE);
      res.buf.write(mime);
      res.buf.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      res.buf.write(("" + blob.length).getBytes(UTF_8));
      res.buf.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      res.buf.write(blob);
      res.buf.write(HTTP_MESSAGE_BODY_TO_LEAVE);
      
      return res;
    }
    
    private SpecificResponse write_text_page(SpecificResponse res, String text) throws IOException
    {
      return write_blob_page(res, HTTP_CONTENT_TYPE_TEXT_PLAIN, text.getBytes(UTF_8));
    }
    
    private SpecificResponse write_html_page(SpecificResponse res, String html) throws IOException
    {
      return write_blob_page(res, HTTP_CONTENT_TYPE_TEXT_HTML, html.getBytes(UTF_8));
    }
    
    private SpecificResponse write_marker(SpecificResponse res, String marker) throws IOException
    {
      byte[] bytes = marker.getBytes(UTF_8);
      res.buf.write(bytes);
      res.buf.write(((byte)(bytes.length)));
      return res;
    }
    
    private SpecificResponse handle_admin_status_request(SpecificResponse res) throws IOException
    {
      StringBuilder content = (new StringBuilder());
      
      content.append(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().toString());
      content.append("\n\n");
      
      if (environment != null) {
        content.append(environment.toString());
        content.append("\n\n");
      }
      
      content.append(("recent_sessioni='" + recent_sessioni + "'\n\n"));
      
      content.append(("sessions.size()=" + sessions.size() + "\n\n"));
      
      for (Map.Entry<String, Session> entry : sessions.entrySet()) {
        Session session = entry.getValue();
        
        content.append(("sessioni='" + session.sessioni + "' ['" + entry.getKey() + "']\n"));
        content.append(("announce='" + session.announce + "'\n"));
        content.append(("packets.size()=" + session.packets.size() + "\n"));
        content.append(("messages.size()=" + session.messages.size() + "\n"));
        content.append(("\n"));
      }
      
      content.append(("forwardingLocked=" + forwardingLocked + "\n\n"));
      
      content.append(("announcedPeers.size()=" + announcedPeers.size() + "\n\n"));
      
      for (String announcedPeer : announcedPeers) {
        content.append(("peer '" + announcedPeer + "' usage " + peerUsage.get(announcedPeer) + "\n"));
      }
      
      return write_text_page(res, content.toString());
    }
    
    private SpecificResponse handle_admin_kill_announced_peers_request(SpecificResponse res) throws IOException
    {
      announcedPeers.clear();
      peerUsage.clear();
      
      // re-enable the forwarding lock (that is, disable forwarding if it was enabled)
      forwardingLocked = true;
      
      return write_text_page(res, "+OK");
    }
    
    private SpecificResponse handle_admin_kill_sessions_request(SpecificResponse res) throws IOException
    {
      // broadcast rather than handling locally; all nodes should clear their session maps
      {
        Packet packet = (new Packet());
        
        packet.setAttr(ATTR_PURPOSE, ENUM_PURPOSE_KILL_ALL_SESSIONS);
        
        res.gen.add(packet);
      }
      
      return write_text_page(res, "+OK");
    }
    
    private SpecificResponse handle_admin_unlock_forwarding_request(SpecificResponse res) throws IOException
    {
      // disable the forwarding lock (enable forwarding)
      forwardingLocked = false;
      
      return write_text_page(res, "+OK");
    }
    
    private SpecificResponse handle_admin_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String mysterion = words[wordi++];
      final String command   = words[wordi++];
      
      if (!(mysterion.equals(gatekeeper.control_credential))) throw null;
      
      /****/ if (command.equals("status")) {
        return handle_admin_status_request(res);
      } else if (command.equals("kill-announced-peers")) {
        return handle_admin_kill_announced_peers_request(res);
      } else if (command.equals("kill-sessions")) {
        return handle_admin_kill_sessions_request(res);
      } else if (command.equals("unlock-forwarding")) {
        return handle_admin_unlock_forwarding_request(res);
      } else {
        throw null;
      }
    }
    
    private SpecificResponse handle_resource_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String name = words[wordi++];
      
      final Resource resource;
      
      if ((resource = gatekeeper.resources.get(name)) == null) {
        Log.log(("resource not found for name='" + name + "'"));
        return res.doFAIL();
      }
      
      res.buf.write(HTTP_ENTER_TO_CONTENT_TYPE);
      res.buf.write(resource.mime.getBytes(UTF_8));
      res.buf.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      res.buf.write(("" + resource.data.length).getBytes(UTF_8));
      res.buf.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      res.buf.write(resource.data);
      res.buf.write(HTTP_MESSAGE_BODY_TO_LEAVE);
      
      return res;
    }
    
    private SpecificResponse handle_teacher_login_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      String organization_name = words[wordi++];
      String channel_name      = words[wordi++];
      String credential        = words[wordi++];
      String method            = words[wordi++];
      
      Channel channel = gatekeeper.organizations.get(organization_name).channels.get(channel_name);
      
      if (!(channel.teacher_credentials.containsKey(credential))) return res.doFAIL();
      
      // broadcast rather than handling locally
      {
        Packet packet = (new Packet());
        
        packet.setAttr(ATTR_PURPOSE, ENUM_PURPOSE_INITIATE_SESSION);
        packet.setAttr(ATTR_ORGANIZATION, organization_name);
        packet.setAttr(ATTR_CHANNEL, channel_name);
        
        res.gen.add(packet);
      }
      
      /*
        there is no telling when the session initiation will be
        handled on other servers, but this is not problematic as long
        as the teacher client uploads splitter data to the same ser it
        logs in on.
        
        the current server should handle the session initiation fairly
        soon; nevertheless, it would be prudent to introduce a small
        delay in the client ...
       */
      
      /****/ if (method.equals("applet")) {
        return write_html_page(res, weave(HTML_APPLET_TEACHER, now_ms, gatekeeper.host_http, formSessionIdentifier(channel), "<<<UNKNOWN_TEACHER_AUTHENTICATOR>>>", HexStr.bin2hex(global_teacher_kmac), ("" + channel.pxH), ("" + channel.pxW)));
      } else if (method.equals("simple")) {
        return write_text_page(res, ("<s><p d=\"teacher\"><p d=\"session\"><k>authenticated</k><v>true</v><k>KMAC</k><v>" + HexStr.bin2hex(global_teacher_kmac) + "</v><k>H</k><v>" + channel.pxH + "</v><k>W</k><v>" + channel.pxW + "</v></p></p></s>\n"));
      } else {
        throw null;
      }
    }
    
    private SpecificResponse handle_student_login_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String credential        = words[wordi++];
      
      // redirect
      {
        if (amRoot && (announcedPeers.size() > 0) && ((!forwardingLocked) || (src.equals(gatekeeper.src_test)))) {
          String sel = null;
          
          /*
            select peer with lowest projected load. this is better
            than round-robin if the peers don't join simultaneously
            (which the currently don't since they must be launched
            manually). if the peer do join simultaneously, it is not
            as clean as round-robin as it may select the same node
            twice in a row; e.g., for usage vector sequence <2, 1, 2>,
            <2, 2, 2>, <2, 3, 2> ... actually this is unlikely to
            happen for most map implementations which will yield the
            items in the same order in the typical case in which there
            have been no insertions
            
            update: after adding forwardingLocked, which opens the
            floodgates simultaneously, there is absolutely no gain
            here -- it should be changed to round robin or, better yet
            code to evenly distribute unique IPs
           */
          {
            int min = Integer.MAX_VALUE;
            
            for (String announcedPeer : announcedPeers) {
              int val = peerUsage.get(announcedPeer);
              
              if (val < min) {
                min = val;
                sel = announcedPeer;
              }
            }
            
            peerUsage.put(sel, (min+1));
          }
          
          return write_redirect(res, ("http://" + sel + "/student-login/" + organization_name + "/" + channel_name + "/" + credential + "/" + now_ms));
        }
      }
      
      final Organization organization;
      final Channel channel;
      final Session session;
      
      if (((organization = gatekeeper.organizations.get(organization_name)) == null) ||
          ((channel = organization.channels.get(channel_name)) == null)) {
        return write_html_page(res, weave(HTML_ERROR_PAGE, ERROR_NOENT, "the requested channel does not exist (error " + ERROR_NOENT + ")"));
      }
      
      String student_name;
      
      if ((student_name = channel.student_credentials.get(credential)) == null) {
        return write_html_page(res, weave(HTML_ERROR_PAGE, ERROR_PERM, "access is restricted (error " + ERROR_PERM + ")"));
      }
      
      student_name = (student_name + "-" + (10000 + secureRandom.nextInt(10000)));
      
      if ((session = sessions.get(formSessionIdentifier(channel))) == null) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        
        return write_html_page(res, weave(HTML_COVER_PAGE, channel.name, zeropad(2, ("" + minutes)), zeropad(2, ("" + seconds)), ("/student-login/" + organization_name + "/" + channel_name + "/" + credential + "/" + now_ms), ("/student-notes/" + organization_name + "/" + channel_name + "/" + credential + "/" + now_ms)));
      }
      
      return write_html_page(res, weave(HTML_APPLET_STUDENT, now_ms, gatekeeper.host_http, (organization_name + "/" + channel_name), session.newStudentAuthenticator(src, credential), student_name, ("" + session.channel.pxH), ("" + session.channel.pxW)));
    }
    
    private Session locate_session(String organization_name, String channel_name, String mysterion, boolean privileged)
    {
      final Channel channel = gatekeeper.organizations.get(organization_name).channels.get(channel_name);
      
      if (channel == null) throw null;
      
      final Session session = sessions.get(formSessionIdentifier(channel));
      
      if (session == null) throw null;
      
      if (!(mysterion.equals(gatekeeper.control_credential) || session.channel.teacher_credentials.containsKey(mysterion) || session.teacher_authenticator.equals(mysterion) || (!privileged && session.student_authenticators.containsKey(mysterion)))) return null;
      
      return session;
    }
    
    private SpecificResponse handle_control_session_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      
      final Session session = locate_session(organization_name, channel_name, mysterion, true);
      
      return write_text_page(res, ("<s><p d=\"teacher\"><p d=\"session\"><k>authenticated</k><v>true</v><k>KMAC</k><v>" + HexStr.bin2hex(global_teacher_kmac) + "</v></p></p></s>\n"));
    }
    
    private SpecificResponse handle_push_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      return res.doFAIL();
    }
    
    private static final Groupir2g.StreamIdentifier[] streamIdentifierValues = Groupir2g.StreamIdentifier.values();
    
    private SpecificResponse handle_pull_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String marker            = words[wordi++];
      final String buffer_size       = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Session session = locate_session(organization_name, channel_name, mysterion, false);
      
      /***
       * marker may be either 'i' or 's' followed by a number (which
       * is always the next-to-be-served index the packets array); 'i'
       * means the connection is in the initial catch-up state while
       * 's' means the connection in the steady state.
       ***/
      
      final int MAXIMUM_MARKER_LENGTH = 12;
      
      final String pseudo_marker;
      
      if (marker.equals("ENTER")) {
        pseudo_marker = "i0";
      } else {
        pseudo_marker = marker;
      }
      
      final boolean initial_marker_catch = (pseudo_marker.charAt(0) == 'i');
      final int     initial_marker_index = Integer.parseInt(pseudo_marker.substring(1));
      
      final int buffer_consumption;
      final String output_marker;
      
      final int initial_buffer_size = Math.max(0, (Integer.parseInt(buffer_size) - MAXIMUM_MARKER_LENGTH));
      
      // measure (setting buffer_consumption and output_marker)
      if (session != null) {
        int remaining_buffer_size = initial_buffer_size;
        
        boolean current_marker_catch = initial_marker_catch;
        int     current_marker_index = initial_marker_index;
        
        boolean overflowed = false;
        
        while (current_marker_index < session.packets.size()) {
          Packet packet = session.packets.get(current_marker_index);
          
          if (current_marker_catch && (streamIdentifierValues[Integer.parseInt(packet.getAttr(ATTR_STREAM_IDENTIFIER))].getDisposition() == Groupir2g.StreamDisposition.PRIMARY)) {
            current_marker_index++;
            continue;
          }
          
          byte[] payload = packet.getItem(ITEM_PAYLOAD);
          
          if (payload.length <= remaining_buffer_size) {
            /* res.buf.write(payload); */
            remaining_buffer_size -= payload.length;
          } else {
            overflowed = true;
            break;
          }
          
          current_marker_index++;
        }
        
        if (!overflowed) {
          current_marker_catch = false;
        }
        
        buffer_consumption = (initial_buffer_size - remaining_buffer_size);
        output_marker = ((current_marker_catch ? "i" : "j") + current_marker_index);
      } else {
        buffer_consumption = 0;
        output_marker = "LEAVE";
      }
      
      // write HTTP header
      res.buf.write(HTTP_ENTER_TO_CONTENT_TYPE);
      res.buf.write(HTTP_CONTENT_TYPE_APPLICATION_OCTET_STREAM);
      res.buf.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      res.buf.write(("" + (buffer_consumption + output_marker.length() + 1)).getBytes(UTF_8));
      res.buf.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      
      // write packets
      if (session != null) {
        int remaining_buffer_size = initial_buffer_size;
        
        boolean current_marker_catch = initial_marker_catch;
        int     current_marker_index = initial_marker_index;
        
        boolean overflowed = false;
        
        while (current_marker_index < session.packets.size()) {
          Packet packet = session.packets.get(current_marker_index);
          
          if (current_marker_catch && (streamIdentifierValues[Integer.parseInt(packet.getAttr(ATTR_STREAM_IDENTIFIER))].getDisposition() == Groupir2g.StreamDisposition.PRIMARY)) {
            current_marker_index++;
            continue;
          }
          
          byte[] payload = packet.getItem(ITEM_PAYLOAD);
          
          if (payload.length <= remaining_buffer_size) {
            res.buf.write(payload);
            remaining_buffer_size -= payload.length;
          } else {
            overflowed = true;
            break;
          }
          
          current_marker_index++;
        }
      }
      
      // write marker
      return write_marker(res, output_marker);
    }
    
    private final static Pattern pattern_hexstr = Pattern.compile("([0-9a-f]{2})*");
    
    private SpecificResponse handle_push_mesg_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String content           = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Session session = locate_session(organization_name, channel_name, mysterion, false);
      
      if ((content.length() % 2) != 0) throw null;
      if (!pattern_hexstr.matcher(content).matches()) throw null;
      
      if (session != null) {
        Log.log(("accepting message for session " + session.sessioni + " from mysterion " + mysterion + " with content " + content));
        
        // broadcast rather than handling locally
        {
          Packet packet = (new Packet());
          
          packet.setAttr(ATTR_PURPOSE, ENUM_PURPOSE_FEEDBACK_PAYLOAD);
          packet.setAttr(ATTR_SESSION, session.sessioni);
          packet.setItem(ITEM_PAYLOAD, content.getBytes(UTF_8));
          
          res.gen.add(packet);
        }
        
        return write_text_page(res, "ACCEPTED");
      } else {
        return write_text_page(res, "OUTDATED");
      }
    }
    
    private SpecificResponse handle_pull_mesg_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String marker            = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Session session = locate_session(organization_name, channel_name, mysterion, true);
      
      StringBuilder result = (new StringBuilder());
      
      for (int i = Integer.parseInt(marker); i < session.messages.size(); i++) {
        result.append(PartsUtils.arr2str(session.messages.get(i)));
        result.append('\n');
      }
      
      return write_text_page(res, result.toString());
    }
    
    private SpecificResponse handle_student_notes_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Channel channel = gatekeeper.student_credentials.get(mysterion);
      
      int count = 0;
      StringBuilder block = (new StringBuilder());
      
      for (Map.Entry<String, ArrayList<Long>> entry : channel.recordings.entrySet()) {
        final String recording = entry.getKey();
        final ArrayList<Long> lengths = entry.getValue();
        
        if (lengths.size() > 0) {
          block.append("<li>" + recording + "</li>");
          block.append("<ul>");
          
          for (int section = 0; section < lengths.size(); section++) {
            block.append("<li><a href=\"/student-notes-applet/" + organization_name + "/" + channel_name + "/" + mysterion + "/" + recording + "/" + section + "/" + now_ms + "\">Part " + (section+1) + "</a></li>");
          }
          
          block.append("</ul>");
          
          count++;
        }
      }
      
      if (count == 0) {
        block.append("<li>(none)</li>");
      }
      
      return write_html_page(res, weave(HTML_NOTES_PAGE, ((count > 0) ? "and" : "but"), ((count > 0) ? ("" + count) : "no"), block.toString()));
    }
    
    private SpecificResponse handle_student_notes_applet_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String recording         = words[wordi++];
      final String section           = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Channel channel = gatekeeper.student_credentials.get(mysterion);
      
      if (!(channel.recordings.containsKey(recording))) return res.doFAIL();
      
      // for now, existence of a rewatch credential means DENY
      if (channel.rewatch_credentials.containsKey(mysterion)) {
        return write_html_page(res, HTML_NOTES_DENIED_PAGE);
      } else {
        return write_html_page(res, weave(HTML_APPLET_NOTES, now_ms, gatekeeper.host_http, ("/student-notes-archive/" + organization_name + "/" + channel_name + "/" + mysterion + "/" + recording + "/" + section + "/" + now_ms), ("" + channel.pxH), ("" + channel.pxW)));
      }
    }
    
    private SpecificResponse handle_student_notes_archive_request(SpecificResponse res, String src, String[] words, int wordi) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String recording         = words[wordi++];
      final String section_string    = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Channel channel = gatekeeper.student_credentials.get(mysterion);
      
      // for now, existence of a rewatch credential means DENY
      if (channel.rewatch_credentials.containsKey(mysterion)) {
        throw null;
      }
      
      final int section = Integer.parseInt(section_string);
      final long length = channel.recordings.get(recording).get(section);
      
      res.buf.write(HTTP_ENTER_TO_CONTENT_TYPE);
      res.buf.write(HTTP_CONTENT_TYPE_APPLICATION_OCTET_STREAM);
      res.buf.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      res.buf.write(("" + length).getBytes(UTF_8));
      res.buf.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      
      final String path = (gatekeeper.recordings_directory + File.separator + channel.name + File.separator + recording + section + "i");
      
      return res.alsoSendFileContents(path);
    }
    
    private SpecificResponse handle_unknown_request(SpecificResponse res, String src, String[] words, int wordi, String action) throws IOException
    {
      final Channel channel;
      
      if ((channel = gatekeeper.student_credentials.get(action)) != null) {
        return handle_student_login_request(res, src, (new String[] { channel.organization_name, channel.name, action }), 0);
      }
      
      return res.doFAIL();
    }
    
    private SpecificResponse handle_request_stage_2(SpecificResponse res, String src, String req) throws IOException
    {
      Log.log(("handle_request_stage_2(src='" + src + "', req='" + req + "')"));
      
      int      wordi = 0;
      String[] words = NoUTF.tokenize(req, FILTER_TOKENIZE);
      
      /*
        for (String word : words) {
          Log.log(("token='" + word + "'"));
        }
      */
      
      String action = words[wordi++];
      
      Channel channel;
      
      /****/ if (action.equals("admin")) {
        return handle_admin_request(res, src, words, wordi);
      } else if (action.equals("resource")) {
        return handle_resource_request(res, src, words, wordi);
      } else if (action.equals("teacher-login")) {
        return handle_teacher_login_request(res, src, words, wordi);
      } else if (action.equals("student-login")) {
        return handle_student_login_request(res, src, words, wordi);
      } else if (action.equals("control-session")) {
        return handle_control_session_request(res, src, words, wordi);
      } else if (action.equals("push")) {
        return handle_push_request(res, src, words, wordi);
      } else if (action.equals("pull")) {
        return handle_pull_request(res, src, words, wordi);
      } else if (action.equals("push-mesg")) {
        return handle_push_mesg_request(res, src, words, wordi);
      } else if (action.equals("pull-mesg")) {
        return handle_pull_mesg_request(res, src, words, wordi);
      } else if (action.equals("student-notes")) {
        return handle_student_notes_request(res, src, words, wordi);
      } else if (action.equals("student-notes-applet")) {
        return handle_student_notes_applet_request(res, src, words, wordi);
      } else if (action.equals("student-notes-archive")) {
        return handle_student_notes_archive_request(res, src, words, wordi);
      } else {
        return handle_unknown_request(res, src, words, wordi, action);
      }
    }
    
    private SpecificResponse handle_request_stage_1(SpecificResponse res, String src, byte[] buf, int off, int lim) throws IOException
    {
      //Log.log(("handle_request_stage_1(src='" + src + "', req='" + quash_bytes(buf, off, lim) + "')"));
      
      String req;
      
      // extract request string
      {
        if ((req = quash_bytes_between(buf, off, lim, HTTP_ENTER_URL, HTTP_LEAVE_URL)) == null) {
          return res.doFAIL();
        }
      }
      
      // embellish source address with data from X-Forwarded-For headers
      /*
      {
        int top, bot;
        
        top = lim;
        
        while ((top = PartsUtils.locate(buf, top, lim, HTTP_ENTER_FORWARDED_FOR)) != -1) {
          top += HTTP_ENTER_FORWARDED_FOR.length;
          
          if ((bot = PartsUtils.locate(buf, top, lim, HTTP_LEAVE_FORWARDED_FOR)) != -1) {
            src = (src + ":" + quash_bytes(buf, top, bot));
            
            top = (bot + HTTP_LEAVE_FORWARDED_FOR.length);
          }
        }
      }
      */
      
      return handle_request_stage_2(res, src, req);
    }
    
    private DataLane2.Response handle_request_stage_0(ArrayList<Packet> gen, String src, byte[] buf, int off, int lim) throws Exception
    {
      //Log.log(("handle_request_stage_0(src='" + src + "', req='" + quash_bytes(buf, off, lim) + "')"));
      
      // skip blank lines
      while (((off < lim) && ((buf[off] == '\r') || (buf[off] == '\n')))) off++;
      
      // ignore empty requests
      if (off == lim) {
        return (new DataLane2.NullResponse());
      }
      
      // require GET request
      {
        if (!PartsUtils.starts_with(buf, off, lim, HTTP_GET_REQUEST)) {
          return (new DataLane2.ByteArrayResponse(HTTP_METHOD_NOT_ALLOWED)).alsoDropConnection();
        }
        
        off += HTTP_GET_REQUEST.length;
      }
      
      SpecificResponse res = (new SpecificResponse(gen));
      
      try {
        handle_request_stage_1(res, src, buf, off, lim);
      } catch (Throwable e) {
        Log.log(e);
        res = res.alsoSendErrorMessageInstead();
      }
      
      /*
      {
        ByteArrayOutputStream tmp = (new ByteArrayOutputStream());
        res.writeTo(tmp);
        byte[] arr = tmp.toByteArray();
        if (arr.length > 512) arr = PartsUtils.extract(arr, 0, 512);
        Log.log(("responding with '" + quash_bytes(arr) + "'"));
      }
      */
      
      return res;
    }
    
    protected DataLane2.Response onRequestLoopback(ArrayList<Packet> gen, String src, byte[] inp) throws Exception
    {
      return handle_request_stage_0(gen, src, inp, 0, inp.length);
    }
  }
  
  public static void main(final String[] args) throws Exception
  {
    Log.loopHandleEventsBackground(System.err, true);
    
    try {
      int argi = 0;
      
      final AsciiTreeMap<String> settings = (new AsciiTreeMap<String>());
      
      Settings.Scanner.populate(settings, args[argi++]);
      
      final Gatekeeper gatekeeper = Gatekeeper.fromSettings(settings, "station");
      
      final LinkedBlockingQueue<Packet> splitter_injector = (new LinkedBlockingQueue<Packet>());
      
      final byte[] global_teacher_kmac = HexStr.hex2bin(args[argi++]);
      
      if (!(global_teacher_kmac.length == SHAZ)) throw (new RuntimeException("Deliver2g::main: bad global teacher kmac specification (of length " + global_teacher_kmac.length + " instead of the expected " + SHAZ + ")"));
      
      AbstractServer.launch
        (gatekeeper.port_splitter,
         (new F1<Void, Socket>()
          {
            public Void invoke(Socket client)
            {
              handle_splitter_connection(global_teacher_kmac, splitter_injector, client);
              return null;
            }
          }));
      
      final boolean amRoot = Boolean.parseBoolean(args[argi++]);
      
      final StationApplication application = (new StationApplication(gatekeeper, amRoot, global_teacher_kmac));
      
      final String mode = args[argi++].intern();
      
      final String MODE_NORMAL = "normal";
      final String MODE_REPLAY = "replay";
      
      /****/ if (mode == MODE_NORMAL) {
        final String logdir_current = args[argi++];
        final String logdir_archive = args[argi++];
        
        final ArrayList<LinkedBlockingQueue<Packet>> injectors = (new ArrayList<LinkedBlockingQueue<Packet>>());
        
        injectors.add(splitter_injector);
        
        final DataLane2.ApplicationWrapper wrapper =
          (new DataLane2.ReplayLoggingApplicationWrapper(application)
            {
              String name = null;
              
              String formPath(String dir, String bas)
              {
                return (dir + "/datalane-" + bas + ".dat");
              }
              
              File formCurrentFile(String name)
              {
                return (new File(formPath(logdir_current, ("current-" + name))));
              }
              
              File formArchiveFile(String name)
              {
                return (new File(formPath(logdir_archive, ("archive-" + name))));
              }
              
              protected DataOutputStream logRotate() throws Exception
              {
                final String prev_name = name;
                
                if (prev_name != null) {
                  Log.startLoggedThread
                    ((new Log.LoggedThread()
                      {
                        public void run() throws Exception
                        {
                          FileInputStream  inp = (new FileInputStream (formCurrentFile(prev_name)));
                          FileOutputStream out = (new FileOutputStream(formArchiveFile(prev_name)));
                          
                          byte[] buf = (new byte[65536]);
                          
                          int amt;
                          
                          while ((amt = inp.read(buf, 0, buf.length)) > 0) {
                            out.write(buf, 0, buf.length);
                          }
                          
                          if (!(amt == -1)) throw null;
                          
                          formCurrentFile(prev_name).delete();
                        }
                      }));
                }
                
                name = ("utc" + System.currentTimeMillis() + "ms");
                
                return (new DataOutputStream(new FileOutputStream(formCurrentFile(name))));
              }
            });
        
        DataLane2.run(settings, wrapper, injectors);
      } else if (mode == MODE_REPLAY) {
        final DataInputStream inp_log = (new DataInputStream(new BufferedInputStream(new FileInputStream(args[argi++]))));
        DataLane2.ReplayLoggingApplicationWrapper.replay(application, inp_log);
      } else {
        throw null;
      }
    } catch (Throwable e) {
      Log.fatal(e);
    }
  }
}
