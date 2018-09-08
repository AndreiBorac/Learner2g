/***
 * Optimizer.java
 * copyright (c) 2011 by andrei borac
 ***/

package zs42.nats.codec;

import java.io.*;
import java.util.*;

class Optimizer
{
  static native void optimize(int numv, int[] blen, int[] optc, int[] optn, int lvl);
}
