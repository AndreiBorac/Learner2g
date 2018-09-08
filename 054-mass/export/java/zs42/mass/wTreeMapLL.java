/***
 * wTreeMapLL.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.mass;

import static zs42.mass.Static.*;

public class wTreeMapLL<K, V> extends rTreeMapLL<K, V>
{
  static class Node<K, V> extends RootObject
  {
    K key;
    V val;
    
    Node<K, V> L, R; // L -> less than, R -> greater than
    
    Node(K key, V val)
    {
      this.key = key;
      this.val = val;
    }
  }
  
  static final F2<Void, Node, Node> assign_L =
    (new F2<Void, Node, Node>()
     { @SuppressWarnings("unchecked")
       public Void invoke(Node parent, Node child)
       { parent.L = child; return null; } });
  
  static final F2<Void, Node, Node> assign_R =
    (new F2<Void, Node, Node>()
     { @SuppressWarnings("unchecked")
       public Void invoke(Node parent, Node child)
       { parent.R = child; return null; } });
  
  private F2<Void, Node<K, V>, Node<K, V>> get_assign_L() { return cast_unchecked(((F2<Void, Node<K, V>, Node<K, V>>)(null)), assign_L); }
  private F2<Void, Node<K, V>, Node<K, V>> get_assign_R() { return cast_unchecked(((F2<Void, Node<K, V>, Node<K, V>>)(null)), assign_R); }
  
  final Node<K, V> root = new Node<K, V>(null, null);
  
  /***
   * (package-private) constructor.
   ***/
  wTreeMapLL(Proxy<K> proxy)
  {
    super(proxy);
  }
  
  public static <K, V> wTreeMapLL<K, V> newEmpty(Proxy<K> proxy)
  {
    return new wTreeMapLL<K, V>(proxy);
  }
  
  private int size(Node<K, V> node)
  {
    int out = (node.val != null) ? 1 : 0;
    if (node.L != null) out += size(node.L);
    if (node.R != null) out += size(node.R);
    return out;
  }
  
  public int size()
  {
    return size(root);
  }
  
  public V get(K key)
  {
    K A = key;
    
    Node<K, V> curr = root.L;
    
    while (curr != null) {
      K B = curr.key;
      
      int AcmpB = proxy.compare(A, B);
      
      /****/ if (AcmpB < 0) {
        curr = curr.L;
      } else if (AcmpB > 0) {
        curr = curr.R;
      } else {
        return curr.val;
      }
    }
    
    return null;
  }
  
  public V put(K key, V val)
  {
    K A = key;
    
    Node<K, V> from = root;
    Node<K, V> curr = root.L;
    F2<Void, Node<K, V>, Node<K, V>> assign_X = get_assign_L();
    
    while (curr != null) {
      K B = curr.key;
      
      int AcmpB = proxy.compare(A, B);
      
      /****/ if (AcmpB < 0) {
        from = curr;
        curr = curr.L;
        assign_X = get_assign_L();
      } else if (AcmpB > 0) {
        from = curr;
        curr = curr.R;
        assign_X = get_assign_R();
      } else {
        V retv = curr.val;
        curr.val = val;
        return retv;
      }
    }
    
    assign_X.invoke(from, (new Node<K, V>(key, val)));
    return null;
  }
  
  private int entries(K[] arrK, V[] arrV, int ptr, Node<K, V> node)
  {
    {
      if (node.L != null) ptr = entries(arrK, arrV, ptr, node.L);
    }
    
    {
      if (node.val != null) {
        arrK[ptr] = node.key;
        arrV[ptr] = node.val;
        ptr++;
      }
    }
    
    {
      if (node.R != null) ptr = entries(arrK, arrV, ptr, node.R);
    }
    
    return ptr;
  }
  
  public int entries(K[] arrK, V[] arrV)
  {
    return entries(arrK, arrV, 0, root);
  }
  
  public void rmv(K key)
  {
    put(key, null);
  }
}
