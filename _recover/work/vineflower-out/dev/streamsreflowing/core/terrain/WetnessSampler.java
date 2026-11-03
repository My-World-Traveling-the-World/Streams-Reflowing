package dev.streamsreflowing.core.terrain;

@FunctionalInterface
public interface WetnessSampler {
   double wetness(int var1, int var2, int var3);
}
