/***
 * Station2g.java
 * copyright (c) 2012 by andrei borac
 ***/

/***
 * access model:
 * 
 * each organization has a list of channels
 * each channel has a list of teacher credentials that may init sessions for that channel
 * each channel has a list of student credentials that may join sessions for that channel
 ***/

package zs42.learner2g.station2g;

import zs42.learner2g.groupir2g.*;

import zs42.buff.*;

import zs42.parts.*;

import zs42.splitter.common.*;

import java.io.*;
import java.nio.charset.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.atomic.*;
import java.security.*;

import java.lang.management.*;

public class Station2g
{
  static final Charset UTF_8;
  
  static
  {
    try {
      UTF_8 = Charset.forName("UTF-8");
    } catch (Exception e) {
      throw (new RuntimeException(e));
    }
  }
  
  static final NoUTF.Filter FILTER_TOKENIZE = (new NoUTF.Filter("\u0000\u0020//"));
  static final NoUTF.Filter FILTER_TOKENIZE_FIELDS = (new NoUTF.Filter("\u0000\u0020::"));
  
  static RuntimeException fatal(Throwable e)
  {
    Log.log(e);
    
    while (true) {
      try {
        Thread.sleep(1000);
      } catch (Throwable e2) {
        // ignored
      }
    }
  }
  
  static void trace(Throwable e)
  {
    Log.log(e);
  }
  
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
    final String host_http;
    final int port_http;
    final int port_splitter;
    final String archive_directory;
    final String recordings_directory;
    final String control_credential;
    
    final AsciiTreeMap<Resource> resources;
    final AsciiTreeMap<Organization> organizations;
    final AsciiTreeMap<Channel> student_credentials;
    
    Gatekeeper(String host_http, int port_http, int port_splitter, String archive_directory, String recordings_directory, String control_credential, AsciiTreeMap<Resource> resources, AsciiTreeMap<Organization> organizations, AsciiTreeMap<Channel> student_credentials)
    {
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
      
      return (new Gatekeeper(host_http, port_http, port_splitter, archive_directory, recordings_directory, control_credential, resources, organizations, student_credentials));
    }
  }
  
  static class Server
  {
    static final int HTTP_READ_TIMEOUT_MS = 10 * 1000; /* 10 seconds */
    
    static final String HTTP_SERVER_LINE = "Server: zs42.station2g.Station2g\r\n";
    static final String HTTP_ERROR_MESSAGE_BODY = "-FAIL\n";
    static final String HTTP_CACHE_CONTROL_VOLATILE_LINE = "Cache-Control: private, no-cache, no-store, no-transform, max-age=1, s-maxage=1\r\nPragma: no-cache\r\n";
    
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
    static final byte[] HTTP_ENTER_URL = (" ").getBytes(UTF_8);
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
    static final String HTML_COMEBACK_FRAGMENT = "<p>No Java? <a href=\"http://www.java.com\">Get Java!</a>";
    
    static final String[] HTML_PAGE = ("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n<html><head><title>Online Class Gateway (@@@)</title></head><body style=\"font-family: monospace\"><p>Welcome to the online class gateway!</p>@@@</body>").split("@@@");
    static final String[] HTML_ERROR_PAGE = (weave(HTML_PAGE, "Error @@@", "<p>Couldn't proceed because <span style=\"font-weight: bold; color: red;\">@@@</span>.</p>" + HTML_SUPPORT_FRAGMENT)).split("@@@");
    static final String[] HTML_COVER_PAGE = (weave(HTML_PAGE, "OK", "<p>Your " + ENTER_SPAN_GOOD + "ticket is valid" + LEAVE_SPAN + ", and the requested channel \"@@@\" exists, but " + ENTER_SPAN_EVIL + "class has not started yet" + LEAVE_SPAN + ". When class starts, this page will change and allow you to join the class. Please check again closer to the starting time of the class. If you think class is supposed to have started by now, then please do contact technical support for assistance.</p><p>Server time indicates " + ENTER_SPAN_DATA + "@@@" + LEAVE_SPAN + " minutes and " + ENTER_SPAN_DATA + "@@@" + LEAVE_SPAN + " seconds past the hour.</p><p><a href=\"@@@\">&gt;&gt; Check Again &lt;&lt;</a> to see whether class has started yet or not.</p><p><a href=\"@@@\">&gt;&gt; Access Recordings &lt;&lt;</a> of previously held sessions of this class.</p>" + HTML_SUPPORT_FRAGMENT)).split("@@@");
    static final String[] HTML_NOTES_PAGE = (weave(HTML_PAGE, "OK", "<p>Your " + ENTER_SPAN_GOOD + "ticket is valid" + LEAVE_SPAN + ", @@@ there are @@@ recording(s) available for viewing at this time.</p><p>Hints:</p><ul><li>Each session may have been split into one or more parts.</li><li>In some cases, it may be necessary to restart your browser between watching each part to avoid running out of memory. This depends upon whether the applet is successfully stopped or not.</li></ul><p>The following recordings are available for viewing:</p><ul>@@@</ul></p>")).split("@@@");
    static final String   HTML_NOTES_DENIED_PAGE = (weave(HTML_PAGE, "Access Denied", "<p>Your ticket is not enabled for recordings access.</p>"));
    static final String[] HTML_APPLET_TEACHER = (weave(HTML_PAGE, "Teacher's Applet", "<applet code=\"zs42.learner2g.teacher2g.Teacher2g\" archive=\"/resource/teacher-applet/utc@@@ms.jar\" height=\"100\" width=\"500\"><param name=\"SETTINGS\" value=\"ORIGIN PSEUDO_HOST PSEUDO_PATH PSEUDO_NAME SUPPORT_REWIND ENFORCE_REWIND ASSISTANT ASSISTANT_KMAC ETCH_H ETCH_W ETCH_UPS\"></param><param name=\"SETTING_ORIGIN\" value=\"pseudo\"></param><param name=\"SETTING_PSEUDO_HOST\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_PATH\" value=\"/\"></param><param name=\"SETTING_PSEUDO_NAME\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_AUTH\" value=\"@@@\"></param><param name=\"SETTING_SUPPORT_REWIND\" value=\"false\"></param><param name=\"SETTING_ENFORCE_REWIND\" value=\"false\"></param><param name=\"SETTING_ASSISTANT\" value=\"KMAC\"></param><param name=\"SETTING_ASSISTANT_KEY\" value=\"@@@\"></param><param name=\"SETTING_ETCH_H\" value=\"@@@\"></param><param name=\"SETTING_ETCH_W\" value=\"@@@\"></param><param name=\"SETTING_ETCH_UPS\" value=\"30\"></param></applet>" + HTML_COMEBACK_FRAGMENT)).split("@@@");
    static final String[] HTML_APPLET_STUDENT = (weave(HTML_PAGE, "Student's Applet", HTML_PREMABLE_FRAGMENT + "<applet code=\"zs42.learner2g.student2g.Student2g\" archive=\"/resource/student-applet/utc@@@ms.jar\" height=\"100\" width=\"500\"><param name=\"SETTINGS\" value=\"ORIGIN PSEUDO_HOST PSEUDO_PATH PSEUDO_NAME PSEUDO_AUTH STUDENT_NAME SUPPORT_REWIND ENFORCE_REWIND ASSISTANT ETCH_H ETCH_W ETCH_UPS" + (DEBUG_APPLETS ? " USE_128_BIT_VECTORS" : "") + "\"></param><param name=\"SETTING_ORIGIN\" value=\"pseudo\"></param><param name=\"SETTING_PSEUDO_HOST\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_PATH\" value=\"/\"></param><param name=\"SETTING_PSEUDO_NAME\" value=\"@@@\"></param><param name=\"SETTING_PSEUDO_AUTH\" value=\"@@@\"><param name=\"SETTING_STUDENT_NAME\" value=\"@@@\"></param><param name=\"SETTING_SUPPORT_REWIND\" value=\"false\"></param><param name=\"SETTING_ENFORCE_REWIND\" value=\"false\"></param><param name=\"SETTING_ASSISTANT\" value=\"SHA2\"></param></param><param name=\"SETTING_ETCH_H\" value=\"@@@\"></param><param name=\"SETTING_ETCH_W\" value=\"@@@\"></param><param name=\"SETTING_ETCH_UPS\" value=\"30\"></param>" + (DEBUG_APPLETS ? "<param name=\"SETTING_USE_128_BIT_VECTORS\" value=\"true\"></param>" : "") + "</applet>" + HTML_COMEBACK_FRAGMENT)).split("@@@");
    static final String[] HTML_APPLET_NOTES = (weave(HTML_PAGE, "Student's Applet", "<p>You are accessing a recorded session; this time, there is no instructor!</p><p>The applet should load soon.</p><applet code=\"zs42.learner2g.student2g.Student2g\" archive=\"/resource/student-applet/utc@@@ms.jar\" height=\"100\" width=\"500\"><param name=\"SETTINGS\" value=\"ORIGIN COOKED_HOST COOKED_PATH SUPPORT_REWIND ENFORCE_REWIND ASSISTANT ETCH_H ETCH_W ETCH_UPS" + (DEBUG_APPLETS ? " USE_128_BIT_VECTORS" : "") + "\"></param><param name=\"SETTING_ORIGIN\" value=\"cooked\"></param><param name=\"SETTING_COOKED_HOST\" value=\"@@@\"></param><param name=\"SETTING_COOKED_PATH\" value=\"@@@\"></param><param name=\"SETTING_SUPPORT_REWIND\" value=\"true\"></param><param name=\"SETTING_ENFORCE_REWIND\" value=\"true\"></param><param name=\"SETTING_ASSISTANT\" value=\"NONE\"></param></param><param name=\"SETTING_ETCH_H\" value=\"@@@\"></param><param name=\"SETTING_ETCH_W\" value=\"@@@\"></param><param name=\"SETTING_ETCH_UPS\" value=\"30\"></param>" + (DEBUG_APPLETS ? "<param name=\"SETTING_USE_128_BIT_VECTORS\" value=\"true\"></param>" : "") + "</applet>" + HTML_COMEBACK_FRAGMENT)).split("@@@");
    
    final Gatekeeper gatekeeper;
    
    Server(Gatekeeper gatekeeper)
    {
      this.gatekeeper = gatekeeper;
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
    
    static boolean starts_with(byte[] buf, int off, int lim, byte[] pat)
    {
      if ((lim - off) < pat.length) return false;
      
      for (int pos = 0; pos < pat.length; pos++) {
        if (buf[off++] != pat[pos]) {
          return false;
        }
      }
      
      return true;
    }
    
    static int locate(byte[] buf, int off, int lim, byte[] pat)
    {
      if (pat.length == 0) return off;
      
      byte end = pat[pat.length - 1];
      
      for (int pos = (off + (pat.length - 1)); pos < lim; pos++) {
        if (buf[pos] == end) {
          boolean success = true;
          
          for (int j = 1; j < pat.length; j++) {
            if (buf[pos - j] != pat[(pat.length - 1) - j]) {
              success = false;
              break;
            }
          }
          
          if (success) {
            return (pos - (pat.length - 1));
          }
        }
      }
      
      return -1;
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
    
    private static String quash_bytes_between(byte[] buf, int off, int lim, byte[] head, byte[] tail)
    {
      int top, bot;
      
      if ((top = locate(buf, off, lim, head)) != -1) {
        top += head.length;
        
        if ((bot = locate(buf, top, lim, tail)) != -1) {
          return quash_bytes(buf, top, bot);
        }
      }
      
      return null;
    }
    
    /***
     * ENTER STATE FIELDS
     * 
     * - fields declared in this block shouldn't be accessed without
     *   holding the Server object's monitor
     ***/
    
    final BufferCentral central = (new BufferCentral(9));
    
    final SecureRandom secureRandom = (new SecureRandom());
    
    byte[] newSecureRandomBytes(int count)
    {
      byte[] out = (new byte[count]);
      secureRandom.nextBytes(out);
      return out;
    }
    
    class Packet
    {
      Groupir2g.StreamIdentifier streamIdentifier;
      
      byte[] encoding;
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
      
      final ArrayList<String> messages = (new ArrayList<String>());
      
      String newAuthenticator()
      {
        return HexStr.bin2hex(newSecureRandomBytes(CODE_LENGTH_BYTES));
      }
      
      Session(Channel channel_shadow)
      {
        channel = channel_shadow;
        
        sessioni = (channel.organization_name + "." + channel.name);
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
        
        (new Thread()
          {
            final long STROBE_NS = (10 * 1000000000L); /* every 10 seconds */
            final long TIMEOUT_NS = (5 * 60 * 1000000000L); /* 5 minutes */
            
            FileOutputStream fos_pkt = null;
            FileOutputStream fos_msg = null;
            
            int packeti = 0;
            int messagei = 0;
            
            SimpleDeque<byte[]> packet_writes  = (new SimpleDeque<byte[]>());
            SimpleDeque<String> message_writes = (new SimpleDeque<String>());
            
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
                
                synchronized (Server.this) {
                  for (Packet packet : packets) {
                    total += packet.streamIdentifier.ordinal();
                    
                    if (packet.encoding.length > 0) {
                      for (int i = 0; i < packet.encoding.length; i += 4096) {
                        total += packet.encoding[i];
                      }
                      
                      total += packet.encoding[packet.encoding.length - 1];
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
              
              synchronized (Server.this) {
                alive = (sessions.get(sessioni) == Session.this);
                
                if (packeti < packets.size()) {
                  while (packeti < packets.size()) {
                    packet_writes.addLast(packets.get(packeti++).encoding);
                  }
                  
                  touch_ns = now_ns;
                }
                
                if (messagei < messages.size()) {
                  while (messagei < messages.size()) {
                    message_writes.addLast(messages.get(messagei++));
                  }
                  
                  touch_ns = now_ns;
                }
              }
              
              if (alive && ((now_ns - touch_ns) > TIMEOUT_NS)) {
                alive = false;
              }
              
              if (!packet_writes.isEmpty()) {
                while (!packet_writes.isEmpty()) {
                  fos_pkt.write(packet_writes.removeFirst());
                }
                
                fos_pkt.flush();
              }
              
              if (!message_writes.isEmpty()) {
                while (!message_writes.isEmpty()) {
                  fos_msg.write(NoUTF.str2bin((message_writes.removeFirst() + "\n")));
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
                
                return false;
              }
              
              return true;
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
                  
                  sleep(1000);
                }
              } catch (Throwable e) {
                Log.log(e);
              }
              
              // de-list session (careful ... only if it is current)
              {
                synchronized (Server.this) {
                  if (sessions.get(sessioni) == Session.this) {
                    sessions.remove(sessioni);
                  }
                }
              }
              
              Log.log(("session " + sessioni + " delisted"));
            }
          }).start();
      }
    }
    
    // key for sessions map is organization.channel
    final AsciiTreeMap<Session> sessions = (new AsciiTreeMap<Session>());
    
    String recent_sessioni = "";
    
    /***
     * LEAVE STATE FIELDS
     ***/
    
    static final class Result
    {
      static final Result SUCCESS = (new Result(true));
      static final Result FAILURE = (new Result(false));
      
      final boolean success;
      final String path;
      
      Result(boolean success)
      {
        this.success = success;
        this.path = null;
      }
      
      Result(String path)
      {
        this.success = true;
        this.path = path;
      }
    }
    
    private String zeropad(int required, String content)
    {
      while (content.length() < required) {
        content = ("0" + content);
      }
      
      return content;
    }
    
    private Result write_blob_page(ByteArrayOutputStream out, byte[] mime, byte[] blob) throws IOException
    {
      out.write(HTTP_ENTER_TO_CONTENT_TYPE);
      out.write(mime);
      out.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      out.write(("" + blob.length).getBytes(UTF_8));
      out.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      out.write(blob);
      out.write(HTTP_MESSAGE_BODY_TO_LEAVE);
      
      return Result.SUCCESS;
    }
    
    private Result write_text_page(ByteArrayOutputStream out, String text) throws IOException
    {
      return write_blob_page(out, HTTP_CONTENT_TYPE_TEXT_PLAIN, text.getBytes(UTF_8));
    }
    
    private Result write_html_page(ByteArrayOutputStream out, String html) throws IOException
    {
      return write_blob_page(out, HTTP_CONTENT_TYPE_TEXT_HTML, html.getBytes(UTF_8));
    }
    
    private Result write_marker(ByteArrayOutputStream out, String marker) throws IOException
    {
      byte[] bytes = marker.getBytes(UTF_8);
      out.write(bytes);
      out.write(((byte)(bytes.length)));
      return Result.SUCCESS;
    }
    
    private Result handle_status_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      return write_text_page(out, (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().toString() + "\n"));
    }
    
    private Result handle_resource_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      final String name = words[wordi++];
      
      final Resource resource;
      
      if ((resource = gatekeeper.resources.get(name)) == null) {
        Log.log(("resource not found for name='" + name + "'"));
        return Result.FAILURE;
      }
      
      out.write(HTTP_ENTER_TO_CONTENT_TYPE);
      out.write(resource.mime.getBytes(UTF_8));
      out.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      out.write(("" + resource.data.length).getBytes(UTF_8));
      out.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      out.write(resource.data);
      out.write(HTTP_MESSAGE_BODY_TO_LEAVE);
      
      return Result.SUCCESS;
    }
    
    private Result handle_teacher_login_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      String organization_name = words[wordi++];
      String channel_name      = words[wordi++];
      String credential        = words[wordi++];
      String method            = words[wordi++];
      
      Channel channel = gatekeeper.organizations.get(organization_name).channels.get(channel_name);
      
      if (!(channel.teacher_credentials.containsKey(credential))) return Result.FAILURE;
      
      Session session = (new Session(channel));
      session.start();
      
      /****/ if (method.equals("applet")) {
        return write_html_page(out, weave(HTML_APPLET_TEACHER, now_ms, gatekeeper.host_http, session.sessioni, session.teacher_authenticator, HexStr.bin2hex(session.teacher_kmac), ("" + session.channel.pxH), ("" + session.channel.pxW)));
      } else if (method.equals("simple")) {
        return write_text_page(out, ("<s><p d=\"teacher\"><p d=\"session\"><k>authenticated</k><v>true</v><k>KMAC</k><v>" + HexStr.bin2hex(session.teacher_kmac) + "</v><k>H</k><v>" + session.channel.pxH + "</v><k>W</k><v>" + session.channel.pxW + "</v></p></p></s>\n"));
      } else {
        throw null;
      }
    }
    
    private Result handle_student_login_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String credential        = words[wordi++];
      
      final Organization organization;
      final Channel channel;
      final Session session;
      
      if (((organization = gatekeeper.organizations.get(organization_name)) == null) ||
          ((channel = organization.channels.get(channel_name)) == null)) {
        return write_html_page(out, weave(HTML_ERROR_PAGE, ERROR_NOENT, "the requested channel does not exist (error " + ERROR_NOENT + ")"));
      }
      
      final String student_name;
      
      if ((student_name = channel.student_credentials.get(credential)) == null) {
        return write_html_page(out, weave(HTML_ERROR_PAGE, ERROR_PERM, "access is restricted (error " + ERROR_PERM + ")"));
      }
      
      if ((session = sessions.get((organization.name + "." + channel.name))) == null) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        
        return write_html_page(out, weave(HTML_COVER_PAGE, channel.name, zeropad(2, ("" + minutes)), zeropad(2, ("" + seconds)), ("/student-login/" + organization_name + "/" + channel_name + "/" + credential + "/" + now_ms), ("/student-notes/" + organization_name + "/" + channel_name + "/" + credential + "/" + now_ms)));
      }
      
      return write_html_page(out, weave(HTML_APPLET_STUDENT, now_ms, gatekeeper.host_http, (organization_name + "/" + channel_name), session.newStudentAuthenticator(src, credential), student_name, ("" + session.channel.pxH), ("" + session.channel.pxW)));
    }
    
    private Session locate_session(String organization_name, String channel_name, String mysterion, boolean privileged)
    {
      final Session session = sessions.get((organization_name + "." + channel_name));
      if (!(mysterion.equals(gatekeeper.control_credential) || session.channel.teacher_credentials.containsKey(mysterion) || session.teacher_authenticator.equals(mysterion) || (!privileged && session.student_authenticators.containsKey(mysterion)))) return null;
      return session;
    }
    
    private Result handle_control_session_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      
      final Session session = locate_session(organization_name, channel_name, mysterion, true);
      
      return write_text_page(out, ("<s><p d=\"teacher\"><p d=\"session\"><k>authenticated</k><v>true</v><k>KMAC</k><v>" + HexStr.bin2hex(session.teacher_kmac) + "</v></p></p></s>\n"));
    }
    
    private Result handle_push_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      return Result.FAILURE;
    }
    
    private Result handle_pull_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
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
          
          if (current_marker_catch && (packet.streamIdentifier.getDisposition() == Groupir2g.StreamDisposition.PRIMARY)) {
            current_marker_index++;
            continue;
          }
          
          if (packet.encoding.length <= remaining_buffer_size) {
            /* out.write(packet.encoding); */
            remaining_buffer_size -= packet.encoding.length;
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
      out.write(HTTP_ENTER_TO_CONTENT_TYPE);
      out.write(HTTP_CONTENT_TYPE_APPLICATION_OCTET_STREAM);
      out.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      out.write(("" + (buffer_consumption + output_marker.length() + 1)).getBytes(UTF_8));
      out.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      
      // write packets
      if (session != null) {
        int remaining_buffer_size = initial_buffer_size;
        
        boolean current_marker_catch = initial_marker_catch;
        int     current_marker_index = initial_marker_index;
        
        boolean overflowed = false;
        
        while (current_marker_index < session.packets.size()) {
          Packet packet = session.packets.get(current_marker_index);
          
          if (current_marker_catch && (packet.streamIdentifier.getDisposition() == Groupir2g.StreamDisposition.PRIMARY)) {
            current_marker_index++;
            continue;
          }
          
          if (packet.encoding.length <= remaining_buffer_size) {
            out.write(packet.encoding);
            remaining_buffer_size -= packet.encoding.length;
          } else {
            overflowed = true;
            break;
          }
          
          current_marker_index++;
        }
      }
      
      // write marker
      return write_marker(out, output_marker);
    }
    
    private final static Pattern pattern_hexstr = Pattern.compile("([0-9a-f]{2})*");
    
    private Result handle_push_mesg_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
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
        session.messages.add(content);
        return write_text_page(out, "ACCEPTED");
      } else {
        return write_text_page(out, "OUTDATED");
      }
    }
    
    private Result handle_pull_mesg_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String marker            = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Session session = locate_session(organization_name, channel_name, mysterion, true);
      
      StringBuilder result = (new StringBuilder());
      
      for (int i = Integer.parseInt(marker); i < session.messages.size(); i++) {
        result.append(session.messages.get(i));
        result.append('\n');
      }
      
      return write_text_page(out, result.toString());
    }
    
    private Result handle_student_notes_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
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
      
      return write_html_page(out, weave(HTML_NOTES_PAGE, ((count > 0) ? "and" : "but"), ((count > 0) ? ("" + count) : "no"), block.toString()));
    }
    
    private Result handle_student_notes_applet_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
    {
      final String now_ms = ("" + System.currentTimeMillis());
      
      final String organization_name = words[wordi++];
      final String channel_name      = words[wordi++];
      final String mysterion         = words[wordi++];
      final String recording         = words[wordi++];
      final String section           = words[wordi++];
      final String timecode          = words[wordi++];
      
      final Channel channel = gatekeeper.student_credentials.get(mysterion);
      
      if (!(channel.recordings.containsKey(recording))) return Result.FAILURE;
      
      // for now, existence of a rewatch credential means DENY
      if (channel.rewatch_credentials.containsKey(mysterion)) {
        return write_html_page(out, HTML_NOTES_DENIED_PAGE);
      } else {
        return write_html_page(out, weave(HTML_APPLET_NOTES, now_ms, gatekeeper.host_http, ("/student-notes-archive/" + organization_name + "/" + channel_name + "/" + mysterion + "/" + recording + "/" + section + "/" + now_ms), ("" + channel.pxH), ("" + channel.pxW)));
      }
    }
    
    private Result handle_student_notes_archive_request(String src, String[] words, int wordi, ByteArrayOutputStream out) throws IOException
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
      
      out.write(HTTP_ENTER_TO_CONTENT_TYPE);
      out.write(HTTP_CONTENT_TYPE_APPLICATION_OCTET_STREAM);
      out.write(HTTP_CONTENT_TYPE_TO_CONTENT_LENGTH);
      out.write(("" + length).getBytes(UTF_8));
      out.write(HTTP_CONTENT_LENGTH_TO_MESSAGE_BODY);
      
      final String path = (gatekeeper.recordings_directory + File.separator + channel.name + File.separator + recording + section + "i");
      
      return (new Result(path));
    }
    
    private Result handle_unknown_request(String src, String[] words, int wordi, String action, ByteArrayOutputStream out) throws IOException
    {
      final Channel channel;
      
      if ((channel = gatekeeper.student_credentials.get(action)) != null) {
        return handle_student_login_request(src, (new String[] { channel.organization_name, channel.name, action }), 0, out);
      }
      
      return Result.FAILURE;
    }
    
    private Result handle_request_stage_2(String src, String req, ByteArrayOutputStream out) throws IOException
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
      
      /****/ if (action.equals("status")) {
        return handle_status_request(src, words, wordi, out);
      } else if (action.equals("resource")) {
        return handle_resource_request(src, words, wordi, out);
      } else if (action.equals("teacher-login")) {
        return handle_teacher_login_request(src, words, wordi, out);
      } else if (action.equals("student-login")) {
        return handle_student_login_request(src, words, wordi, out);
      } else if (action.equals("control-session")) {
        return handle_control_session_request(src, words, wordi, out);
      } else if (action.equals("push")) {
        return handle_push_request(src, words, wordi, out);
      } else if (action.equals("pull")) {
        return handle_pull_request(src, words, wordi, out);
      } else if (action.equals("push-mesg")) {
        return handle_push_mesg_request(src, words, wordi, out);
      } else if (action.equals("pull-mesg")) {
        return handle_pull_mesg_request(src, words, wordi, out);
      } else if (action.equals("student-notes")) {
        return handle_student_notes_request(src, words, wordi, out);
      } else if (action.equals("student-notes-applet")) {
        return handle_student_notes_applet_request(src, words, wordi, out);
      } else if (action.equals("student-notes-archive")) {
        return handle_student_notes_archive_request(src, words, wordi, out);
      } else {
        return handle_unknown_request(src, words, wordi, action, out);
      }
    }
    
    private Result handle_request_stage_1(String src, byte[] buf, int off, int lim, ByteArrayOutputStream out) throws IOException
    {
      String req;
      
      // extract request string
      {
        if ((req = quash_bytes_between(buf, off, lim, HTTP_ENTER_URL, HTTP_LEAVE_URL)) == null) {
          return Result.FAILURE;
        }
      }
      
      // embellish source address with data from X-Forwarded-For headers
      {
        int top, bot;
        
        top = lim;
        
        while ((top = locate(buf, top, lim, HTTP_ENTER_FORWARDED_FOR)) != -1) {
          top += HTTP_ENTER_FORWARDED_FOR.length;
          
          if ((bot = locate(buf, top, lim, HTTP_LEAVE_FORWARDED_FOR)) != -1) {
            src = (src + ":" + quash_bytes(buf, top, bot));
            
            top = (bot + HTTP_LEAVE_FORWARDED_FOR.length);
          }
        }
      }
      
      Log.log(("NOTICE: calling handle_request_stage_2 with req='" + req + "'"));
      return handle_request_stage_2(src, req, out);
    }
    
    protected final void sendfile(String path, OutputStream out)
    {
      FileInputStream inp = null;
      
      try {
        inp = (new FileInputStream(path));
        
        byte[] buf = (new byte[65536]);
        
        int amt;
        
        while ((amt = inp.read(buf, 0, buf.length)) >= 0) {
          out.write(buf, 0, amt);
        }
      } catch (Throwable e1) {
        if (inp != null) {
          try {
            inp.close();
          } catch (Throwable e2) {
            Log.log(e1);
            throw (new RuntimeException(e2));
          }
        }
        
        throw (new RuntimeException(e1));
      }
    }
    
    final AtomicInteger httpParallelConnectionCounter = (new AtomicInteger(0));
    
    protected final void handle_http_connection(Socket client)
    {
      InputStream  inp = null;
      OutputStream out = null;
      
      try {
        client.setSoTimeout(HTTP_READ_TIMEOUT_MS);
        
        final String src = ((InetSocketAddress)(client.getRemoteSocketAddress())).getAddress().getHostAddress();
        
        Log.log(("entering handle_http_connection (parallel " + httpParallelConnectionCounter.incrementAndGet() + ") (from src='" + src + "')"));
        
        inp = client.getInputStream();
        out = client.getOutputStream();
        
        final byte[] buf = (new byte[8192]);
        int          off = 0;
        int          lim = 0;
        
        final ByteArrayOutputStream tmp = (new ByteArrayOutputStream());
        
        while (true) {
          Log.log(("NOTICE: outer loop top, off=" + off + ", lim=" + lim));
          
          // process
          {
            // handle available HTTP requests
            {
              int bot;
              
              while ((bot = locate(buf, off, lim, HTTP_CRLF_CRLF)) != -1) {
                Log.log(("NOTICE: inner loop top, off=" + off + ", lim=" + lim));
                
                // skip blank lines
                {
                  while ((off < bot) && starts_with(buf, off, bot, HTTP_CRLF)) {
                    off += HTTP_CRLF.length;
                  }
                }
                
                if (bot > off) {
                  // handle one request
                  {
                    if (starts_with(buf, off, bot, HTTP_GET_REQUEST)) {
                      Result result = Result.FAILURE;
                      
                      try {
                        Log.log(("NOTICE: waiting for synchronized (this) ..."));
                        
                        synchronized (this) {
                          Log.log(("NOTICE: calling handle_request_stage_1 with off=" + off + ", bot/lim=" + bot));
                          result = handle_request_stage_1(src, buf, off, bot, tmp);
                        }
                      } catch (Throwable e) {
                        Log.log(("WARNING: from src='" + src + "', request='" + quash_bytes(buf, off, bot) + "' errored"), e);
                      }
                      
                      if (result.success) {
                        Log.log(("success resulting in " + tmp.size() + " bytes"));
                        tmp.writeTo(out);
                        
                        if (result.path != null) {
                          Log.log(("success further resulting in a file transfer"));
                          sendfile(result.path, out);
                        }
                      } else {
                        Log.log(("WARNING: from src='" + src + "', request='" + quash_bytes(buf, off, bot) + "' failed"));
                        out.write(HTTP_ERROR_RESPONSE);
                      }
                      
                      tmp.reset();
                    } else {
                      Log.log(("WARNING: from src='" + src + "', request='" + quash_bytes(buf, off, bot) + "' tried a non-GET method"));
                      out.write(HTTP_METHOD_NOT_ALLOWED);
                    }
                    
                    out.flush();
                  }
                } else {
                  // ignore empty requests (i.e., skip blank lines as recommended by the HTTP specification)
                }
                
                off = bot + HTTP_CRLF_CRLF.length;
              }
            }
            
            // repage
            {
              Log.log(("NOTICE: repaging from off=" + off + ", lim=" + lim));
              
              int bot = lim;
              
              for (lim = 0; off < bot; off++) {
                buf[lim++] = buf[off];
              }
              
              off = 0;
              
              Log.log(("NOTICE: repaged to off=" + off + ", lim=" + lim));
            }
          }
          
          // read
          {
            if (lim < buf.length) {
              int amt = inp.read(buf, lim, (buf.length - lim));
              
              if (amt <= 0) {
                throw (new RuntimeException("encountered EOF"));
              }
              
              Log.log(("NOTICE: read an additional " + amt + " bytes"));
              
              lim += amt;
            } else {
              throw (new RuntimeException("request too long"));
            }
          }
        }
      } catch (Throwable e) {
        trace(e);
      }
      
      try {
        inp.close();
      } catch (Throwable e) {
        trace(e);
      }
      
      try {
        out.close();
      } catch (Throwable e) {
        trace(e);
      }
      
      try {
        client.close();
      } catch (Throwable e) {
        trace(e);
      }
      
      Log.log(("leaving handle_http_connection (parallel " + httpParallelConnectionCounter.decrementAndGet() + ")"));
    }
    
    final AtomicInteger splitterConnectionCounter = (new AtomicInteger(0));
    
    protected final void handle_splitter_connection(Socket client)
    {
      final int connectionId = splitterConnectionCounter.getAndIncrement();
      
      InputStream  inp = null;
      OutputStream out = null;
      
      try {
        final String src = ((InetSocketAddress)(client.getRemoteSocketAddress())).getAddress().getHostAddress();
        
        Log.log(("splitter connection from src='" + src + "'"));
        
        final Session session;
        final ByteArrayOutputStream capture_encoding;
        final SplitterCommon.Groupir.ChecksumAssistant assistant;
        final SplitterCommon.Groupir.Packet splitter_common_packet;
        final SplitterCommon.Groupir.PacketOutputStream packet_output_stream;
        final SimpleDeque<Groupir2g.Packet> packet_queue;
        final Groupir2g.SteamRoller steam_roller;
        
        Buffer.xB buffer_tail;
        
        synchronized (Server.this) {
          session = sessions.get(recent_sessioni);
          Log.log(("will expect KMAC '" + HexStr.bin2hex(session.teacher_kmac) + "'"));
          capture_encoding = (new ByteArrayOutputStream());
          assistant = (new SplitterCommon.Groupir.ChecksumAssistant.KMAC(session.teacher_kmac));
          splitter_common_packet = (new SplitterCommon.Groupir.Packet(central, false, false, false, false));
          packet_output_stream = (new SplitterCommon.Groupir.PacketOutputStream(central, (new SplitterCommon.Groupir.ChecksumAssistant.SHA2()), capture_encoding));
          packet_queue = (new SimpleDeque<Groupir2g.Packet>());
          steam_roller = (new Groupir2g.SteamRoller());
          
          buffer_tail = central.acquireB();
        }
        
        inp = client.getInputStream();
        out = client.getOutputStream();
        
        final byte[] buf = (new byte[65536]);
        int amt;
        
        while (true) {
          Log.log(("on connectionId " + connectionId + " resuming reading at " + System.nanoTime()));
          if (!((amt = inp.read(buf, 0, buf.length)) > 0)) break;
          Log.log(("on connectionId " + connectionId + " got " + amt + " bytes " + System.nanoTime()));
          
          synchronized (Server.this) {
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
              Packet packet = (new Packet());
              
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
                
                {
                  int stream_id = oI.rI();
                  
                  switch (stream_id) {
                  case 0: packet.streamIdentifier = Groupir2g.StreamIdentifier.COMMAND;               break;
                  case 1: packet.streamIdentifier = Groupir2g.StreamIdentifier.AUDIO;                 break;
                  case 6: packet.streamIdentifier = Groupir2g.StreamIdentifier.NWED_EVENT;            break;
                  case 7: packet.streamIdentifier = Groupir2g.StreamIdentifier.ETCH_JARFILE_FRAGMENT; break;
                  case 8: packet.streamIdentifier = Groupir2g.StreamIdentifier.ETCH_EVENT_BUNDLE;     break;
                  case 9: packet.streamIdentifier = Groupir2g.StreamIdentifier.FROB_CODE;             break;
                  default: /* ignore the packet (below) */
                  }
                  
                  if (packet.streamIdentifier == null) {
                    //Log.log(("ignored packet with strange stream identifier " + stream_id));
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
                  
                  packet.encoding = capture_encoding.toByteArray();
                  
                  capture_encoding.reset();
                }
              }
              
              Log.log(("adding packet with streamIdentifier='" + packet.streamIdentifier + "' and encoding.length='" + packet.encoding.length + "'"));
              
              session.packets.add(packet);
            }
          }
        }
      } catch (Throwable e) {
        trace(e);
      }
      
      try {
        inp.close();
      } catch (Throwable e) {
        trace(e);
      }
      
      try {
        out.close();
      } catch (Throwable e) {
        trace(e);
      }
      
      try {
        client.close();
      } catch (Throwable e) {
        trace(e);
      }
    }
  }
  
  public static void main(String[] args) throws Exception
  {
    final int HTTP_BACKLOG = AbstractServer.DEFAULT_LISTEN_BACKLOG;
    final int HTTP_THREADS = 128;
    
    final int SPLITTER_BACKLOG = AbstractServer.DEFAULT_LISTEN_BACKLOG;
    final int SPLITTER_THREADS = 24;
    
    Log.loopHandleEventsBackground(System.err, true);
    
    try {
      final AsciiTreeMap<String> settings = (new AsciiTreeMap<String>());
      
      Settings.Scanner.populate(settings, args[0]);
      
      final Gatekeeper gatekeeper = Gatekeeper.fromSettings(settings, "station");
      
      final Server server = (new Server(gatekeeper));
      final AbstractServer<Void> server_http = (new AbstractServer<Void>() { protected final void handle(Void context, Socket client) { server.handle_http_connection(client); } });
      final AbstractServer<Void> server_splitter = (new AbstractServer<Void>() { protected final void handle(Void context, Socket client) { server.handle_splitter_connection(client); } });
      
      final F0<Boolean> TRUE = (new F0<Boolean>() { public Boolean invoke() { return Boolean.TRUE; } });
      final F1<Void, Throwable> BOMB = (new F1<Void, Throwable>() { public Void invoke(Throwable e) { throw fatal(e); } });
      
      Log.log(("starting AbstractServer for HTTP protocol on port " + gatekeeper.port_http));
      AbstractServer.launch((new F0<AbstractServer<Void>>() { public AbstractServer<Void> invoke() { return server_http; } }), null, gatekeeper.port_http, HTTP_BACKLOG, HTTP_THREADS, TRUE, BOMB, BOMB, BOMB);
      
      Log.log(("starting AbstractServer for splitter protocol on port " + gatekeeper.port_splitter));
      AbstractServer.launch((new F0<AbstractServer<Void>>() { public AbstractServer<Void> invoke() { return server_splitter; } }), null, gatekeeper.port_splitter, SPLITTER_BACKLOG, SPLITTER_THREADS, TRUE, BOMB, BOMB, BOMB);
    } catch (Throwable e) {
      throw fatal(e);
    }
  }
}
