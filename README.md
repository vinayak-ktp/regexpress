# regexpress

A regular expression engine built from scratch in Java, with no external
dependencies — tokenizer, parser, AST, Thompson-construction NFA builder, and
two matchers (a plain boolean matcher and a Pike VM for search + captures),
all hand-rolled, all hand-tested.

This is a learning project: the point was to build every layer myself, understand
why each design decision was made, and verify behavior against `java.util.regex`
wherever the two are expected to agree.

## Requirements

- Java 21 (see `.java-version`)
- No build tool, no dependencies. `run.sh` compiles everything with `javac`
  directly.

## Build and run

```bash
./run.sh                          # compiles src/main + src/test, runs Main
./run.sh RegexTest                # runs one test class
./run.sh matcher.NfaMatcherTest   # a package-relative name also works
```

`run.sh` always wipes `out/production` and recompiles from scratch first, so a
renamed or deleted class never leaves stale bytecode behind.

There is no JUnit and no single "run everything" target — each test class has
its own `main` and reports its own pass/fail count via a hand-rolled harness
(`TestSupport`). Run the ones relevant to what you changed:

```bash
./run.sh ast.nodes.AstTest
./run.sh ast.CharSetTest
./run.sh tokenizer.TokenizerTest
./run.sh parser.ParserTest
./run.sh nfa.NfaTest
./run.sh matcher.NfaMatcherTest
./run.sh matcher.PikeMatcherTest
./run.sh RegexTest
```

All eight suites currently pass, 3,749 checks in total.

## Usage

```java
Regex regex = Regex.compile("a (cat|dog)");

regex.matches("a cat");            // false — matches() requires the WHOLE input

Match m = regex.find("there's a cat here");
m.group();                          // "a cat"
m.group(1);                         // "cat"
m.start(); m.end();                 // 8, 13

regex.replaceAll("a cat and a dog", "pet");  // "pet and pet"

Regex.compile("\\d+").split("a1b22c333d");    // ["a", "b", "c", "d"]

// one-off convenience, for code that only needs the answer once
Regex.matches("a+", "aaa");         // true
```

`find` can also resume after a given offset (`regex.find(input, from)`), which
is how repeated search — finding every match in a string — is built on top of
it; see `Main.java` for a worked example.

## Package layout

```
com.regexpress                  Regex, Match — the only public API most callers need
├── ast                         the syntax tree and character-set logic
│   ├── nodes                   Node (sealed) and its record variants: Concat,
│   │                           Alternate, Star/Plus/Optional (each greedy-or-lazy),
│   │                           CharSet, Group, StartAnchor, EndAnchor, Empty
│   ├── CharSet.java             ranges, negation, union, shorthand classes
│   └── AstPrinter.java          canonical flat/tree rendering of a Node
├── tokenizer                   turns pattern text into a token stream
│   ├── tokens                   Token (sealed): Literal, Operator, ClassShorthand, End
│   ├── Tokenizer.java            the escape state machine
│   └── RegexSyntaxException.java thrown by the tokenizer and the parser
├── parser                      recursive descent over tokens -> Node
│   └── Parser.java
├── nfa                         Thompson construction: Node -> runnable machine
│   ├── State.java                one NFA state: a labelled arrow, free (epsilon)
│   │                             arrows, an optional assertion guard, a save-slot
│   │                             index for capture groups, an accepting flag
│   ├── NfaBuilder.java           one build method per Node variant
│   ├── Nfa.java / Fragment.java  a built machine / a sub-machine under construction
│   └── NfaPrinter.java           debug rendering of a machine's states and arrows
└── matcher                     running a machine against input
    ├── NfaMatcher.java           plain yes/no matching: epsilon-closure over a
    │                             Set<State> — fast, but reports no position and
    │                             no captures
    ├── PikeMatcher.java          search, leftmost-first priority, and capture
    │                             groups: an ordered thread list instead of a set,
    │                             each thread carrying its start position and a
    │                             capture-slot array
    └── Match.java                a matcher-internal result: span + raw slots
```

Dependencies between packages are acyclic: `ast` and `tokenizer` are leaves,
`parser` depends on both, `nfa` depends on `ast`, `matcher` depends on `nfa`.
Everything inside a package is package-private unless an outside consumer
genuinely needs it — public accessors are added on demand, never
preemptively.

## How each component works

A pattern travels through the pipeline in this order:

```
pattern text -> Tokenizer -> Parser -> AST (Node) -> NfaBuilder -> Nfa -> Matcher -> Match
```

Each stage below takes the previous stage's output type and produces the next.

### Tokenizer

`Tokenizer.tokenize(String)` turns the raw pattern text into a flat `List<Token>`.
It is a single pass with one bit of memory: whether the previous character was
an unconsumed backslash.

- Not escaped, and the character is one of `*+?|(){}[].^$` → an `Operator`.
- Not escaped, anything else → a `Literal`.
- Escaped (preceded by `\`), and the character is one of `dDwWsS` → a
  `ClassShorthand`.
- Escaped, anything else → a `Literal` — this is how `\.`, `\*`, `\\`, and so
  on turn a syntactic character back into ordinary text.
- A trailing, unconsumed `\` at the end of the pattern is a syntax error
  (`RegexSyntaxException`), not a silent no-op.

A synthetic `End` token is appended after the last real character, so every
other stage can test "have I run out of input?" by checking the token type
instead of comparing an index against the pattern's length.

Every token records its source position (the index in the pattern string it
came from), which is how a later syntax error can point a caret at the exact
character that caused it, even though the parser is working several stages
removed from the raw string.

**Deliberate scope decision:** a character is only ever an `Operator` if it is
syntax *everywhere* it appears. `,` and `-` are two characters that are
syntactic only in specific contexts (`{m,n}`, `[a-z]`) and ordinary text
everywhere else — so they are never tokenized as operators at all. The parser
recognizes them contextually, later, only inside `{...}` and `[...]`.

### AST — `Node` and `CharSet`

`Node` is a sealed interface with one record per syntax construct:
`ConcatNode`, `AlternateNode`, `StarNode`/`PlusNode`/`OptionalNode` (each
carrying a `lazy` flag), `CharSetNode`, `GroupNode`, `StartAnchorNode`,
`EndAnchorNode`, `EmptyNode`. Because it's `sealed`, every `switch` over a
`Node` in the rest of the codebase (in `NfaBuilder`, `AstPrinter`) is
exhaustive — the compiler refuses to build if a new `Node` variant is ever
added without teaching every consumer how to handle it.

The tree carries no behavior of its own; every operation over it
(`AstPrinter.tree()`/`flat()`, `NfaBuilder.buildFragment()`) lives as an
exhaustive `switch` in a separate class. `toString()` on each record is the
one canonical rendering of that node, reused everywhere a human-readable form
is needed (debug prints, error context).

`CharSet` is the other half of the AST layer — it represents "which
characters does this position accept," used by `CharSetNode` and by `.` and
character classes. Internally it's a sorted-on-demand list of `Range(from,
to)` pairs plus a `negated` flag; `contains(char)` XORs "is the character in
any range" against `negated`, so a single boolean flag turns any set into its
complement without rebuilding the ranges. `union(other)` folds another set's
ranges in directly, expanding a negated operand into its complement's ranges
first (`complement()` sorts the ranges and walks the gaps between them).
`negate()` is guarded so double-negation is a no-op rather than flipping back
and forth. The four shorthand classes (`\d \w \s` and their uppercase
negations) are built once from these primitives — `digit()` is a range,
`word()` unions three ranges and a literal `_`, `whitespace()` unions five
literal characters.

### Parser

`Parser.parse(String)` is hand-written recursive descent over the token list
from the tokenizer, one method per grammar level, each level handling one
precedence tier:

```
parseAlternation    a|b|c              — lowest precedence, loops on '|'
  parseConcatenation ab c              — implicit: adjacent items with no operator between them
    parseRepetition   a* a+ a? a{m,n}  — postfix quantifiers, optionally followed by '?' for lazy
      parseAtom        a . (a) [a-z] \d ^ $  — a single unit: literal, group, class, anchor
```

Each level calls the level below it and then loops, consuming operators at
its own precedence as long as they appear — this is what gives `ab|cd`
its correct grouping (concatenation binds tighter than alternation) without
any explicit precedence table.

A few grammar decisions worth knowing:

- **Groups.** `(...)` increments an internal `groupCount` and wraps the
  contents in a `GroupNode` carrying that 0-based index. `(?:...)` is
  recognized as its own branch immediately after consuming `(` — if what
  follows `?` isn't `:`, parsing fails with "unsupported group modifier"
  rather than silently accepting unknown syntax. A non-capturing group
  returns its inner `Node` directly, with no `GroupNode` wrapper and no
  effect on `groupCount`.
- **Lazy quantifiers.** `*?`, `+?`, `??` are one line each: `tryConsumeOperator('?')`
  is evaluated as the second constructor argument to `StarNode`/`PlusNode`/`OptionalNode`,
  so it greedily (in the Java-evaluation-order sense) consumes a trailing `?`
  if one is there, and that's the node's `lazy` flag.
- **Bounded repetition** (`{m}`, `{m,n}`, `{m,}`) is desugared in the parser,
  not carried as its own `Node` type: `{0,}` becomes a `StarNode`, `{1,}` a
  `PlusNode`, `{m,n}` becomes `m` concatenated copies followed by `n - m`
  nested `OptionalNode`s. By the time the AST exists, there is no trace of
  which syntax produced a given `Star`/`Plus`/`Optional` — that's intentional;
  the rest of the pipeline only needs to know the resulting shape.
- **Control flow dispatches on token type, never on character value.** The
  `peek*`/`tryConsume*`/`consume*`/`expect` helper family decides what to do
  by `instanceof` on the token, and only reads the underlying character after
  the type is already known. `consumeChar()` is the one place that legitimately
  reads an `Operator`'s symbol as an ordinary character — inside `[...]`, an
  unescaped operator character is just another class member.

### NFA construction — `NfaBuilder`, `State`, `Fragment`

`NfaBuilder.build(Node)` walks the AST exactly once, bottom-up, turning each
node into a `Fragment` — a sub-machine with exactly one `entrance` state and
one `exit` state, following Thompson's construction. Composing two fragments
means wiring one's `exit` to the other's `entrance` with a free ("epsilon")
arrow; nothing more.

A `State` is a plain, mutable, package-private bag of fields:

- `set` + `next` — a labelled arrow: on the character(s) in `set`, move to
  `next`. Only ever set for the states `CharSetNode` produces.
  a state has *either* a labelled arrow or free arrows, never both.
- `epsilon` — a list of free arrows, followed without consuming input.
  **The list's order is significant** — see below.
- `assertion` — `null`, or `START`/`END`: a state that can only be entered
  when that condition holds at the current input position (built by
  `buildAssertion`, used for `^`/`$`).
- `saveSlot` — `-1`, or an even/odd index into a capture-slot array: crossing
  this state records the current input position into that slot (built by
  `buildGroup`, used for capture groups — see PikeMatcher below).
- `accepting` — true only for the single exit state of the whole machine,
  set once by `NfaBuilder.build()` after the recursive walk finishes.

One `build*` method per `Node` variant. The interesting ones:

- **Concatenation** is just `left.exit.epsilon.add(right.entrance)` — one
  arrow, no new states.
- **Alternation** adds a new `in`/`out` state pair, epsilon-branches from
  `in` into both children's entrances, and epsilon-merges both children's
  exits into `out`.
- **Star/Plus/Optional** each add an `in`/`out` pair around the child
  fragment, wiring a loop-back arrow (Star, Plus) and/or a skip arrow
  (Star, Optional). **Which arrow is added to a given state's `epsilon` list
  *first* encodes a preference** — greedy quantifiers list "keep going"
  before "stop," lazy quantifiers list them in the opposite order. This is
  the only difference between a greedy and a lazy quantifier anywhere in the
  codebase: they describe the exact same set of matching strings, and differ
  only in which the matcher tries first (see PikeMatcher).
- **Groups** wrap the child fragment in an `in`/`out` pair whose `saveSlot`
  fields mark "record the position on the way in" and "record the position on
  the way out" — the group's captured text is just the substring between
  those two recorded positions.

`NfaPrinter.print(Nfa)` renders every state's id, its arrow (labelled or
epsilon), any assertion guard, and whether it's accepting — useful for
inspecting what a pattern actually compiled to. For example, `a*` (greedy)
compiles to something like:

```
State   0  ——a——>  1
State   1  ——ε——>  2, 3
State   2  ——ε——>  0, 3
State   3 (accepting)
```

(state 0 is the loop body's own entrance/exit pair for matching one `a`;
states 2 and 3 are the `in`/`out` pair `buildStar` added around it. State 2's
epsilon list is `[0, 3]` — loop back to state 0 to match another `a` *before*
the direct route to the accepting state 3 — exactly the "try to keep going
first" preference described above. A lazy `a*?` would print the same four
states with that order reversed.)

### Matching — `NfaMatcher` and `PikeMatcher`

Both matchers work by tracking, at each input position, which states could
currently be "active," and updating that collection one character at a time.
Both start by taking the **epsilon-closure** of the start state: follow every
free arrow (skipping any assertion state whose condition doesn't hold at the
current position) until only states with a labelled arrow, or the accepting
state, remain live.

**`NfaMatcher.matches(Nfa, String)`** answers only "does the pattern match the
*entire* input?" It keeps a plain `Set<State>` — order doesn't matter, and two
different paths that reach the same state at the same position are provably
interchangeable from then on, so the set just merges them. This makes it the
fastest of the two matchers, but a set has no way to say *where* a match
started or *what* a group captured — it can only report yes or no.

**`PikeMatcher`** answers the harder questions: where does a match start and
end (`find`), and what did each group capture. It replaces the set with an
ordered `List` of **threads** (here called `Candidate`s), each one a
`(state, start position, capture-slot array)` triple:

- **`start`** is what makes unanchored search possible: at every input
  position where no match has been confirmed yet, a fresh thread is injected
  at the start state, carrying that position as `start`. This is "search"
  reduced to "matching, with the start state continuously re-seeded" — no
  separate search algorithm is needed.
- **List order is priority order.** Existing threads are always ahead of a
  freshly injected one, and within a single position, a state's own epsilon
  list is expanded in order — so the ordering choices made back in
  `NfaBuilder` (greedy-loops-first vs. lazy-skip-first) directly become
  "which thread does the matcher prefer." The moment any thread reaches the
  accepting state, every thread *after* it in the list is dropped — they are
  strictly lower priority and can never produce a better answer — while
  higher-priority threads already in progress are kept, since a greedy
  continuation might still find a longer match. This one rule is what makes
  the engine leftmost-first rather than leftmost-longest.
- **Capture slots** ride along on each thread, copied (not mutated in place)
  whenever the thread crosses a `saveSlot` state, so two threads that
  briefly shared a state can still have recorded different capture history.
  Deduplication is still by `State` alone (the same rule that makes plain
  matching fast), which is safe here specifically *because* only the
  higher-priority thread at a given state is ever kept, and it carries the
  slots that should win.

`PikeMatcher.find(Nfa, String, int from)` is the primitive everything else is
built on: `replaceAll` and `split` are ordinary loops that call `find`
repeatedly, each time resuming after the previous match's end — with one
necessary adjustment: an *empty* match must advance the search position by
one extra character, or the loop never terminates.

### Facade — `Regex` and `Match`

`Regex.compile(String)` runs the whole pipeline once (tokenize → parse →
build) and returns an object holding only the finished `Nfa` — nothing about
tokens, the AST, or NFA states is exposed. `matches`/`find`/`replaceAll`/`split`
all delegate to the two matcher classes.

The public `Match` (`com.regexpress.Match`, distinct from the
matcher-internal `com.regexpress.matcher.Match`) is where the raw capture-slot
array becomes something usable: `group(0)` is always the whole match's
substring (from `start`/`end`, not from the slots); `group(i)` for `i ≥ 1`
maps onto `slots[2*(i-1)]`/`slots[2*(i-1)+1]` — translating the parser's
internal 0-based group index into the 1-based numbering every regex user
expects, with `null` returned for a group that didn't take part in the match.
This translation happens exactly once, at the public boundary — nothing
internal needs to know about 1-based numbering at all.

## What's supported

- Literals, escaping, and the four class shorthands `\d \D \w \W \s \S`
- `.` (any character, including newline — see "Deliberate decisions" below)
- Concatenation and alternation (`|`)
- Quantifiers `* + ?` and bounded repetition `{m}`, `{m,n}`, `{m,}`, each with
  a lazy variant (`*? +? ?? {m,n}?`)
- Character classes `[...]`, with ranges (`a-z`), negation (`[^...]`), and
  shorthand members
- Anchors `^` and `$`
- Capturing groups `(...)` and non-capturing groups `(?:...)`
- Unanchored search (`find`), repeated search, `replaceAll`, and `split`,
  all built on the same Pike VM

### Semantics this engine has committed to

Search results follow **leftmost-first** ("Perl") semantics, not
leftmost-longest ("POSIX") — the same convention as Java, Python, and
JavaScript. Concretely: among matches starting at the same, earliest
position, the alternative written first in the pattern wins, even if a later
alternative would have matched more text (`a|ab` against `"ab"` matches
`"a"`, not `"ab"`).

### Deliberate divergences from `java.util.regex`

- `.` matches any character including `\n` (this engine has no DOTALL-style
  mode yet — `.` is unconditionally `CharSet.all()`), so `a.c` matches
  `"a\nc"` here but not under plain `java.util.regex`.
- Escape provenance is dropped at tokenization: `\,` and `,` are the same
  token, as are `\-` and `-`, outside the contexts where those characters are
  syntactic (`{m,n}`, `[a-z]`).

Everywhere else, behavior is verified against `java.util.regex` directly,
including on adversarial ReDoS-style patterns and stacked quantifiers.

## Project status

Implemented and tested: syntax tree, tokenizer, parser, NFA construction,
plain matching, anchors, character classes and shorthands, unanchored search,
leftmost-first priority, greedy/lazy quantifiers, capture groups and
non-capturing groups, `replaceAll`, `split`, and the public `Regex`/`Match`
API.

Not yet started: performance work (lazy DFA, bit-parallel state sets,
prefilters) and a full low-level design / SOLID audit.
