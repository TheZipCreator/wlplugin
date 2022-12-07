package net.mcwarlords.wlplugin.shell;

import java.util.ArrayList;

public class Value {
  // constants
  public native int NULL();
  public native int STRING();
  public native int FLOAT();
  public native int LIST();
  public native int BLOCK();
  
  // constructors
  private native void ctor();
  public Value() { ctor(); }
  private native void ctor(String v);
  public Value(String v) { ctor(v); }
  private native void ctor(int v);
  public Value(int v) { ctor(v); }
  private native void ctor(float v);
  public Value(float v) { ctor(v); }
  private native void ctor(Value[] v);
  public Value(Value[] v) { ctor(v); }
  public Value(ArrayList<Value> v) { ctor((Value[])v.toArray()); }

  public native String getString();
  public native int getInt();
  public native float getFloat();
  public native Value[] getList();

  public static native Value add(Value a, Value b);
  public static native Value sub(Value a, Value b);
  public static native Value mul(Value a, Value b);
  public static native Value div(Value a, Value b);
  public static native Value mod(Value a, Value b);

  public native boolean truthy();
}
