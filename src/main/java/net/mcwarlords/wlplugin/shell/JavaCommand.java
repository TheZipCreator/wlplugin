package net.mcwarlords.wlplugin.shell;

import java.util.function.Function;

public class JavaCommand {
  Function<Value[], Value> fn;

  public JavaCommand(Function<Value[], Value> fn) {
    this.fn = fn;
  }

  public JavaCommand(JavaCommand jc) {
    fn = jc.fn;
  }

  Value run(Value[] vals) {
    return fn.apply(vals);
  }
}
