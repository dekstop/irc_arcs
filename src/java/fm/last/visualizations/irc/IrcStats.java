/**
 * 
 */
package fm.last.visualizations.irc;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author martind
 *
 */
public class IrcStats {

  public ArrayList<User> users = new ArrayList<User>();
  
  public int totalMessages = 0;
  public int maxMessages = 0;
  public float totalWpm = 0;
  public float maxWpm = 0f;
  public int maxNamedropsPerLink = 0;

  Map<String, Integer> incoming = new HashMap<String, Integer>();
  Map<String, Integer> outgoing = new HashMap<String, Integer>();
  Map<String, Float> ioRate = new HashMap<String, Float>();

  protected static class User {
    public String name;
    public int messages;
    public int words;
    public float words_per_message;

    public Map<String, Integer> namedrops = new HashMap<String, Integer>();
  }
  
  protected void analyze() {
    for (IrcStats.User user : users) {
      outgoing.put(user.name, 0);
      incoming.put(user.name, 0);
      for (String recipient : user.namedrops.keySet()) {
        incoming.put(recipient, 0);
      }
    }
      
    for (IrcStats.User user : users) {
      for (String recipient : user.namedrops.keySet()) {
        int count = user.namedrops.get(recipient);
        outgoing.put(user.name, outgoing.get(user.name) + count);
        incoming.put(recipient, incoming.get(recipient) + count);
      }
    }

    for (IrcStats.User user : users) {
      ioRate.put(user.name, (float)incoming.get(user.name) / outgoing.get(user.name));
    }
    
    // prepare
    totalMessages = 0;
    maxMessages = 0;
    totalWpm = 0;
    maxWpm = 0;
    for (IrcStats.User user : users) {
      totalMessages += user.messages;
      maxMessages = Math.max(maxMessages, user.messages);
      totalWpm += user.words_per_message;
      maxWpm = Math.max(maxWpm, user.words_per_message);
    }

    maxNamedropsPerLink = 0;
    for (IrcStats.User user : users) {
      for (String recipient: user.namedrops.keySet()) {
        maxNamedropsPerLink = 
          Math.max(
            maxNamedropsPerLink, 
            user.namedrops.get(recipient));
      }
    }
  }

  public void sortByIncoming() {
    
    Collections.sort(users, new Comparator<IrcStats.User>() {
      public int compare(User o1, User o2) {
        if (incoming.get(o1.name) == incoming.get(o2.name)) return 0;
        return incoming.get(o1.name) > incoming.get(o2.name) ? 1 : -1;
      }
    });
  }

  public void sortByOutgoing() {
    
    Collections.sort(users, new Comparator<IrcStats.User>() {
      public int compare(User o1, User o2) {
        if (outgoing.get(o1.name) == outgoing.get(o2.name)) return 0;
        return outgoing.get(o1.name) > outgoing.get(o2.name) ? 1 : -1;
      }
    });
  }

  public void sortByRatio() {
    
    Collections.sort(users, new Comparator<IrcStats.User>() {
      public int compare(User o1, User o2) {
        if (ioRate.get(o1.name) == ioRate.get(o2.name)) return 0;
        return ioRate.get(o1.name) > ioRate.get(o2.name) ? 1 : -1;
      }
    });
  }

  public void sortByName() {
    
    Collections.sort(users, new Comparator<IrcStats.User>() {
      public int compare(User o1, User o2) {
        return o1.name.compareTo(o2.name);
      }
    });
  }
  
  public User getUserByName(String name) {
    for (User user : users) {
      if (user.name.equals(name)) {
        return user;
      }
    }
    return null;
  }

  public static IrcStats loadFromXMLDocument(Document doc) {
    IrcStats irc = new IrcStats();

    Node rootNode = doc.getDocumentElement();

    NodeList userNodes = rootNode.getChildNodes();
    for (int idx = 0; idx < userNodes.getLength(); idx++) {
      
      Node userNode = userNodes.item(idx);
      
      if (userNode.getNodeType() == Node.ELEMENT_NODE &&
          userNode.getNodeName().equals("user")) {
        
        User user = new User();
        user.name = userNode.getAttributes().getNamedItem("name").getNodeValue();
        user.messages = Integer.parseInt(userNode.getAttributes().getNamedItem("messages").getNodeValue());
        user.words = Integer.parseInt(userNode.getAttributes().getNamedItem("words").getNodeValue());
        user.words_per_message = Float.parseFloat(userNode.getAttributes().getNamedItem("words_per_message").getNodeValue());
        irc.users.add(user);

        NodeList namedropNodes = userNode.getChildNodes();
        for (int idx2 = 0; idx2 < namedropNodes.getLength(); idx2++) {
          
          Node namedropNode = namedropNodes.item(idx2);
          
          if (namedropNode.getNodeType() == Node.ELEMENT_NODE &&
              namedropNode.getNodeName().equals("namedrop")) {
            
            String name = namedropNode.getAttributes().getNamedItem("name").getNodeValue();
            int count = Integer.parseInt(namedropNode.getAttributes().getNamedItem("count").getNodeValue());
            user.namedrops.put(name, count);
          }
        }
      }
    }
    irc.analyze();
    return irc;
  }

  public static IrcStats loadFromXMLFile(String filename) {
    Document doc = null;
    File docFile = new File(filename);

    // Parse the file
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      doc = db.parse(docFile);
    } catch (Exception e) {
      throw new RuntimeException("Problem parsing the file [" + filename + "]", e);
    }   

    return loadFromXMLDocument(doc);
  }

}
