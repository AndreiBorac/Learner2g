/***
 * Blazon.java
 * copyright (c) 2012 by andrei borac
 ***/

package zs42.parts;

public final class Blazon<T>
{
  public static final Blazon<Object>  BLAZON_OBJECT  = (new Blazon<Object>  ());
  public static final Blazon<Boolean> BLAZON_BOOLEAN = (new Blazon<Boolean> ());
  public static final Blazon<String>  BLAZON_STRING  = (new Blazon<String>  ());
  
  public static final Blazon<byte   []> BLAZON_BYTE_ARRAY   = (new Blazon<byte   []>());
  public static final Blazon<short  []> BLAZON_SHORT_ARRAY  = (new Blazon<short  []>());
  public static final Blazon<int    []> BLAZON_INT_ARRAY    = (new Blazon<int    []>());
  public static final Blazon<long   []> BLAZON_LONG_ARRAY   = (new Blazon<long   []>());
  public static final Blazon<float  []> BLAZON_FLOAT_ARRAY  = (new Blazon<float  []>());
  public static final Blazon<double []> BLAZON_DOUBLE_ARRAY = (new Blazon<double []>());
  
  public static final Blazon<Boolean[]> BLAZON_BOOLEAN_OBJECT_ARRAY = (new Blazon<Boolean[]> ());
  public static final Blazon<String[]>  BLAZON_STRING_OBJECT_ARRAY  = (new Blazon<String[]>  ());
}
