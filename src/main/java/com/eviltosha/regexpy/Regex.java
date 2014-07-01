package com.eviltosha.regexpy;

import java.util.Stack;

/**
 *
 */
public class Regex {
  /**
   *
   * @param regex
   * @throws RegexSyntaxException
   */
  public Regex(String regex) throws RegexSyntaxException {
    myRegexString = regex;
    parse(regex);
  }

  public Matcher matcher() {
    return new Matcher(myStartNode, myNumGroups);
  }

  public boolean matches(String str) {
    return matcher().matches(str);
  }

  private final String myRegexString;
  private final Node myStartNode = new OpenGroupNode(0);
  private int myNumGroups = 0;

  private void parse(String regex) throws RegexSyntaxException {
    RegexStringProcessor processor = new RegexStringProcessor(regex);
    Node endNode = new EndNode();
    Node termBeginNode = myStartNode;
    int maxGroupRecallId = 0;

    // used for '|' operator, indicate beginning of current group
    Stack<Node> groupStartNodeStack = new Stack<Node>();
    groupStartNodeStack.push(myStartNode);
    // used for '|' operator, indicate end of current group
    Stack<Node> groupEndNodeStack = new Stack<Node>();
    groupEndNodeStack.push(endNode);
    // used for quantifiers, indicate position before the start of current group
    Stack<Node> openGroupNodeStack = new Stack<Node>();

    boolean escaped = false;
    while (processor.hasNext()) {
      Node termEndNode;
      boolean quantifierApplicable = true;
      if (escaped) {
        if (Character.isDigit(processor.peek())) {
          // group recall
          int groupRecallId = processor.nextNumber();
          maxGroupRecallId = Math.max(maxGroupRecallId, groupRecallId);
          termEndNode = new GroupRecallNode(groupRecallId);
        } else {
          // special escape sequences
          switch (processor.peek()) {
            case 'd':
            case 'D':
            case 's':
            case 'S':
              termEndNode = constructSpecialCharRange(processor.next());
              break;
            default:
              // FIXME: if escaped character is not special, exception should be thrown
              termEndNode = new SymbolNode(processor.next());
              break;
          }
        }
        termBeginNode.addNextNode(termEndNode);
        escaped = false;
      } else {
        char ch = processor.next();
        switch (ch) {
          case '{':
          case '*':
          case '+':
            throw new RegexSyntaxException("Incorrect use of quantifier", myRegexString);
          case '|':
            termBeginNode.addNextNode(groupEndNodeStack.peek());
            termEndNode = groupStartNodeStack.peek();
            quantifierApplicable = false;
            break;
          case '(': { // artificially create scope to reuse some variable names in other cases
            ++myNumGroups;
            // we store this OpenGroupNode, so after the group is closed quantifiers could use it
            // as a termBeginNode
            openGroupNodeStack.push(termBeginNode);

            Node openNode = new OpenGroupNode(myNumGroups);
            termBeginNode.addNextNode(openNode);
            // we create and store CloseGroupNode, so '|' could use it as the end of the group node
            Node closeNode = new CloseGroupNode(myNumGroups);
            groupEndNodeStack.push(closeNode);
            // we store this EmptyNode, so '|' could use it as the start of the group node
            termEndNode = new EmptyNode();
            groupStartNodeStack.push(termEndNode);
            openNode.addNextNode(termEndNode);

            quantifierApplicable = false;
            break;
          }
          case ')': {
            if (openGroupNodeStack.isEmpty()) {
              throw new RegexSyntaxException("Unpaired ')'", myRegexString);
            }
            Node openNode = openGroupNodeStack.pop();
            termEndNode = groupEndNodeStack.pop();
            groupStartNodeStack.pop();

            termBeginNode.addNextNode(termEndNode);
            termBeginNode = openNode;
            break;
          }
          case '[':
            termEndNode = constructCharRangeNode(processor);
            termBeginNode.addNextNode(termEndNode);
            break;
          case '\\':
            escaped = true;
            quantifierApplicable = false;
            termEndNode = termBeginNode;
            break;
          case '.':
            termEndNode = new AnySymbolNode();
            termBeginNode.addNextNode(termEndNode);
            break;
          case '^':
            termEndNode = new AnchorStartStringNode();
            termBeginNode.addNextNode(termEndNode);
            quantifierApplicable = false;
            break;
          case '$':
            termEndNode = new AnchorEndStringNode();
            termBeginNode.addNextNode(termEndNode);
            quantifierApplicable = false;
            break;
          default:
            termEndNode = new SymbolNode(ch);
            termBeginNode.addNextNode(termEndNode);
            break;
        }
      }
      // quantifier application (if present & applicable)
      if (processor.hasNext() && quantifierApplicable) {
        Node exitNode = new EmptyNode();
        if (!tryApplyQuantifier(processor, termBeginNode, termEndNode, exitNode)) {
          termEndNode.addNextNode(exitNode);
        }
        termBeginNode = exitNode;
      } else {
        termBeginNode = termEndNode;
      }
    }
    if (!openGroupNodeStack.empty()) {
      throw new RegexSyntaxException("Unpaired '('", myRegexString);
    }
    if (maxGroupRecallId > myNumGroups) {
      throw new RegexSyntaxException("Recall of nonexistent group", myRegexString);
    }
    termBeginNode.addNextNode(endNode);
  }

  private Node constructCharRangeNode(RegexStringProcessor processor) {
    CharRangeNode charRangeNode = new CharRangeNode();
    // first characters that need special treatment: '^' (negates range),
    // '-' (in first position it acts like literal hyphen, also can be part of a range),
    // ']' (in first position it acts like literal closing square bracket, also can be part of a range)
    char ch = processor.next();
    if (ch == '^') {
      charRangeNode.setNegate(true);
      // we need to perform the first character analysis once more (for special '-' and ']' cases)
      ch = processor.next();
    }
    // we store parsed char,
    // if the next char is not '-', we add it as a char, otherwise construct range
    // if storedChar == null, we don't have any char stored
    Character storedChar = ch;
    boolean asRange = false;
    // flag of completion
    boolean charRangeFinished = false;
    while (processor.hasNext() && !charRangeFinished) {
      ch = processor.next();
      switch (ch) {
        case ']':
          if (storedChar != null) {
            charRangeNode.addChar(storedChar);
            // if '-' stands right before the closing bracket it's treated as literal '-'
            if (asRange) {
              charRangeNode.addChar('-');
            }
          }
          charRangeFinished = true;
          break;
        case '-':
          if (storedChar == null || asRange) {
            // check whether it's the last char in group (like in "[a--]")
            if (processor.next() == ']') {
              if (asRange) {
                try {
                  charRangeNode.addCharRange(storedChar, '-');
                } catch (IllegalArgumentException e) {
                  throw new RegexSyntaxException("Invalid char range", myRegexString);
                }
              } else {
                charRangeNode.addChar('-');
              }
              charRangeFinished = true;
            } else {
              throw new RegexSyntaxException("Incorrect use of hyphen inside char range", myRegexString);
            }
          }
          asRange = true;
          break;
        default:
          if (storedChar != null) {
            if (asRange) {
              try {
                charRangeNode.addCharRange(storedChar, ch);
              } catch (IllegalArgumentException e) {
                throw new RegexSyntaxException("Invalid char range", myRegexString);
              }
              storedChar = null;
            } else {
              charRangeNode.addChar(storedChar);
              storedChar = ch;
              // charIsStored remains true
            }
          } else {
            storedChar = ch;
          }
          asRange = false;
          break;
      }
    }
    if (!charRangeFinished) {
      throw new RegexSyntaxException("Unclosed char range", myRegexString);
    }
    return charRangeNode;
  }

  private Node constructSpecialCharRange(char rangeId) {
    CharRangeNode rangeNode = new CharRangeNode();
    switch (rangeId) {
      case 'D':
        rangeNode.setNegate(true);
        // fall through
      case 'd':
        rangeNode.addCharRange('0', '9');
      break;
      case 'S':
        rangeNode.setNegate(true);
        // fall through
      case 's':
        rangeNode.addChar('\r');
        rangeNode.addChar('\n');
        rangeNode.addChar('\t');
        rangeNode.addChar('\f');
        rangeNode.addChar(' ');
        break;
    }
    return rangeNode;
  }

  private boolean tryApplyQuantifier(RegexStringProcessor processor, Node termBeginNode, Node termEndNode, Node exitNode)
      throws RegexSyntaxException {
    switch (processor.peek()) {
      case '{':
        processor.next();
        InfinityRange range = constructInfinityRange(processor);
        if (range.getBegin() == 0) {
          termBeginNode.addNextNode(exitNode);
        }
        RangeQuantifierNode rangeNode = new RangeQuantifierNode(range, exitNode);
        termEndNode.addNextNode(rangeNode);
        rangeNode.addNextNode(termBeginNode);
        break;
      case '?':
        processor.next();
        termBeginNode.addNextNode(exitNode);
        termEndNode.addNextNode(exitNode);
        break;
      case '*':
        termBeginNode.addNextNode(exitNode);
        // fall through
      case '+':
        processor.next();
        termEndNode.addNextNode(termBeginNode);
        termEndNode.addNextNode(exitNode);
        // fall through
      default:
        // don't eat char here, we'll process it later
        return false;
    }
    return true;
  }

  private InfinityRange constructInfinityRange(RegexStringProcessor processor) {
    InfinityRange range = new InfinityRange();
    range.setBegin(processor.nextNumber());
    switch (processor.next()) {
      case ',':
        if (processor.peek() == '}') {
          processor.next();
        } else {
          try {
            range.setEnd(processor.nextNumber());
          } catch (IllegalArgumentException e) {
            throw new RegexSyntaxException("Invalid range quantifier parameters", myRegexString);
          }
          if (processor.next() != '}') {
            throw new RegexSyntaxException("Malformed range quantifier", myRegexString);
          }
        }
        break;
      case '}':
        range.setEnd(range.getBegin());
        break;
      default:
        throw new RegexSyntaxException("Invalid range quantifier", myRegexString);
    }

    return range;
  }
}
