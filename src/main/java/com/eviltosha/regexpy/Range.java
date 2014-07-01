package com.eviltosha.regexpy;

class Range {
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
  void setBegin(final int begin) throws IllegalArgumentException {
    myBegin = begin;
    if (!checkConsistency()) {
      throw new IllegalArgumentException("Range begin > end");
    }
  }
  void setEnd(final int end) throws IllegalArgumentException {
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
    return (!isDefined() || myBegin <= myEnd);
  }

  boolean isDefined() {
    return (beginIsSet() && endIsSet());
  }

  private int myBegin, myEnd;
}

/**
 * Subclass of Range that treats unset end as infinity
 */
class InfinityRange extends Range {
  boolean checkUpper(final int num) {
    return (!endIsSet() || getEnd() >= num);
  }

  boolean checkLower(final int num) {
    return (getBegin() <= num);
  }
}