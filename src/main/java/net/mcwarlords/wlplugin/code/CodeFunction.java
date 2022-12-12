package net.mcwarlords.wlplugin.code;

@FunctionalInterface
public interface CodeFunction {
  Value apply(Value[] args) throws InterpreterException;
}
