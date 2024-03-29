package com.eviltosha.regexpy;

class Range {
  private int myBegin, myEnd;

  Range() { reset(); }

  void reset() {
    resetBegin();
    resetEnd();
  }

  int length() {
    assert(isDefined());
    return myEnd - myBegin;
  }

  int getBegin() { return myBegin; }

  int getEnd() { return myEnd; }

  void setBegin(int begin) throws IllegalArgumentException {
    myBegin = begin;
    if (!checkConsistency()) {
      throw new IllegalArgumentException("Range begin > end");
    }
  }

  void setEnd(int end) throws IllegalArgumentException {
    myEnd = end;
    if (!checkConsistency()) {
      throw new IllegalArgumentException("Range begin > end");
    }
  }

  boolean beginIsSet() { return (myBegin >= 0); }

  boolean endIsSet() { return (myEnd >= 0); }

  void resetBegin() { myBegin = -1; }

  void resetEnd() { myEnd = -1; }

  boolean checkConsistency() {
    return (!isDefined() || (myBegin <= myEnd));
  }

  boolean isDefined() {
    return (beginIsSet() && endIsSet());
  }
}

/**
 * Subclass of Range that treats unset end as infinity
 */
class InfinityRange extends Range {
  boolean checkUpper(int num) {
    return (!endIsSet() || (getEnd() >= num));
  }

  boolean checkLower(int num) {
    return (getBegin() <= num);
  }
}