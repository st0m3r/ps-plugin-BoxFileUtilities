package com.appiancorp.ps.plugins.boxFileUtilities.security;

/**
 * @author Jay Berkenbilt
 *
 * This class provides a simple RC4 implementation.
 *
 */
class RC4 {
  private short[] state = new short[256];
  private short x = 0;
  private short y = 0;

  public RC4(byte[] keyData) {
    for (short i = 0; i < 256; ++i) {
      state[i] = i;
    }
    x = 0;
    y = 0;

    int i1 = 0;
    int i2 = 0;
    for (int i = 0; i < 256; ++i) {
      i2 = (asUnsigned(keyData[i1]) + state[i] + i2) % 256;
      swapStateBytes(i, i2);
      i1 = (i1 + 1) % keyData.length;
    }
  }

  private short asUnsigned(byte val) {
    return (short)((val < 0) ? val + 0x100 : val);
  }

  private void swapStateBytes(int i1, int i2) {
    short temp = this.state[i1];
    this.state[i1] = this.state[i2];
    this.state[i2] = temp;
  }

  public void process(byte[] data) {
    process(data, data);
  }

  public void process(byte[] inData, byte[] outData) {
    if (inData.length != outData.length) {
      throw new RuntimeException("RC4.process: inData and outData must have same length");
    }

    for (int i = 0; i < inData.length; ++i) {
      x = (short)((x + 1) % 256);
      y = (short)((state[x] + y) % 256);
      swapStateBytes(x, y);
      int xorIndex = (state[x] + state[y]) % 256;
      outData[i] = (byte)(asUnsigned(inData[i]) ^ state[xorIndex]);
    }
  }
}
